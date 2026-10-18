package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.search.CanonQuery

private const val GAINS = "Gains"
private const val REMOVES = "Removes"
private const val TRIGGERS = "Triggers"
private const val REQUIREMENTS = "Requirements"
private const val COUNTED = "Counted in"
private const val CONDITIONS = "Conditions"
private const val OTHER = "Other references"
internal val usageCategories =
    listOf(GAINS, REMOVES, TRIGGERS, REQUIREMENTS, COUNTED, CONDITIONS, OTHER)

/** Class references selected from the same elaborated XPath tree as the query editor. */
internal fun buildUsageIndex(
    table: ClassTable,
    query: CanonQuery,
): Map<ClassName, Map<String, Set<ClassName>>> {
  val names = table.allClasses().associateBy { it.className.toString() }
  val index = mutableMapOf<ClassName, MutableMap<String, MutableSet<ClassName>>>()
  val categorizedPairs = mutableSetOf<Pair<ClassName, ClassName>>()

  fun record(xpath: String, category: String) {
    query.evaluate(xpath).forEach { match ->
      val user = names[match.owner]?.className ?: return@forEach
      val target =
          if (category == TRIGGERS && match.value in setOf("This", "-This")) user
          else names[match.value]?.className ?: return@forEach
      val pair = target to user
      if (category == OTHER && pair in categorizedPairs) return@forEach
      index.getOrPut(target, ::linkedMapOf).getOrPut(category, ::linkedSetOf).add(user)
      if (category != OTHER) categorizedPairs.add(pair)
    }
  }

  record(
      "/Canon/*/effects//*[@gainingClass]/@gainingClass | " +
          "/Canon/*/invariants//*[self::Min or self::Exact][@target > 0]" +
          "/countedMetric/Count/expression/Expression[arguments/Expression[@className='This']]/@className",
      GAINS,
  )
  record("/Canon/*/effects//*[@removingClass]/@removingClass", REMOVES)
  record(
      "/Canon/*/effects/Effect/trigger//Expression" +
          "[not(ancestor::condition | ancestor::by)]/@className | " +
          "/Canon/*/effects/Effect/trigger//*[self::WhenGain or self::WhenRemove]/@pets",
      TRIGGERS,
  )
  record(
      "/Canon/*/requirement//Expression/@className | " +
          "/Canon/*/invariants//Expression/@className",
      REQUIREMENTS,
  )
  record(
      "/Canon/*//Count[not(ancestor::invariants or ancestor::effectiveInvariants)]/@countedClass",
      COUNTED,
  )
  record(
      "/Canon/*/effects//gate//Expression/@className | " +
          "/Canon/*/effects//condition//Expression/@className | " +
          "/Canon/*/effects//refinement//Expression/@className",
      CONDITIONS,
  )
  record(
      "/Canon/*/dependencies//Expression/@className | " +
          "/Canon/*/properties//Expression/@className | " +
          "/Canon/*/defaults//Expression/@className | " +
          "/Canon/*/extraNodes//Expression/@className | " +
          "/Canon/*/effects//Expression/@className",
      OTHER,
  )
  return index.mapValues { (_, groups) -> groups.mapValues { (_, users) -> users.toSet() } }
}
