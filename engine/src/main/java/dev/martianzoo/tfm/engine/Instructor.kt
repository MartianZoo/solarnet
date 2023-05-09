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
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.tfm.types.MType
import kotlin.math.min

/** Just a cute name for "instruction handler". It prepares and executes instructions. */
internal data class Instructor(
    private val agent: PlayerAgentImpl, // makes sense as inner class but file would be so long
    private val cause: Cause? = null
) {
  private val game by agent::game

  fun prepare(unprepared: Instruction): Instruction? {
    val prepped = doPrepare(unprepared)
    return if (prepped == NoOp) null else prepped
  }

  /**
   * Returns a narrowed form of [unprepared] based on the current game state (but changes no game
   * state itself). The returned instruction *must* be executed against this very same game state
   * (i.e., must be the next one executed. The returned instruction might still be abstract. If the
   * returned instruction would be [NoOp], returns `null` instead, to signal that the instruction
   * should be discarded.
   */
  private fun doPrepare(unprepared: Instruction): Instruction {
    return when (unprepared) {
      is NoOp -> NoOp
      is Change -> prepareChangeInstruction(unprepared)
      is Per -> doPrepare(unprepared.instruction * agent.reader.count(unprepared.metric))
      is Gated -> {
        if (agent.reader.evaluate(unprepared.gate)) {
          doPrepare(unprepared.instruction)
        } else if (unprepared.mandatory) {
          throw UserException.requirementNotMet(unprepared.gate)
        } else {
          NoOp
        }
      }
      is Custom -> {
        // make sure it exists
        agent.reader.authority.customInstruction(unprepared.functionName)
        unprepared // TODO
      }
      is Or -> {
        /*
         * Try to prepare each option, then form an OR out of what survives. Throwing is very
         * expensive, but does it happen enough to matter? TODO
         */
        val options =
            unprepared.instructions.mapNotNull {
              try {
                doPrepare(it)
              } catch (e: Exception) {
                null // just prune it
              }
            }
        if (options.none()) {
          throw UserException.NotNowException("all OR options are impossible at this time")
        }
        Or.create(options)
      }
      is Then -> Then.create(unprepared.instructions.map(::doPrepare).filter { it != NoOp })
      is Instruction.Multi -> error("should have been split by now: $unprepared")
      is Instruction.Transform -> error("should have been transformed already: $unprepared")
    }
  }

  private fun prepareChangeInstruction(instruction: Change): Instruction {
    // can't prepare at all if we still have an X?
    val scal = instruction.count as? ActualScalar ?: return instruction
    val (g: MType?, r: MType?) = autoNarrowTypes(instruction.gaining, instruction.removing)

    if (g?.abstract == true || r?.abstract == true) {
      // Then narrowing the types some is all we'll do (TODO get more clever)
      return Change.change(
          scal.value,
          gaining = g?.expression,
          removing = r?.expression,
          intensity = instruction.intensity!!,
      )
    }

    val gc = g?.let { Component.ofType(agent.reader.resolve(it.expressionFull) as MType) }
    val rc = r?.let { Component.ofType(agent.reader.resolve(it.expressionFull) as MType) }

    val upperLimit: Int = game.components.findLimit(gaining = gc, removing = rc)
    val adjusted: Int = min(scal.value, upperLimit)

    return if (instruction.intensity!! == MANDATORY) {
      // As long as we're capable of doing the full amount...
      if (adjusted != scal.value) {
        throw UserException.LimitsException(
            "When gaining $gc and removing $rc: can do only $adjusted of ${scal.value} required",
        )
      }
      // Then change nothing...
      return Change.change(scal.value, gaining = gc?.expression, removing = rc?.expression)
    } else {
      val newIntensity =
          if (instruction.intensity == Instruction.Intensity.AMAP) MANDATORY
          else Instruction.Intensity.OPTIONAL
      Change.change(adjusted, gaining = g?.expression, removing = r?.expression, newIntensity)
    }
  }

  private fun autoNarrowTypes(gaining: Expression?, removing: Expression?): Pair<MType?, MType?> {
    var g: MType? = gaining?.let(agent.reader::resolve) as? MType
    var r: MType? = removing?.let(agent.reader::resolve) as? MType

    if (g?.abstract == true) {
      // Infer a type if there IS only one concrete subtype
      // TODO filter down to those whose dependents exist
      g = g.allConcreteSubtypes().singleOrNull() ?: g
    }
    if (r?.abstract == true) {
      // Infer a type if there IS only one kind of component that has it
      // TODO could be smarter, like if the instr is mandatory and only one cpt type can satisfy
      r = agent.reader.getComponents(r).singleOrNull() as? MType ?: r
    }
    return g to r
  }

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
      is Custom -> invokeCustomInstruction(instruction)
      is Or -> throw UserException.orWithoutChoice(instruction)
      is NoOp -> {}
      else -> error("something went wrong: $instruction")
    }
  }

  private fun invokeCustomInstruction(instr: Custom) {
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
      val instructor = copy(cause = fx.cause)
      Instruction.split(fx.instruction).forEach { doPrepare(it)?.let(instructor::execute) }
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
