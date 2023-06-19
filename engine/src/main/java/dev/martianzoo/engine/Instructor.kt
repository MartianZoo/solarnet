package dev.martianzoo.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.abstractInstruction
import dev.martianzoo.api.Exceptions.orWithoutChoice
import dev.martianzoo.api.Exceptions.requirementNotMet
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Task
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.PlayerScoped
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Change.Companion.change
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType
import javax.inject.Inject
import kotlin.math.min

/** Just a cute name for "instruction handler". It prepares and executes instructions. */
@PlayerScoped
internal class Instructor
@Inject
constructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer?,
    private val effector: Effector?,
    private val table: MClassTable,
) {

  fun execute(instruction: Instruction, cause: Cause?): List<Task> {
    val list = mutableListOf<Task>()
    doExecute(instruction, cause, list)
    return list
  }

  private fun doExecute(instruction: Instruction, cause: Cause?, deferred: MutableList<Task>) {
    when (val prepped = prepare(instruction)) { // idempotent?
      is Change -> executeChange(prepped, cause, deferred)
      is Then -> prepped.instructions.forEach { doExecute(it, cause, deferred) }
      is Or -> throw orWithoutChoice(prepped)
      is NoOp -> {}
      else -> error("somehow a ${prepped.kind.simpleName!!} was enqueued: $prepped")
    }
  }

  private fun executeChange(instruction: Change, cause: Cause?, deferred: MutableList<Task>) {
    val ct = instruction.count as? ActualScalar ?: throw abstractInstruction(instruction)
    if (instruction.intensity != MANDATORY) throw abstractInstruction(instruction)

    val gaining = instruction.gaining?.toComponent(reader)
    val removing = instruction.removing?.toComponent(reader)

    while (true) {
      val (result, done) =
          changer!!.change(
              count = ct.value,
              gaining = gaining,
              removing = removing,
              cause = cause,
              orRemoveOneDependent = true)

      val consequences: List<Task> = try {
        effector!!.fire(result)
      } catch (e: Exception) {
        println("triggers from $result")
        throw e
      }
      val (now, later) = consequences.partition { it.next }
      for (task in now) {
        split(task.instruction).forEach { doExecute(it, task.cause, deferred) }
      }
      deferred += later
      if (done) break
    }
  }

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
   * * Prepares each option of an [Or]
   * * If gaining a *concrete* custom type, rewrites to the result of [CustomClass.translate] *
   */
  fun prepare(unprepared: Instruction) = doPrepare(unprepared)

  private fun doPrepare(unprepared: Instruction): Instruction {
    return when (unprepared) {
      is NoOp -> NoOp
      is Change -> prepareChange(unprepared)
      is Per -> doPrepare(unprepared.inner * reader.count(unprepared.metric))
      is Gated -> {
        if (reader.has(unprepared.gate)) {
          doPrepare(unprepared.inner)
        } else if (unprepared.mandatory) {
          throw requirementNotMet(unprepared.gate)
        } else {
          NoOp
        }
      }
      is Or -> prepareOr(unprepared)
      is Then ->
          Then.create(
              listOf(doPrepare(unprepared.instructions.first())) + unprepared.instructions.drop(1))
      is Multi -> error("")
      is Transform -> error("should have been transformed already: $unprepared")
    }
  }

  private fun prepareChange(change: Change): Instruction {
    // can't prepare at all if we still have an X?
    val count = (change.count as? ActualScalar)?.value ?: return change

    val (g: MType?, r: MType?) = autoNarrowTypes(change.gaining, change.removing)
    if (g?.className == DIE) throw DeadEndException("a Die instruction was reached")

    val intens = change.intensity ?: error("missing intensity: $change")

    if (listOfNotNull(g, r).any { it.abstract }) {
      // Still abstract, don't check limits yet
      return change(count, g?.expression, r?.expression, intens)
    }

    if (g == r) throw ExpressionException("Can't both gain and remove ${g?.expression}")

    val gaining = g?.toComponent()
    val removing = r?.toComponent()

    if (g?.root?.custom != null) {
      require(r == null) { "custom class instructions can only be pure gains" }
      val translated = reader.preprocess(gaining!!.prepareCustom(reader))
      return if (translated is Multi) translated else doPrepare(translated)
    }

    val limit = limiter.findLimit(gaining, removing)
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

  private fun prepareOr(unprepared: Or): Instruction {
    val options: List<Any> =
        unprepared.instructions.map {
          try {
            if (it is Multi) it else doPrepare(it)
          } catch (e: NotNowException) {
            e
          }
        }
    val good = options.filterIsInstance<Instruction>()
    return if (good.any()) {
      Or.create(good)
    } else {
      throw NotNowException("all options impossible: $options")
    }
  }

  // Still spending 25% of solo game time in this method
  // TODO make it faster and/or need it less
  private fun autoNarrowTypes(gaining: Expression?, removing: Expression?): Pair<MType?, MType?> {
    var g = gaining?.let(reader::resolve) as MType?
    var r = removing?.let(reader::resolve) as MType?

    if (g?.abstract == true) { // I guess otherwise it'll fail somewhere else...
      val missing =
          g.dependencies.typeDependencies().map { it.boundType }.filter { !reader.containsAny(it) }
      if (missing.any()) throw DependencyException(missing)

      g = g.singleConcreteSubtype() ?: g
    }

    if (r?.abstract == true) {
      // Infer a type if there IS only one kind of component that has it
      // TODO could be smarter, like if the instr is mandatory and only one cpt type can satisfy
      r = reader.getComponents(r).singleOrNull()?.let { table.resolve(it.expression) } ?: r
    }
    return g to r
  }
}
