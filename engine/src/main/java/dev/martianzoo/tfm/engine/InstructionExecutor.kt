package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Game.PlayerAgent
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.tfm.types.MType

internal data class InstructionExecutor(
    val agent: PlayerAgent,
    val cause: Cause?,
) {
  fun doInstruction(instr: Instruction) {
    return when (instr) {
      is NoOp -> {}
      is Change -> handleChange(instr)
      is Per -> doInstruction(instr.instruction * agent.reader.count(instr.metric))
      is Gated -> {
        if (!agent.reader.evaluate(instr.gate)) {
          if (instr.mandatory) throw UserException.requirementNotMet(instr.gate)
          return // do nothing
        }
        doInstruction(instr.instruction)
      }
      is Custom -> handleCustomInstruction(instr)
      is Or -> throw UserException.orWithoutChoice(instr)
      is Then -> split(instr.instructions).forEach { doInstruction(it) }
      is Multi -> error("should have been split: $instr")
      is Transform -> error("should have been transformed already: $instr")
    }
  }

  private fun handleChange(instr: Change) {
    val scal = instr.count as? ActualScalar ?: throw UserException.unresolvedX(instr)

    val amap =
        when (instr.intensity) {
          null -> error("should have had defaults inserted: $instr")
          MANDATORY -> false
          AMAP -> true
          OPTIONAL -> throw UserException.optionalAmount(instr)
        }

    val result =
        agent.update(
            count = scal.value,
            gaining = instr.gaining?.let(agent.reader::resolve),
            removing = instr.removing?.let(agent.reader::resolve),
            amap = amap,
            cause = cause)
    result.changes.forEach(::fireTriggers)
  }

  private fun handleCustomInstruction(instr: Custom) {
    val arguments = instr.arguments.map(agent.reader::resolve)
    val abstractArgs = arguments.filter { it.abstract }
    if (abstractArgs.any()) throw UserException.abstractArguments(abstractArgs, instr)

    val custom = agent.reader.authority.customInstruction(instr.functionName)
    val translated: Instruction = custom.translate(agent.reader, arguments) * instr.multiplier

    // I guess custom instructions can't return things using `This`
    // and Owner means the context player... (TODO think)
    val xers = agent.game.transformers
    val instruction =
        chain(
                xers.atomizer(),
                xers.insertDefaults(), // TODO context component??
                xers.deprodify(),
                replaceOwnerWith(agent.player),
                TransformNode.unwrapper(RAW),
            )
            .transform(translated)

    agent.addTasks(instruction, agent.player, cause)
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val (now, later) = getFiringEffects(triggerEvent).partition { it.automatic }

    for (fx in now) {
      val executor = copy(cause = fx.cause)
      split(fx.instruction).forEach(executor::doInstruction)
    }
    agent.game.addTriggeredTasks(later) // TODO why can't we do this before??
  }

  private fun getFiringEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      getFiringSelfEffects(triggerEvent) + getFiringOtherEffects(triggerEvent)

  // TODO can the differences be collapsed some?

  private fun getFiringSelfEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      componentsIn(triggerEvent)
          .map { Component.ofType(it) }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, agent.game) }

  private fun getFiringOtherEffects(triggerEvent: ChangeEvent): List<FiredEffect> {
    val classesInvolved = componentsIn(triggerEvent).map { it.root }
    return agent.game.activeEffects(classesInvolved).mapNotNull {
      it.onChangeToOther(triggerEvent, agent.game)
    }
  }

  private fun componentsIn(triggerEvent: ChangeEvent): List<MType> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
          .map(agent.game::resolve)
}
