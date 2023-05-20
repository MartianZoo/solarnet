package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomClass
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
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
internal data class Instructor(
    private val writer: GameWriterImpl, // makes sense as inner class but file would be so long
    private val effector: Effector,
    private val addTasks: (FiredEffect) -> Unit,
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
      is Per -> doPrepare(unprepared.inner * reader.count(unprepared.metric))
      is Gated -> {
        if (reader.evaluate(unprepared.gate)) {
          doPrepare(unprepared.inner)
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

    if (g != null && !g.abstract && g.root.custom != null) {
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
            gaining = g?.toComponent(reader), removing = r?.toComponent(reader))
    val adjusted: Int = min(count, limit)

    if (intens == MANDATORY && adjusted != count) {
      val mesg =
          if (g != null) {
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

  public fun prepareCustom(type: MType): Instruction {
    val translated = type.root.custom!!.prepare(reader, type)

    val prepped = chain(
        reader.transformers.standardPreprocess(),
        reader.transformers.substituter(type.root.baseType, type),
        type.owner?.let { replaceOwnerWith(it) },
    ).transform(translated)

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

  fun execute(instruction: Instruction, cause: Cause?) {
    val prepped = prepare(instruction) // idempotent?
    when (prepped) {
      is Change -> {
        val ct = prepped.count as? ActualScalar ?: throw abstractInstruction(prepped)
        if (prepped.intensity != MANDATORY) throw abstractInstruction(prepped)
        val g = prepped.gaining?.toComponent(reader)
        val r = prepped.removing?.toComponent(reader)
        if (g?.mtype?.root?.custom != null) error("custom")
        writer.changeAndFixOrphans(ct.value, g, r, cause) { fireMatchingTriggers(it) }
      }
      is Or -> throw orWithoutChoice(prepped)
      is Then -> prepped.instructions.forEach { execute(it, cause) }
      is NoOp -> {}
      else -> error("somehow a ${prepped.kind.simpleName!!} was enqueued: $prepped")
    }
  }

  private fun fireMatchingTriggers(triggerEvent: ChangeEvent) {
    val (now, later) = effector.fire(triggerEvent, reader).partition { it.automatic }
    for (fx in now) {
      split(fx.instruction).forEach { execute(it, fx.cause) }
    }
    later.forEach(addTasks) // TODO why can't we do this before the for loop??
  }
}
