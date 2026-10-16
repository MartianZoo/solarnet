package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.ClassTable

/**
 * A conservative Canon review guard, not a general proof that an effect cannot require a choice.
 * Expiry listeners may only perform the reviewed turn bookkeeping and enter subsequent play. New
 * fixed rewards also fail: their downstream reactions could introduce a choice.
 */
internal fun actionScopeRemovalViolations(table: ClassTable): List<String> {
  val classes = table.allClasses()
  // Releasing the baton or consuming extra actions can follow scope removal.
  val roots = listOf("ActionScope", "Baton", "ExtraAction").map { table.getClass(cn(it)) }
  val expiring = classes.filter { klass -> roots.any(klass::isSubtypeOf) }.toMutableSet()
  do {
    // Include possible indirect dependents, including Tag<TagHolder> through WildTagElection.
    // Class literals are not lifetime dependencies. Bounds and trigger refinements are deliberately
    // approximated by their root classes: uncertain new forms should request review.
    val added =
        expiring.addAll(
            classes.filter { klass ->
              klass.dependencies.typeDependencies().any { dependency ->
                expiring.any { it.isSubtypeOf(dependency.boundType.rootClass) }
              }
            }
        )
  } while (added)

  return classes.flatMap { owner ->
    owner.declaration.effects
        .filter { effect ->
          val observesExpiry =
              effect.trigger.descendantsOfType<Trigger>().any { trigger ->
                when (trigger) {
                  Trigger.WhenRemove -> expiring.any { it.isSubtypeOf(owner) }
                  is Trigger.OnRemoveOf ->
                      expiring.any { it.isSubtypeOf(table.getClass(trigger.expression.className)) }
                  else -> false
                }
              }
          observesExpiry &&
              !isScopeRemovalHandoff(
                  effect.instruction,
                  table,
                  owner.className == cn("ActionPhase"),
              )
        }
        .map { "${owner.className}: $it" }
  }
}

private fun isScopeRemovalHandoff(
    tree: InstructionTree,
    table: ClassTable,
    actionPhase: Boolean,
): Boolean {
  fun fixedReference(expression: Expression): Boolean =
      expression == THIS.expression ||
          (expression.typeVariableName is Reference &&
              expression.arguments.isEmpty() &&
              expression.refinement == null)

  fun target(expression: Expression, vararg names: String): Boolean =
      expression.className in names.map(::cn) &&
          expression.refinement == null &&
          (!expression.argumentsSpecified || expression.arguments.isNotEmpty()) &&
          expression.arguments.all(::fixedReference)

  fun fixedAmount(change: Instruction.Change): Boolean {
    val defaults =
        table.getClass((change.gaining ?: checkNotNull(change.removing)).className).defaults
    val quantifier =
        change.quantifier
            ?: if (change.gaining != null) defaults.gainOnly.quantifier
            else defaults.removeOnly.quantifier
    return !change.count.abstract && quantifier?.abstract != true
  }

  fun decline(tree: InstructionTree): Boolean =
      tree is Instruction.Remove && target(tree.removing, "Baton") && fixedAmount(tree)

  fun nextAction(tree: InstructionTree): Boolean =
      when (tree) {
        is Instruction.Gated -> nextAction(tree.inner)
        is Instruction.Gain ->
            target(tree.gaining, "StandardAction", "DoRequiredActions") && fixedAmount(tree)
        else -> false
      }

  return when (tree) {
    is InstructionGroup -> tree.instructions.all { isScopeRemovalHandoff(it, table, actionPhase) }
    is Instruction.Then -> tree.instructions.all { isScopeRemovalHandoff(it, table, actionPhase) }
    is Instruction.Dispatch ->
        (tree.actor == null || tree.actor == THIS.expression) &&
            isScopeRemovalHandoff(tree.body, table, actionPhase)
    is Instruction.Gated -> isScopeRemovalHandoff(tree.inner, table, actionPhase)
    is Instruction.Per -> isScopeRemovalHandoff(tree.inner, table, actionPhase)
    // Choosing the next action is allowed; choosing whether/how to clean up the old action is not.
    is Instruction.Or ->
        tree.instructions.any(::nextAction) &&
            tree.instructions.all { nextAction(it) || decline(it) }
    is Instruction.Change ->
        when (tree) {
          is Instruction.Remove -> target(tree.removing, "Baton", "SecondAction", "ExtraAction")
          is Instruction.Gain ->
              target(
                  tree.gaining,
                  "Baton",
                  "OfferAction",
                  "RoundOver",
                  "StandardAction",
                  "DoRequiredActions",
              )
          is Instruction.Transmute ->
              actionPhase &&
                  tree.removing == THIS.expression &&
                  target(tree.gaining, "ProductionPhase")
        } && fixedAmount(tree)
    else -> false
  }
}
