package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.abstractInstruction
import dev.martianzoo.pets.api.Exceptions.orWithoutChoice
import dev.martianzoo.pets.api.Exceptions.requirementNotMet
import dev.martianzoo.pets.api.Exceptions.requirementsNotMetInChoices
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import kotlin.math.min

/** Just a cute name for "instruction handler". It prepares and executes instructions. */
internal class Instructor
internal constructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer?,
    private val effector: Effector?,
    private val classTable: ClassTable,
    private val defaultActor: Actor? = null,
    private val customClasses: CustomClassRuntime =
        CustomClassRuntime(reader.catalog, Transformers(classTable)),
) {
  internal constructor(
      reader: GameReader,
      limiter: Limiter,
      classTable: ClassTable,
  ) : this(reader, limiter, null, null, classTable)

  private val automaticEffectStack = mutableListOf<PendingTask>()
  private val transformDispatcher by lazy { classTable.transformDispatcher() }

  internal fun execute(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor = checkNotNull(defaultActor),
  ): List<PendingTask> = buildList { doExecute(instruction, cause, this, actor) }

  /**
   * Executes a prepared first stage; later linked stages prepare against the state they inherit.
   */
  internal fun executePrepared(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor = checkNotNull(defaultActor),
  ): List<PendingTask> = buildList { doExecutePrepared(instruction, cause, this, actor) }

  private fun doExecute(
      instruction: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
  ) {
    when (val prepared = prepare(instruction)) {
      is Instruction -> doExecutePrepared(prepared, cause, deferred, actor)
      is InstructionGroup -> throw abstractInstruction(prepared)
    }
  }

  private fun doExecutePrepared(
      prepared: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
  ) {
    when (prepared) {
      is Change -> executeChange(prepared, cause, deferred, actor)
      is By -> doExecutePrepared(prepared.inner, cause, deferred, actorFor(prepared))
      is Then ->
          prepared.instructions.forEachIndexed { index, tree ->
            val instruction = tree as? Instruction ?: throw abstractInstruction(tree)
            if (index == 0) {
              doExecutePrepared(instruction, cause, deferred, actor)
            } else {
              doExecute(instruction, cause, deferred, actor)
            }
          }
      is Or -> throw orWithoutChoice(prepared)
      is NoOp -> {}
      else -> error("somehow a ${prepared::class.simpleName} was enqueued: $prepared")
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
        executeAutomaticEffect(task, deferred)
      }
      deferred += effector.fire(result, automatic = false)
      if (done) break
    }
  }

  private fun executeAutomaticEffect(
      task: PendingTask,
      deferred: MutableList<PendingTask>,
  ) {
    if (automaticEffectStack.size >= MAX_AUTOMATIC_EFFECT_DEPTH) {
      throw RunawayEffectChainException(
          MAX_AUTOMATIC_EFFECT_DEPTH,
          (automaticEffectStack + task).map(PendingTask::instruction),
      )
    }
    automaticEffectStack += task
    try {
      task.instruction.instructions.forEach { doExecute(it, task.cause, deferred, task.actor) }
    } finally {
      automaticEffectStack.removeLast()
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
  internal fun prepare(unprepared: Instruction): InstructionTree = doPrepare(unprepared)

  /**
   * Validates a concrete target selected from an abstract pure AMAP gain or removal. Returns true
   * when that kind of selection occurred, so an unprepared task can be locked to this world before
   * retaining the selection.
   */
  internal fun validateAmApSelection(
      wide: InstructionTree,
      proposed: InstructionTree,
  ): Boolean {
    val pairs =
        firstStageChanges(wide).flatMap { domain ->
          firstStageChanges(proposed).mapNotNull { selection ->
            if (selection.change.narrows(domain.change, reader)) domain to selection else null
          }
        }
    val selections = pairs.filter { (domain, selection) ->
      isAbstractPureAmAp(domain.change, selection.change)
    }
    selections.forEach { (domain, selection) ->
      if (
          domain.metricPositive &&
              selection.metricPositive &&
              hasPositiveExecution(domain.change) &&
              !hasPositiveExecution(selection.change)
      ) {
        throw NarrowingException(
            "AMAP target `${selection.change}` cannot execute while " +
                "`${domain.change}` has a positive choice"
        )
      }
    }
    return selections.isNotEmpty()
  }

  private data class FirstStageChange(val change: Change, val metricPositive: Boolean = true)

  private fun firstStageChanges(tree: InstructionTree): List<FirstStageChange> =
      when (tree) {
        is Change -> listOf(FirstStageChange(tree))
        is By -> firstStageChanges(tree.inner)
        is Gated -> firstStageChanges(tree.inner)
        is Per ->
            firstStageChanges(tree.inner).map {
              it.copy(metricPositive = reader.count(tree.metric) > 0)
            }
        is Then -> firstStageChanges(tree.first)
        is Or -> tree.instructions.flatMap(::firstStageChanges)
        is InstructionGroup -> tree.instructions.flatMap(::firstStageChanges)
        else -> emptyList()
      }

  private fun isAbstractPureAmAp(domain: Change, selection: Change): Boolean {
    if (domain.intensity != AMAP) return false
    val domainTarget = domain.gaining ?: domain.removing ?: return false
    if (domain.gaining != null && domain.removing != null) return false
    val selectionTarget = selection.gaining ?: selection.removing ?: return false
    val domainType = reader.resolve(domainTarget)
    return domainType.abstract &&
        !domainType.rootClass.declaration.custom &&
        !reader.resolve(selectionTarget).abstract
  }

  private fun hasPositiveExecution(change: Change): Boolean {
    val gaining = change.gaining?.let(reader::resolve)
    val removing = change.removing?.let(reader::resolve)
    if (listOfNotNull(gaining, removing).any { !classTable.isActive(it) }) return false
    return when {
      gaining != null && removing == null ->
          if (gaining.abstract) {
            !gaining.rootClass.declaration.custom &&
                limiter.hasExecutableConcreteGain(gaining, minimum = 1, reader)
          } else {
            try {
              limiter.findLimit(gaining.toComponent(), null) > 0
            } catch (_: DependencyException) {
              false
            }
          }
      gaining == null && removing != null ->
          if (removing.abstract) {
            limiter.hasExecutableConcreteRemoval(removing, minimum = 1, reader)
          } else {
            limiter.findLimit(null, removing.toComponent()) > 0
          }
      else -> false
    }
  }

  private companion object {
    const val MAX_AUTOMATIC_EFFECT_DEPTH = 8
  }

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
    if (type.className == ENGINE.className) return ENGINE
    return Player.fromClassNameOrNull(type.className)
        ?: throw ExpressionException("unsupported Actor: ${type.expression}")
  }

  // TODO: Split narrowing, limit calculation, and custom-class translation into focused helpers.
  private fun prepareChange(change: Change): InstructionTree {
    val intensity = change.intensity ?: error("missing intensity: $change")
    return try {
      prepareChangeWithoutDependencyFallback(change, intensity)
    } catch (e: DependencyException) {
      val gaining = change.gaining
      val canFallBackToZero =
          intensity != MANDATORY &&
              gaining != null &&
              change.removing == null &&
              (intensity == OPTIONAL || reader.resolve(gaining).abstract) &&
              !classTable.getClass(gaining.className).declaration.custom
      if (canFallBackToZero) NoOp else throw e
    }
  }

  private fun prepareChangeWithoutDependencyFallback(
      change: Change,
      intens: Instruction.Intensity,
  ): InstructionTree {
    // can't prepare at all if we still have an X?
    val count = (change.count as? ActualScalar)?.value ?: return change

    val (g: Type?, r: Type?) =
        autoNarrowTypes(
            change.gaining,
            change.removing,
            preserveAbstractActor = intens == AMAP,
        )
    if (
        change is Transmute &&
            !Change.change(g?.expression, r?.expression, count, intens).narrows(change, reader)
    ) {
      // Independent auto-narrowing must not choose conflicting values for one atomic linkage.
      return change
    }
    if (listOfNotNull(g, r).any { !classTable.isActive(it) }) {
      if (intens != MANDATORY) return NoOp
      throw DeadEndException(
          "mandatory change uses inactive type: " +
              listOfNotNull(g, r).filterNot(classTable::isActive).joinToString()
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
          g?.abstract == true &&
              r == null &&
              intens != OPTIONAL &&
              !g.rootClass.declaration.custom &&
              !limiter.hasExecutableConcreteGain(
                  g,
                  minimum = if (intens == MANDATORY) count else 1,
                  reader,
              )
      ) {
        if (intens == MANDATORY) {
          throw LimitsException(
              "Can't gain $count ${g.expression}: no concrete narrowing can execute"
          )
        }
        return NoOp
      }
      if (g == null && r?.abstract == true) {
        val canRemove =
            if (intens == OPTIONAL) {
              reader.hasAnyComponents(r)
            } else {
              limiter.hasExecutableConcreteRemoval(
                  r,
                  minimum = if (intens == MANDATORY) count else 1,
                  reader,
              )
            }
        if (!canRemove) {
          if (intens == MANDATORY) {
            throw LimitsException("Can't remove $count ${r.expression}: max possible is 0")
          }
          return NoOp
        }
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
          transformDispatcher.transformInstructionTree(customClasses.prepare(gaining!!, reader))
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

    if (listOfNotNull(g, r).any { !classTable.isActive(it) }) return g to r

    if (g?.abstract == true) { // I guess otherwise it'll fail somewhere else...
      val dependencyComponents = g.dependencies.typeDependencies().map { it.boundType }
      val missing = dependencyComponents.filterNot(reader::hasAnyComponents)
      if (missing.any()) throw DependencyException(missing)

      g = classTable.singleConcreteSubtype(g, reader) ?: g
    }

    val hasAbstractActorDependency =
        r?.dependencies?.typeDependencies()?.any {
          it.boundType.abstract && it.boundType.rootClass.isSubtypeOf(classTable.getClass(ACTOR))
        } ?: false
    if (r?.abstract == true && !(preserveAbstractActor && hasAbstractActorDependency)) {
      // Infer a type if there IS only one kind of component that has it
      r =
          reader.getComponents(r).elements.singleOrNull()?.let { classTable.resolve(it.expression) }
              ?: r
    }
    return g to r
  }
}

private fun GameReader.hasAnyComponents(type: Type): Boolean =
    (this as? GameReaderImpl)?.containsAny(type) ?: getComponents(type).isNotEmpty()
