package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Transforming
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.abstractInstruction
import dev.martianzoo.pets.api.Exceptions.orWithoutChoice
import dev.martianzoo.pets.api.Exceptions.requirementNotMet
import dev.martianzoo.pets.api.Exceptions.requirementsNotMetInChoices
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
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
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import kotlin.math.min

/** Just a cute name for "instruction handler". It resolves and executes instructions. */
internal class Instructor
internal constructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer,
    private val effector: Effector,
    private val classTable: ClassTable,
    private val elaborator: PetElaborator,
    private val customClasses: CustomClassRuntime,
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
          resolved.instructions.forEach {
            doExecute(
                it as? Instruction ?: throw abstractInstruction(it),
                cause,
                deferred,
                actor,
                controller,
            )
          }
    }
  }

  private fun doExecuteResolved(
      resolved: Instruction,
      cause: Cause?,
      deferred: MutableList<PendingTask>,
      actor: Actor,
      controller: Actor,
  ) {
    when (resolved) {
      is Change -> executeChange(resolved, cause, deferred, actor, controller)
      is By -> doExecuteResolved(resolved.inner, cause, deferred, actorFor(resolved), controller)
      is Then ->
          resolved.instructions.forEachIndexed { index, tree ->
            val instruction = tree as? Instruction ?: throw abstractInstruction(tree)
            if (index == 0) {
              doExecuteResolved(instruction, cause, deferred, actor, controller)
            } else {
              doExecute(instruction, cause, deferred, actor, controller)
            }
          }
      is Or -> throw orWithoutChoice(resolved)
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
    val ct = instruction.count as? ActualScalar ?: throw abstractInstruction(instruction)
    if (instruction.intensity != MANDATORY) throw abstractInstruction(instruction)

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
   */
  internal fun resolve(unresolved: Instruction): InstructionTree {
    return when (unresolved) {
      is NoOp -> NoOp
      is Change -> resolveChange(unresolved)
      is By -> By.createTree(resolve(unresolved.inner), canonicalActorExpression(unresolved))
      is Per -> resolve(unresolved.inner * reader.count(unresolved.metric))
      is Gated -> {
        if (!reader.has(unresolved.gate)) throw requirementNotMet(unresolved.gate)
        resolveTree(unresolved.inner)
      }
      is Each -> resolveEach(unresolved)
      is Or -> resolveOr(unresolved)
      is Then ->
          unresolved.withInstructions(
              listOf(resolveTree(unresolved.first)) + unresolved.instructions.drop(1)
          )
      is Transform -> throw ExpressionException("unhandled instruction transform: $unresolved")
    }
  }

  private fun resolveTree(unresolved: InstructionTree): InstructionTree =
      if (unresolved is InstructionGroup) unresolved else resolve(unresolved as Instruction)

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

  private fun resolveChange(change: Change): InstructionTree {
    val intensity = change.intensity ?: error("missing intensity: $change")
    return try {
      resolveChangeWithoutDependencyFallback(change, intensity)
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

  private fun resolveChangeWithoutDependencyFallback(
      change: Change,
      intens: Instruction.Intensity,
  ): InstructionTree {
    // can't resolve at all if we still have an X?
    val count = (change.count as? ActualScalar)?.value ?: return change

    val (g, r) = narrowChangeTypes(change, count, intens) ?: return change
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
      // Still abstract, don't check limits yet
      return Change.change(g?.expression, r?.expression, count, intens)
    }

    if (g == r && intens != MANDATORY) return NoOp
    if (g == r) throw ExpressionException("Can't both gain and remove ${g?.expression}")

    translateCustomChange(change, g, r)?.let {
      return it
    }
    return limitChange(g, r, count, intens)
  }

  private fun narrowChangeTypes(
      change: Change,
      count: Int,
      intensity: Instruction.Intensity,
  ): Pair<Type?, Type?>? {
    val narrowed =
        autoNarrowTypes(
            change.gaining,
            change.removing,
            preserveAbstractActor = intensity == AMAP,
        )
    val (gaining, removing) = narrowed
    if (
        change is Transmute &&
            !Change.change(gaining?.expression, removing?.expression, count, intensity)
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
  ): InstructionTree? {
    if (gainingType?.rootClass?.declaration?.custom != true) return null
    if (removingType != null) {
      throw ExpressionException("custom class instructions can only be pure gains: $original")
    }
    val gaining = gainingType.toComponent()
    val translated = customClasses.translateInstruction(gaining, reader)
    return resolveTree(translated)
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
      count: Int,
      intensity: Instruction.Intensity,
  ): Instruction {
    val gaining = gainingType?.toComponent()
    val removing = removingType?.toComponent()
    val limit = limiter.findLimit(gaining, removing)
    val adjusted: Int = min(count, limit)

    if (intensity == MANDATORY && adjusted != count) {
      throw LimitsException(
          "Can't ${describe(gainingType, removingType, count)}: max possible is $adjusted"
      )
    }

    return Change.change(
        gainingType?.expression,
        removingType?.expression,
        adjusted,
        if (intensity == AMAP) MANDATORY else intensity,
    )
  }

  /**
   * Fans one instruction out over the World as it stands right now. Every component matching the
   * selector contributes one independent branch, so a selector refinement — evaluated against each
   * candidate like any other refinement — is how "each player who..." is expressed. The resulting
   * siblings carry no order, so they are deliberately produced in a stable but arbitrary sort.
   */
  private fun resolveEach(each: Each): InstructionTree {
    val selectorType = reader.resolve(each.selector)
    if (!selectorType.abstract) {
      throw ExpressionException(
          "`EACH ${each.selector}` selects one concrete Type, so it would have a single " +
              "branch. Select an abstract type whose matching components can differ."
      )
    }
    val ownsBody = elaborator.selectionSuppliesOwner(each.selector)
    val named =
        each.body.descendantsOfType<Expression>().any {
          it == each.selectorName ||
              it == each.representedSelectorName ||
              (ownsBody && it.className == OWNER)
        }
    if (!named) {
      throw ExpressionException(
          "`EACH ${each.selector}` never names its selection in `${each.body}`, " +
              "so every branch would be the same instruction"
      )
    }
    val selected =
        reader.getComponents(selectorType).elements.map { it.expression }.sortedBy { "$it" }
    val branches = selected.map { branchFor(each, it) }
    return InstructionGroup.createTree(branches)
  }

  private fun branchFor(each: Each, selected: Expression): InstructionTree {
    val owner = selected.takeIf { elaborator.selectionSuppliesOwner(each.selector) }
    val representedSelection =
        each.representedSelectorName?.let {
          check(selected.className == CLASS)
          selected.arguments.single()
        }
    val bind =
        PetTransformer.chain(
            replacer(each.selectorName, selected),
            each.representedSelectorName?.let { replacer(it, checkNotNull(representedSelection)) },
            // This selection, rather than the enclosing context, supplies Owner. The unshielded
            // replacement is intentional.
            owner?.let(Transforming::replaceOwnerWith),
        )
    val bound = bind.transformInstructionTree(each.body)
    val evaluated = elaborator.evaluateProperties(bound, context = selected, owner = owner)
    return resolveTree(evaluated)
  }

  /** Resolves each arm against the same world, discarding the unavailable ones. */
  private fun resolveOr(unresolved: Or): InstructionTree {
    val surviving = mutableListOf<InstructionTree>()
    val failures = mutableListOf<Exception>()
    unresolved.instructions.forEach {
      try {
        surviving += resolveTree(it)
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
    if (unmet.size == failures.size) throw requirementsNotMetInChoices(unmet)
    throw NotNowException("all options impossible: $why")
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

private const val MAX_AUTOMATIC_EFFECT_DEPTH = 8

private fun GameReader.hasAnyComponents(type: Type): Boolean =
    (this as? GameReaderImpl)?.containsAny(type) ?: getComponents(type).isNotEmpty()
