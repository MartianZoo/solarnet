package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.AbstractException
import dev.martianzoo.tfm.api.UserException.DependencyException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Game.PlayerAgentImpl
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.tfm.types.MType

internal data class InstructionExecutor(
    val game: Game,
    val agent: PlayerAgentImpl,
    val cause: Cause?
) {

  fun execute(instruction: Instruction) {
    when (instruction) {
      is Change -> {
        val ct =
            instruction.count as? ActualScalar
                ?: throw UserException.abstractInstruction(instruction)
        if (instruction.intensity != MANDATORY) throw UserException.abstractInstruction(instruction)

        val g = game.toComponent(instruction.gaining)
        val r = game.toComponent(instruction.removing)
        r?.let { (game.components as WritableComponentGraph).checkDependents(ct.value, it) }

        val result = agent.update(count = ct.value, gaining = g, removing = r, cause = cause)
        result.changes.forEach(::fireTriggers)
      }
      is Then -> {
        if (instruction.descendantsOfType<XScalar>().any()) {
          throw AbstractException("$instruction has x's still")
        }
        instruction.instructions.forEach(::execute) // TODO hmm....
      }
      is Custom -> handleCustomInstruction(instruction)
      is Or -> throw UserException.orWithoutChoice(instruction)
      is NoOp -> {}
      else -> error("something went wrong: $instruction")
    }
  }

  private fun handleCustomInstruction(instr: Custom) {
    val arguments: List<Type> = instr.arguments.map { agent.reader.resolve(it) }
    val oops = arguments.filter { agent.reader.countComponent(it) == 0 }
    if (oops.any()) throw DependencyException(oops) // or it could be abstract

    val custom = agent.reader.authority.customInstruction(instr.functionName)
    val translated: Instruction = custom.translate(agent.reader, arguments) * instr.multiplier

    // I guess custom instructions can't return things using `This`
    // and Owner means the context player... (TODO think)
    val xers = game.transformers
    val instruction =
        chain(
                xers.atomizer(),
                xers.insertDefaults(), // TODO context component??
                xers.deprodify(),
                replaceOwnerWith(agent.player),
            )
            .transform(translated)

    agent.addTasks(instruction, agent.player, cause)
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val (now, later) = getFiringEffects(triggerEvent).partition { it.automatic }

    for (fx in now) {
      val executor = copy(cause = fx.cause)
      val preparer = Preparer(agent.reader, game.components)
      Instruction.split(fx.instruction).forEach {
        val committed = preparer.toPreparedForm(it)
        executor.execute(committed)
      }
    }
    game.addTriggeredTasks(later) // TODO why can't we do this before the for loop??
  }

  private fun getFiringEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      getFiringSelfEffects(triggerEvent) + getFiringOtherEffects(triggerEvent)

  // TODO can the differences be collapsed some?

  private fun getFiringSelfEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      componentsIn(triggerEvent)
          .map { Component.ofType(it) }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, game) }

  private fun getFiringOtherEffects(triggerEvent: ChangeEvent): List<FiredEffect> {
    val classesInvolved = componentsIn(triggerEvent).map { it.root }
    return game.activeEffects(classesInvolved).mapNotNull { it.onChangeToOther(triggerEvent, game) }
  }

  private fun componentsIn(triggerEvent: ChangeEvent): List<MType> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing).map(game::resolve)
}
