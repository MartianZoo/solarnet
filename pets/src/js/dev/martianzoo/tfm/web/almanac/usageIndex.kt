package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.ClassTable

private const val GAINS = "Gains"
private const val REMOVES = "Removes"
private const val GATES = "Gates"
private const val REQUIREMENTS = "Requirements"
private const val OTHER_METRICS = "Other Metrics"
private const val TRIGGERS = "Triggers"
private const val INVARIANTS = "Invariants"
private const val OTHER = "Other references"
internal val usageCategories: List<String> =
    listOf(GAINS, REMOVES, GATES, REQUIREMENTS, OTHER_METRICS, TRIGGERS, INVARIANTS, OTHER)

/** Index declaration references by their role, rather than every enclosing AST kind. */
internal fun buildUsageIndex(table: ClassTable): Map<ClassName, Map<String, Set<ClassName>>> {
  val index = mutableMapOf<ClassName, MutableMap<String, MutableSet<ClassName>>>()
  val transforms = table.transformDispatcher()
  table.allClasses().forEach { user ->
    fun record(target: ClassName, category: String) {
      index.getOrPut(target, ::linkedMapOf).getOrPut(category, ::linkedSetOf).add(user.className)
    }
    val declaration = user.declaration
    // allNodes already includes executable actions. Lower PROD with the existing handler too.
    val roots =
        declaration.allNodes -
            declaration.className -
            declaration.supertypes -
            declaration.invariants
    roots.forEach { collectUsages(transforms.transformWithoutKindCheck(it), OTHER, ::record) }
    Requirement.split(declaration.invariants).forEach { invariant ->
      val lowered = transforms.transformRequirement(invariant)
      val count = lowered as? Requirement.Counting
      val expression = (count?.metric as? Metric.Count)?.expression
      if (
          count != null &&
              count !is Requirement.Max &&
              count.target > 0 &&
              expression != null &&
              THIS.expression in expression.arguments
      ) {
        // A newly gained component brings its declared contents (tags, attachments, etc.).
        collectUsages(expression, GAINS, ::record)
      } else {
        collectUsages(lowered, INVARIANTS, ::record)
      }
    }
  }
  return index.mapValues { (_, categories) -> categories.mapValues { (_, users) -> users.toSet() } }
}

private fun collectUsages(node: PetNode, category: String, record: (ClassName, String) -> Unit) {
  fun visit(child: PetNode, role: String = category) = collectUsages(child, role, record)
  when (node) {
    is Change -> {
      node.gaining?.let { visit(it, GAINS) }
      node.removing?.let { visit(it, REMOVES) }
    }
    is Gated -> {
      visit(node.gate, GATES)
      visit(node.inner, OTHER)
    }
    is Effect -> {
      visit(node.trigger, TRIGGERS)
      visit(node.instruction, OTHER)
    }
    is Effect.Trigger.IfTrigger -> {
      visit(node.inner, TRIGGERS)
      visit(node.condition, GATES)
    }
    is Effect.Trigger.ByTrigger -> {
      visit(node.inner, TRIGGERS)
      visit(node.by, OTHER)
    }
    is Requirement ->
        node.childNodes().forEach {
          visit(it, if (category == GATES || category == INVARIANTS) category else REQUIREMENTS)
        }
    is Metric ->
        node.childNodes().forEach {
          visit(
              it,
              if (category in setOf(GATES, REQUIREMENTS, INVARIANTS)) category else OTHER_METRICS,
          )
        }
    is Expression -> {
      record(node.className, category)
      // Gaining Microbe<SomeCard> gains a Microbe; it only references SomeCard.
      node.arguments.forEach { visit(it, OTHER) }
      node.refinement?.let { visit(it, GATES) }
    }
    is ClassName -> record(node, category)
    else -> node.childNodes().forEach { visit(it) }
  }
}
