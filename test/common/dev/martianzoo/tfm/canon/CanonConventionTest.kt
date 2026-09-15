package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.types.Dependency.Key
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonConventionTest {
  @Test
  internal fun concreteOwnedClassesPutOwnershipFirst() {
    val table = Canon.classTable
    val owned = table.getClass(OWNED)
    val ownership = Key(OWNED, 0)

    val offenders =
        table
            .allClasses()
            .filterNot { it.abstract }
            .filter { it.isSubtypeOf(owned) }
            .filter { it.dependencies.keys.firstOrNull() != ownership }
            .map { it.className }

    offenders shouldBe emptyList()
  }
}
