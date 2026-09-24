package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.RunawayEffectChainException
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Transforming
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.recordTypeVariableScopes
import dev.martianzoo.state.Component.Companion.toComponent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.toComponent
import kotlin.math.min

/**
 * Resolves instructions and executes their concrete changes.
 *
 * For each recorded change, all matching automatic effects are executed recursively before queued
 * effects from that change are evaluated and admitted as tasks. Automatic execution may itself
 * produce queued work, but it never enters the task pool. [Effector.fire] owns selection of each
 * effect batch; this class owns the automatic-before-queued execution boundary.
 */
internal class Instructor
internal constructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer,
    private val effector: Effector,
    private val classTable: ClassTable,
    private val elaborator: PetElaborator,
    private val customClasses: CustomInstructionRuntime,
) {
  private val automaticEffectStack = mutableListOf<PendingTask>()

  internal fun execute(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor,
      controller: Actor = actor,
  ): List<PendingTask> = buildList { doExecute(instruction, cause, this, actor, controller) }

  /**
   * Executes a resolved first stage; later shared stages resolve against the state they inherit.
   */
  internal fun executeResolved(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor,
      controller: Actor = actor,
  ): List<PendingTask> = buildList {
    doExecuteResolved(instruction, cause, this, actor, controller)
  }

  private fun doExecute(
      instruction: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
      controller: Actor,
  ) {
    when (val resolved = resolve(instruction)) {
      is Instruction -> doExecuteResolved(resolved, cause, deferred, actor, controller)
      // Independent siblings, such as the branches of a fanout, execute in place here; only an
      // enqueued task turns them into separately selectable work.
      is InstructionGroup ->
          resolved.instructions.forEach { doExecute(it, cause, deferred, actor, controller) }
    }
  }

  private fun doExecuteResolved(
      resolved: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
      controller: Actor,
  ) {
    if (resolved !is Then && resolved.isAbstract(reader)) {
      throw NotFullySpecifiedException("instruction is abstract: $resolved")
    }
    when (resolved) {
      is Change -> executeChange(resolved, cause, deferred, actor, controller)
      is By -> doExecuteResolved(resolved.inner, cause, deferred, actorFor(resolved), controller)
      is Then -> {
        doExecuteResolved(resolved.first, cause, deferred, actor, controller)
        resolved.instructions.drop(1).forEach { tree ->
          InstructionGroup.of(tree).instructions.forEach {
            doExecute(it, cause, deferred, actor, controller)
          }
        }
      }
      is Or -> error("abstract OR passed the execution boundary: $resolved")
      is NoOp -> {}
      else -> error("somehow a ${resolved::class.simpleName} was enqueued: $resolved")
    }
  }

  private fun executeChange(
      instruction: Change,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
      controller: Actor,
  ) {
    val ct = instruction.count as ActualScalar
    check(instruction.quantifier == MANDATORY)

    val gaining = instruction.gaining?.toComponent(reader)
    val removing = instruction.removing?.toComponent(reader)

    while (true) {
      val (result, done) =
          changer.change(
              count = ct.value,
              gaining = gaining,
              removing = removing,
              cause = cause,
              orRemoveOneDependent = true,
              actor = actor,
          )

      val now = effector.fire(result, controller, automatic = true)
      for (task in now) {
        executeAutomaticEffect(task, deferred)
      }
      deferred += effector.fire(result, controller, automatic = false)
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
      task.instruction.instructions.forEach {
        doExecute(it, task.cause, deferred, task.actor, task.controller)
      }
    } finally {
      automaticEffectStack.removeLast()
    }
  }

  /**
   * Returns a narrowed form of [unresolved] based on the current world (but does not change the
   * world itself). The returned instruction tree *must* be executed against this very same world
   * (i.e., must be the next one executed). The returned instruction tree might still be abstract.
   *
   * Resolution iterates to a fixed point. Examples include:
   * * Replaces inert instructions with `Ok`
   * * Auto-narrows gained and removed types to the extent possible
   * * Modifies a `?` or `.` change based on limits (upgrading `.` to `!`)
   * * Validates and removes "gates"
   * * Evaluates a metric in a [Per] instruction, multiplying the inner instruction appropriately
   * * Resolves each option of an [Or]
   * * If gaining a *concrete* custom type, rewrites to the result of [CustomClass.translate]
   *
   * [worldGainNarrowing] permits current-World gain narrowing after a Task is selected.
   */
  internal fun resolve(
      unresolved: Instruction,
      worldGainNarrowing: Boolean = false,
  ): InstructionTree {
    return when (unresolved) {
      is NoOp -> NoOp
      is Change -> resolveChange(unresolved, worldGainNarrowing)
      is By ->
          By.createTree(
              resolve(unresolved.inner, worldGainNarrowing),
              canonicalActorExpression(unresolved),
          )
      is Per -> resolve(unresolved.inner * reader.count(unresolved.metric), worldGainNarrowing)
      is Gated -> {
        if (!reader.has(unresolved.gate)) {
          throw RequirementException("requirement not met: `${unresolved.gate}` / null")
        }
        resolveTree(unresolved.inner, worldGainNarrowing)
      }
      is Each -> resolveEach(unresolved, worldGainNarrowing)
      is Or -> resolveOr(unresolved, worldGainNarrowing)
      is Then -> {
        val first = unresolved.first
        val gated = first as? Gated
        val gateVariables =
            gated
                ?.gate
                ?.descendantsOfType<Expression>()
                ?.mapNotNull(unresolved.typeVariables::variableAt)
                ?.toSet()
                .orEmpty()
        val openGate =
            gated?.inner?.descendantsOfType<Expression>()?.any {
              unresolved.typeVariables.variableAt(it) in gateVariables
            } == true
        val resolvedFirst = if (openGate) first else resolveTree(first, worldGainNarrowing)
        val stages =
            when (resolvedFirst) {
              is InstructionGroup -> resolvedFirst.instructions
              is Then -> resolvedFirst.instructions
              is Instruction -> listOf(resolvedFirst)
            }
        unresolved.withInstructions(stages + unresolved.instructions.drop(1))
      }
      is Transform -> throw ExpressionException("unhandled instruction transform: $unresolved")
    }
  }

  private fun resolveTree(
      unresolved: InstructionTree,
      worldGainNarrowing: Boolean,
  ): InstructionTree =
      if (unresolved is InstructionGroup) unresolved
      else resolve(unresolved as Instruction, worldGainNarrowing)

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
    val type = reader.resolve(instruction.actor)
    if (reader.countComponent(type) != 1) {
      throw ExpressionException("BY requires a participating Actor, not ${type.expression}")
    }
    if (type.className == ADMIN.className) return ADMIN
    if (type.rootClass.isSubtypeOf(classTable.getClass(PLAYER))) {
      return Player(type.className)
    }
    throw ExpressionException("unsupported Actor: ${type.expression}")
  }

  private fun resolveChange(change: Change, worldGainNarrowing: Boolean): InstructionTree {
    val quantifier = change.quantifier ?: error("missing quantifier: $change")
    return try {
      resolveChangeWithoutDependencyFallback(change, quantifier, worldGainNarrowing)
    } catch (e: DependencyException) {
      val gaining = change.gaining
      val canFallBackToZero =
          quantifier != MANDATORY &&
              gaining != null &&
              change.removing == null &&
              (quantifier == OPTIONAL || reader.resolve(gaining).abstract) &&
              !classTable.getClass(gaining.className).declaration.custom
      if (canFallBackToZero) NoOp else throw e
    }
  }

  private fun resolveChangeWithoutDependencyFallback(
      change: Change,
      intens: Instruction.Quantifier,
      worldGainNarrowing: Boolean,
  ): InstructionTree {
    // can't resolve at all if we still have an X?
    val count = (change.count as? ActualScalar)?.value ?: return change
    val atomized = classTable.findClass(ATOMIZED)
    if (
        count > 1 &&
            atomized != null &&
            change.gaining?.let { classTable.getClass(it.className).isSubtypeOf(atomized) } == true
    ) {
      return InstructionGroup(
          List(count) {
            Change.change(change.gaining, change.removing, 1, intens)
          }
      )
    }
    if (change is Transmute) {
      // An exclusion containing an open co-reference becomes meaningful only when an atomic
      // proposal binds that explicitly named variable.
      val variables =
          change.typeVariables.takeUnless { it.isEmpty }
              ?: classTable.recordTypeVariableScopes().transformInstruction(change).typeVariables
      val openExclusion =
          change.descendantsOfType<Not>().any { not ->
            not.excluded.descendantsOfType<Expression>().any {
              variables.variableAt(it) != null
            }
          }
      if (openExclusion) return change
    }

    val (g, r) = narrowChangeTypes(change, count, intens, worldGainNarrowing) ?: return change
    fun retainedExpression(resolved: Type?, authored: Expression?): Expression? =
        resolved?.expression?.let { expression ->
          authored?.let { elaborator.retainTypeVariableNames(expression, it) } ?: expression
        }
    if (listOfNotNull(g, r).any { !classTable.isInhabited(it) }) {
      if (intens != MANDATORY) return NoOp
      throw DeadEndException(
          "mandatory change uses uninhabited type: " +
              listOfNotNull(g, r).filterNot(classTable::isInhabited).joinToString()
      )
    }
    if (g?.className == DIE && intens == MANDATORY) {
      throw DeadEndException("a Die instruction was reached")
    }

    if (listOfNotNull(g, r).any { it.abstract }) {
      // Mandatory needs the whole count from one candidate; AMAP only needs a useful target.
      val required = if (intens == MANDATORY) count else 1

      fun unavailable(reason: String): InstructionTree {
        if (intens == MANDATORY) throw LimitsException("Can't ${describe(g, r, count)}: $reason")
        return NoOp
      }

      if (
          g?.abstract == true &&
              r == null &&
              intens != OPTIONAL &&
              !g.rootClass.declaration.custom &&
              !limiter.hasExecutableConcreteGain(g, required, reader)
      ) {
        return unavailable("no concrete narrowing can execute")
      }
      if (g == null && r?.abstract == true) {
        val canRemove =
            if (intens == OPTIONAL) {
              reader.hasAnyComponents(r)
            } else {
              limiter.hasExecutableConcreteRemoval(r, required, reader)
            }
        if (!canRemove) return unavailable("max possible is 0")
      }
      if (change is Transmute && change.fromEx is Compact) return change
      // Still abstract, don't check limits yet
      return Change.change(
          retainedExpression(g, change.gaining),
          retainedExpression(r, change.removing),
          count,
          intens,
      )
    }

    if (g == r && intens != MANDATORY) return NoOp
    if (g == r) throw ExpressionException("Can't both gain and remove ${g?.expression}")

    translateCustomChange(change, g, r, worldGainNarrowing)?.let {
      return it
    }
    return limitChange(
        g,
        r,
        retainedExpression(g, change.gaining),
        retainedExpression(r, change.removing),
        count,
        intens,
    )
  }

  private fun narrowChangeTypes(
      change: Change,
      count: Int,
      quantifier: Instruction.Quantifier,
      worldGainNarrowing: Boolean,
  ): Pair<Type?, Type?>? {
    val narrowed =
        autoNarrowTypes(
            change.gaining,
            change.removing,
            preserveAbstractActor = quantifier == AMAP,
            worldGainNarrowing = worldGainNarrowing,
        )
    val (gaining, removing) = narrowed
    if (
        change is Transmute &&
            !Change.change(gaining?.expression, removing?.expression, count, quantifier)
                .narrows(change, reader)
    ) {
      // Independent auto-narrowing must not choose conflicting values for one atomic variable.
      return null
    }
    return narrowed
  }

  private fun translateCustomChange(
      original: Change,
      gainingType: Type?,
      removingType: Type?,
      worldGainNarrowing: Boolean,
  ): InstructionTree? {
    if (gainingType?.rootClass?.declaration?.custom != true) return null
    if (removingType != null) {
      throw ExpressionException("custom class instructions can only be pure gains: $original")
    }
    val gaining = gainingType.toComponent()
    val translated = customClasses.translateInstruction(gaining, reader)
    return resolveTree(translated, worldGainNarrowing)
  }

  /** Names one change the way its error messages do: `gain 3 Plant<Player1>`. */
  private fun describe(gaining: Type?, removing: Type?, count: Int): String =
      when {
        gaining == null -> "remove $count ${removing!!.expression}"
        removing == null -> "gain $count ${gaining.expression}"
        else -> "transmute $count ${removing.expression} into ${gaining.expression}"
      }

  private fun limitChange(
      gainingType: Type?,
      removingType: Type?,
      gainingExpression: Expression?,
      removingExpression: Expression?,
      count: Int,
      quantifier: Instruction.Quantifier,
  ): Instruction {
    val gaining = gainingType?.toComponent()
    val removing = removingType?.toComponent()
    val limit = limiter.findLimit(gaining, removing)
    val adjusted: Int = min(count, limit)

    if (quantifier == MANDATORY && adjusted != count) {
      throw LimitsException(
          "Can't ${describe(gainingType, removingType, count)}: max possible is $adjusted"
      )
    }

    return Change.change(
        gainingExpression,
        removingExpression,
        adjusted,
        if (quantifier == AMAP) MANDATORY else quantifier,
    )
  }

  /**
   * Fans one instruction out over the World as it stands right now. Every component matching the
   * selector contributes one independent branch, so a selector refinement — evaluated against each
   * candidate like any other refinement — is how "each player who..." is expressed. The resulting
   * siblings carry no game order.
   */
  private fun resolveEach(each: Each, worldGainNarrowing: Boolean): InstructionTree {
    val selectorType = reader.resolve(each.selector)
    if (!selectorType.abstract) {
      throw ExpressionException(
          "`EACH ${each.selector}` resolves to a concrete Type; `EACH` requires an abstract selector"
      )
    }
    val targets = reader.getComponents(selectorType).map { it.expression }
    val branches = targets.map { branchFor(each, it, worldGainNarrowing) }
    return InstructionGroup.createTree(branches)
  }

  private fun branchFor(
      each: Each,
      selected: Expression,
      worldGainNarrowing: Boolean,
  ): InstructionTree {
    val owner = selected.takeIf { elaborator.selectionSuppliesOwner(each.selector) }
    val bind =
        PetTransformer.chain(
            // This selection, rather than the enclosing context, supplies Owner. The unshielded
            // replacement is intentional.
            owner?.let(Transforming::replaceOwnerWith),
        )
    val bound = bind.transformInstructionTree(each.bodyFor(selected))
    val evaluated = elaborator.evaluateProperties(bound, context = selected, owner = owner)
    return resolveTree(evaluated, worldGainNarrowing)
  }

  /** Resolves each arm against the same world, discarding the unavailable ones. */
  private fun resolveOr(unresolved: Or, worldGainNarrowing: Boolean): InstructionTree {
    val surviving = mutableListOf<InstructionTree>()
    val failures = mutableListOf<Exception>()
    unresolved.instructions.forEach {
      try {
        surviving += resolveTree(it, worldGainNarrowing)
      } catch (e: NotNowException) {
        failures += e
      } catch (e: DeadEndException) {
        failures += e
      }
    }
    if (surviving.any()) return Or.createTree(surviving)

    // No arm survived, so report the strongest applicable failure rather than becoming `Ok`.
    val why = failures.joinToString { it.message.orEmpty() }
    if (failures.any { it is DeadEndException }) throw DeadEndException("no choice remains: $why")
    val unmet = failures.filterIsInstance<RequirementException>()
    if (unmet.size == failures.size) {
      require(unmet.isNotEmpty())
      throw RequirementException(
          "requirements not met in every choice: " + unmet.joinToString { it.message.orEmpty() }
      )
    }
    throw NotNowException("all options impossible: $why")
  }

  // Still spending 25% of solo game time in this method
  private fun autoNarrowTypes(
      gaining: Expression?,
      removing: Expression?,
      preserveAbstractActor: Boolean,
      worldGainNarrowing: Boolean,
  ): Pair<Type?, Type?> {
    var g = gaining?.let(reader::resolve)
    var r = removing?.let(reader::resolve)

    if (listOfNotNull(g, r).any { !classTable.isInhabited(it) }) return g to r

    if (g?.abstract == true) { // I guess otherwise it'll fail somewhere else...
      val dependencyComponents = g.dependencies.typeDependencies().map { it.boundType }
      val missing = dependencyComponents.filterNot(reader::hasAnyComponents)
      if (missing.any()) throw DependencyException(missing)

      g =
          classTable.singleConcreteSubtype(g, reader)
              ?: (if (worldGainNarrowing) {
                limiter.singleConcreteGainWithPresentDependencies(g, r, reader)
              } else null)
              ?: g
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

private const val MAX_AUTOMATIC_EFFECT_DEPTH = 8

private fun GameReader.hasAnyComponents(type: Type): Boolean = getComponents(type).isNotEmpty()
