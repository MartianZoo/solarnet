package dev.martianzoo.pets.types

import dev.martianzoo.pets.types.ClassLimitTable.Limit
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ClassLimitTableTest {
  @Test
  internal fun inheritedRequiredCountsApplyOnlyToLiveOwners() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Anchor { HAS MAX 1 This, =2 Part<This> }",
            "CLASS First : Anchor",
            "CLASS Second : Anchor",
            "CLASS Part<Anchor>",
        )
    val first = table.resolve(te("First"))
    val second = table.resolve(te("Second"))
    val limits = table.componentLimits

    limits.requiredLimits(listOf(first), changedTypes = emptyList()) shouldBe
        setOf(Limit(table.resolve(te("Part<First>")), 2..2))
    limits.requiredLimits(listOf(first, second), changedTypes = emptyList()) shouldBe
        setOf(
            Limit(table.resolve(te("Part<First>")), 2..2),
            Limit(table.resolve(te("Part<Second>")), 2..2),
        )
    limits.requiredLimits(emptyList(), changedTypes = emptyList()) shouldBe emptySet()
  }

  @Test
  internal fun requiredCountsBindEveryLiveSpecializationOfTheSameClass() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Anchor { HAS MAX 1 This }",
            "CLASS First : Anchor",
            "CLASS Second : Anchor",
            "CLASS Holder<Anchor> { HAS MAX 1 This, =2 Part<This> }",
            "CLASS Part<Holder>",
        )
    val first = table.resolve(te("Holder<First>"))
    val second = table.resolve(te("Holder<Second>"))

    table.componentLimits.requiredLimits(listOf(first, second), changedTypes = emptyList()) shouldBe
        setOf(
            Limit(table.resolve(te("Part<Holder<First>>")), 2..2),
            Limit(table.resolve(te("Part<Holder<Second>>")), 2..2),
        )
  }
}
