package dev.martianzoo.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.api.Exceptions.abstractInstruction
import dev.martianzoo.api.Exceptions.orWithoutChoice
import dev.martianzoo.api.Exceptions.requirementNotMet
import dev.martianzoo.api.Exceptions.requirementsNotMetInChoices
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.ACTOR
import dev.martianzoo.api.SystemClasses.ATOMIZED
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.Prod
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import kotlin.math.min

/** Just a cute name for "instruction handler". It prepares and executes instructions. */
internal class Instructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer?,
    private val effector: Effector?,
    private val classTable: ClassTable,
    private val defaultActor: Actor? = null,
    private val customClasses: CustomClassRuntime =
        CustomClassRuntime(reader.authority, Transformers(classTable)),
) {

  internal fun execute(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor = checkNotNull(defaultActor),
  ): List<PendingTask> = buildList {
    doExecute(instruction, cause, this, actor)
  }

  private fun doExecute(
      instruction: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
  ) {
    when (val prepped = prepare(instruction)) { // idempotent?
      is Change -> executeChange(prepped, cause, deferred, actor)
      is By -> doExecute(prepped.inner, cause, deferred, actorFor(prepped))
      is Then ->
          prepped.instructions.forEach {
            doExecute(it as? Instruction ?: throw abstractInstruction(it), cause, deferred, actor)
          }
      is Or -> throw orWithoutChoice(prepped)
      is NoOp -> {}
      is InstructionGroup -> throw abstractInstruction(prepped)
      else -> error("somehow a ${prepped::class.simpleName} was enqueued: $prepped")
    }
  }

  private fun executeChange(
      instruction: Change,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
  ) {
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
              orRemoveOneDependent = true,
              actor = actor,
          )

      val now = effector!!.fire(result, automatic = true)
      for (task in now) {
        task.instruction.instructions.forEach { doExecute(it, task.cause, deferred, task.actor) }
      }
      deferred += effector.fire(result, automatic = false)
      if (done) break
    }
  }

  /**
   * Returns a narrowed form of [unprepared] based on the current world (but does not change the
   * world itself). The returned instruction tree *must* be executed against this very same world
   * (i.e., must be the next one executed). The returned instruction tree might still be abstract.
   *
   * Preparing iterates to a fixed point. Examples of preparing:
   * * Replaces inert instructions with `Ok`
   * * Auto-narrows gained and removed types to the extent possible
   * * Modifies a `?` or `.` change based on limits (upgrading `.` to `!`)
   * * Validates and removes "gates"
   * * Evaluates a metric in a [Per] instruction, multiplying the inner instruction appropriately
   * * Prepares each option of an [Or]
   * * If gaining a *concrete* custom type, rewrites to the result of [CustomClass.translate]
   */
  internal fun prepare(unprepared: Instruction) = doPrepare(unprepared)

  private fun prepareTree(unprepared: InstructionTree): InstructionTree =
      if (unprepared is InstructionGroup) unprepared else doPrepare(unprepared as Instruction)

  private fun doPrepare(unprepared: Instruction): InstructionTree {
    return when (unprepared) {
      is NoOp -> NoOp
      is Change -> prepareChange(unprepared)
      is By -> By.createTree(doPrepare(unprepared.inner), canonicalActorExpression(unprepared))
      is Per -> doPrepare(unprepared.inner * reader.count(unprepared.metric))
      is Gated -> {
        if (!reader.has(unprepared.gate)) throw requirementNotMet(unprepared.gate)
        prepareTree(unprepared.inner)
      }
      is Or -> prepareOr(unprepared)
      is Then ->
          unprepared.withInstructions(
              listOf(prepareTree(unprepared.first)) + unprepared.instructions.drop(1)
          )
      is Transform -> throw ExpressionException("unhandled instruction transform: $unprepared")
    }
  }

  private fun canonicalActorExpression(instruction: By): Expression {
    val type = reader.resolve(instruction.actor)
    if (!type.rootClass.isSubtypeOf(classTable.getClass(ACTOR))) {
      throw ExpressionException("BY requires an Actor, not ${instruction.actor}")
    }
    if (type.abstract) {
      throw ExpressionException("BY requires one concrete Actor, not ${instruction.actor}")
    }
    return type.expression
  }

  private fun actorFor(instruction: By): Actor {
    val type = reader.resolve(canonicalActorExpression(instruction))
    if (reader.countComponent(type) != 1) {
      throw ExpressionException("BY requires a participating Actor, not ${type.expression}")
    }
    return when {
      type.className == ENGINE.className -> ENGINE
      Player.isValid(type.className) -> Player(type.className)
      else -> throw ExpressionException("unsupported Actor: ${type.expression}")
    }
  }

  // TODO: Split narrowing, limit calculation, and custom-class translation into focused helpers.
  private fun prepareChange(change: Change): InstructionTree {
    // can't prepare at all if we still have an X?
    val count = (change.count as? ActualScalar)?.value ?: return change
    val intens = change.intensity ?: error("missing intensity: $change")

    val (g: Type?, r: Type?) =
        try {
          autoNarrowTypes(
              change.gaining,
              change.removing,
              preserveAbstractActor = intens == AMAP,
          )
        } catch (e: DependencyException) {
          if (intens == AMAP && change.gaining != null && change.removing == null) return NoOp
          throw e
        }
    if (
        change is Transmute &&
            !Change.change(g?.expression, r?.expression, count, intens).narrows(change, reader)
    ) {
      // Independent auto-narrowing must not choose conflicting values for one atomic linkage.
      return change
    }
    if (listOfNotNull(g, r).any(Type::phantom)) {
      if (intens != MANDATORY) return NoOp
      throw DeadEndException(
          "mandatory change uses inactive type: " +
              listOfNotNull(g, r).filter(Type::phantom).joinToString()
      )
    }
    if (g?.className == DIE) throw DeadEndException("a Die instruction was reached")

    val atomized = classTable.findClass(ATOMIZED)
    if (r != null && count > 1 && atomized != null && g?.rootClass?.isSubtypeOf(atomized) == true) {
      throw ExpressionException(
          "Can't transmute $count components into atomized type ${g.expression}; " +
              "split it into one-component transmutations"
      )
    }

    if (listOfNotNull(g, r).any { it.abstract }) {
      if (
          intens == AMAP &&
              g?.abstract == true &&
              r == null &&
              limiter.findAbstractGainLimit(g) == 0
      ) {
        return NoOp
      }
      if (intens == AMAP && g == null && r?.abstract == true && !reader.containsAny(r)) {
        return NoOp
      }
      // Still abstract, don't check limits yet
      return Change.change(g?.expression, r?.expression, count, intens)
    }

    if (g == r && intens != MANDATORY) return NoOp
    if (g == r) throw ExpressionException("Can't both gain and remove ${g?.expression}")

    val gaining = g?.toComponent()
    val removing = r?.toComponent()

    if (g?.rootClass?.declaration?.custom == true) {
      if (r != null) {
        throw ExpressionException("custom class instructions can only be pure gains: $change")
      }
      val translated =
          Prod.deprodify(classTable)
              .transformInstructionTree(customClasses.prepare(gaining!!, reader))
      return prepareTree(translated)
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

    return Change.change(
        g?.expression,
        r?.expression,
        adjusted,
        if (intens == AMAP) MANDATORY else intens,
    )
  }

  private fun prepareOr(unprepared: Or): InstructionTree {
    val options: List<Any> =
        unprepared.instructions.map {
          try {
            prepareTree(it)
          } catch (e: NotNowException) {
            e
          } catch (e: DeadEndException) {
            e
          }
        }
    val good = options.filterIsInstance<InstructionTree>()
    return if (good.any()) {
      Or.createTree(good)
    } else if (options.any { it is DeadEndException }) {
      throw DeadEndException("every choice reaches an inactive type: $options")
    } else if (options.all { it is RequirementException }) {
      throw requirementsNotMetInChoices(options.filterIsInstance<RequirementException>())
    } else {
      throw NotNowException("all options impossible: $options")
    }
  }

  // Still spending 25% of solo game time in this method
  private fun autoNarrowTypes(
      gaining: Expression?,
      removing: Expression?,
      preserveAbstractActor: Boolean,
  ): Pair<Type?, Type?> {
    var g = gaining?.let(reader::resolve)
    var r = removing?.let(reader::resolve)

    if (listOfNotNull(g, r).any(Type::phantom)) return g to r

    if (g?.abstract == true) { // I guess otherwise it'll fail somewhere else...
      val dependencyComponents = g.dependencies.typeDependencies().map { it.boundType }
      val missing = dependencyComponents.filterNot(reader::containsAny)

      // TODO this needs to not happen (or be intercepted) if the instruction is non-mandatory!
      if (missing.any()) throw DependencyException(missing)

      g = g.singleConcreteSubtype(reader) ?: g
    }

    val hasAbstractActorDependency =
        r?.dependencies?.typeDependencies()?.any {
          it.boundType.abstract && it.boundType.rootClass.isSubtypeOf(classTable.getClass(ACTOR))
        } ?: false
    if (r?.abstract == true && !(preserveAbstractActor && hasAbstractActorDependency)) {
      // Infer a type if there IS only one kind of component that has it
      r =
          reader.getComponents(r).elements.singleOrNull()?.let {
            classTable.resolve(it.expression)
          } ?: r
    }
    return g to r
  }
}
