package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.ClassTable

/**
 * Finds production counts that may include MC without a matching offset in the arithmetic. Input
 * has already passed through the Catalog's transform dispatcher, so PROD syntax and explicit
 * Production counts are identical here. This recognizes current authoring forms, not algebraic
 * equivalence: a new compensation formula should fail for review rather than be silently accepted.
 */
internal fun unadjustedMoneyProduction(metric: Metric, table: ClassTable): List<Metric.Count> {
  val production = table.getClass(cn("Production"))
  val money = table.getClass(cn("MC"))

  fun countsMoneyProduction(count: Metric.Count): Boolean {
    if (table.findClass(count.expression.className)?.isSubtypeOf(production) != true) return false
    val resource =
        count.expression.arguments.singleOrNull { it.className == CLASS }?.arguments?.singleOrNull()
    // Bare Production and broad resource selectors can include MC too.
    return resource == null || table.findClass(resource.className)?.let(money::isSubtypeOf) == true
  }

  fun isMatchingOffset(candidate: Metric, count: Metric.Count): Boolean =
      candidate is Metric.Count &&
          candidate.expression == count.expression.copy(className = cn("ProdOffset"))

  fun inspect(node: Metric): List<Metric.Count> =
      when (node) {
        is Metric.Count -> listOfNotNull(node.takeIf(::countsMoneyProduction))
        is Metric.Subtract -> {
          val current = node.minuend as? Metric.Count
          val removed = node.subtrahend as? Metric.Count
          when {
            // Printed production = stored production minus its offset, for the same resource/owner.
            current != null && isMatchingOffset(node.subtrahend, current) -> emptyList()
            // Industrial Complex: (target OR offset) - stored production. The offset increases the
            // stored target, rather than being subtracted from a count before the outer
            // subtraction.
            removed != null &&
                (node.minuend as? Metric.Or)?.metrics?.any {
                  isMatchingOffset(it, removed)
                } == true -> inspect(node.minuend)
            else -> inspect(node.minuend) + inspect(node.subtrahend)
          }
        }
        else -> node.immediateChildren().filterIsInstance<Metric>().flatMap(::inspect)
      }

  return inspect(metric)
}
