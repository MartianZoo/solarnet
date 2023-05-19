package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomClass
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.abstractInstruction
import dev.martianzoo.tfm.api.Exceptions.orWithoutChoice
import dev.martianzoo.tfm.api.Exceptions.requirementNotMet
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.GameWriterImpl
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Change.Companion.change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import kotlin.math.min

/** Just a cute name for "instruction handler". It prepares and executes instructions. */
public data class Instructor(
  private val writer: GameWriterImpl, // makes sense as inner class but file would be so long
) {
  private val game by writer::game
  private val reader by game::reader

  /**
   * Returns a narrowed form of [unprepared] based on the current game state (but changes no game
   * state itself). The returned instruction *must* be executed against this very same game state
   * (i.e., must be the next one executed. The returned instruction might still be abstract.
   *
   * Preparing iterates to a fixed point. Examples of preparing:
   * * Replaces inert instructions with `Ok`
   * * Auto-narrows gained and removed types to the extent possible
   * * Modifies a `?` or `.` change based on limits (upgrading `.` to `!`)
   * * Validates and removes "gates"
   * * Evaluates a metric in a [Per] instruction, multiplying the inner instruction appropriately
   * * Prepares each option of an [Or] or [Then] (TODO what if gets separated?)
   * * If gaining a *concrete* custom type, rewrites to the result of [CustomClass.translate] *
   */
  fun prepare(unprepared: Instruction) = doPrepare(unprepared)

  private fun doPrepare(unprepared: Instruction): Instruction {
    return when (unprepared) {
      is NoOp -> NoOp
      is Change -> prepareChange(unprepared)
      is Per -> doPrepare(unprepared.instruction * reader.count(unprepared.metric))
      is Gated -> {
        if (reader.evaluate(unprepared.gate)) {
          doPrepare(unprepared.instruction)
        } else if (unprepared.mandatory) {
          throw requirementNotMet(unprepared.gate)
        } else {
          NoOp
        }
      }

      is Or -> {
        val options = unprepared.instructions.nonThrowing(::doPrepare)
        if (options.none()) throw NotNowException("all OR options are impossible at this time")
        Or.create(options.map(::doPrepare))
      }

      is Then -> Then.create(unprepared.instructions.map(::doPrepare).filter { it != NoOp })
      is Multi -> error("")
      is Transform -> error("should have been transformed already: $unprepared")
    }
  }

  private fun <T : Any> Iterable<T>.nonThrowing(block: (T) -> Unit) = filter {
    try {
      block(it)
      true
    } catch (e: Exception) {
      false
    }
  }

  private fun prepareChange(instruction: Change): Instruction {
    // can't prepare at all if we still have an X?
    val count = (instruction.count as? ActualScalar)?.value ?: return instruction

    val (g: MType?, r: MType?) = autoNarrowTypes(instruction.gaining, instruction.removing)
    if (g?.className == DIE) throw DeadEndException("a Die instruction was reached")

    val custom = g != null && !g.abstract && g.root.custom != null
    if (custom) {
      require(r == null) { "custom class instructions can only be pure gains" }
      return prepareCustom(g)
    }

    val intens = instruction.intensity!!

    if (listOfNotNull(g, r).any { it.abstract }) {
      // Still abstract, don't check limits yet
      return change(count, g?.expression, r?.expression, intens)
    }

    val limit: Int =
        game.components.findLimit(
            gaining = g?.toComponent(reader), removing = r?.toComponent(reader),
        )
    val adjusted: Int = min(count, limit)

    if (intens == MANDATORY && adjusted != count) {
      val mesg = if (g != null) {
        if (r == null) {
          "gain $count ${g.expression}"
        } else {
          "transmute $count ${r.expression} into ${g.expression}"
        }
      } else {
        "remove $count ${r!!.expression}"
      }
      throw LimitsException("Can't $mesg: max possible is $adjusted")
    }

    return change(adjusted, g?.expression, r?.expression, if (intens == AMAP) MANDATORY else intens)
  }

  public fun prepareCustom(type: MType?): Instruction {
    val gc = type?.toComponent(reader)
    val cc: CustomClass = type!!.root.custom!!
    val args = gc!!.dependencyComponents
    val missing = args.filter { reader.countComponent(it) == 0 }
    if (missing.any()) throw DependencyException(missing.map { it.mtype })

    val translated: Instruction = cc.translate(reader, args.map { it.mtype }) // TODO whole type?
    val prepper =
        chain(
            reader.transformers.standardPreprocess(),
            reader.transformers.substituter(type.root.baseType, type),
            gc.owner?.let { replaceOwnerWith(it) },
        )
    val prepped = prepper.transform(translated)
    return if (prepped is Multi) prepped else doPrepare(prepped) // TODO hmm?
  }

  private fun autoNarrowTypes(gaining: Expression?, removing: Expression?): Pair<MType?, MType?> {
    var g: MType? = gaining?.let(reader::resolve)
    var r: MType? = removing?.let(reader::resolve)

    if (g?.abstract == true) {
      // Infer a type if there IS only one concrete subtype -- this part could be done sooner
      // TODO filter down to those whose dependents exist
      g = g.allConcreteSubtypes().singleOrNull() ?: g
    }
    if (r?.abstract == true) {
      // Infer a type if there IS only one kind of component that has it
      // TODO could be smarter, like if the instr is mandatory and only one cpt type can satisfy
      r = reader.getComponents(r).singleOrNull() ?: r
    }
    return g to r
  }

  // must prepare first?? or what?? TODO
  fun execute(instruction: Instruction, cause: Cause?) {
    when (instruction) {
      is Change -> {
        val ct = instruction.count as? ActualScalar ?: throw abstractInstruction(instruction)
        if (instruction.intensity != MANDATORY) throw abstractInstruction(instruction)
        val g = instruction.gaining?.toComponent(reader)
        val r = instruction.removing?.toComponent(reader)
        if (g?.mtype?.root?.custom != null) error("custom")
        writer.changeAndFixOrphans(ct.value, g, r, cause, ::fireMatchingTriggers)
      }

      is Or -> throw orWithoutChoice(instruction)
      is Then -> instruction.instructions.forEach { execute(it, cause) }
      is NoOp -> {}
      else -> error("somehow a ${instruction.kind.simpleName!!} was enqueued: $instruction")
    }
  }

  // Effects - TODO Effector

  private fun fireMatchingTriggers(triggerEvent: ChangeEvent) {
    val (now, later) = getFiringEffects(triggerEvent).partition { it.automatic }
    for (fx in now) {
      for (instr in split(fx.instruction)) {
        execute(prepare(instr), fx.cause)
      }
    }

    // TODO why can't we do this before the for loop??
    later.forEach(game::addTriggeredTasks)
  }

  private fun getFiringEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      getFiringSelfEffects(triggerEvent) + getFiringOtherEffects(triggerEvent)

  private fun getFiringSelfEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      componentsIn(triggerEvent)
          .map { Component.ofType(it) }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader) }

  private fun getFiringOtherEffects(triggerEvent: ChangeEvent): List<FiredEffect> {
    val classesInvolved = componentsIn(triggerEvent).map { it.root }
    return game.activeEffects(classesInvolved).mapNotNull {
      it.onChangeToOther(triggerEvent, reader)
    }
  }

  private fun componentsIn(triggerEvent: ChangeEvent): List<MType> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing).map(reader::resolve)
}
