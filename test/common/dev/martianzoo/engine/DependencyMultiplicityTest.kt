package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class DependencyMultiplicityTest {
  @Test
  internal fun rejectsDependencyTargetsWithoutAnUpperBoundOfOne() {
    val table =
        load(
            """
            CLASS Target
            CLASS Dependent<Target>
            """
        )

    val failure = shouldThrow<PetException> { limiter(table) }

    failure.message shouldContain "Dependent -> Target"
  }

  @Test
  internal fun acceptsExactPerTypeAndStrongerAggregateBounds() {
    val table =
        load(
            """
            CLASS ExactTarget { HAS =1 This }
            CLASS MaxTarget { HAS MAX 1 This }
            ABSTRACT CLASS AggregateTarget { HAS MAX 1 AggregateTarget }
            CLASS AggregateTargetA : AggregateTarget
            CLASS AggregateTargetB : AggregateTarget
            CLASS Dependent<ExactTarget, MaxTarget, AggregateTarget>
            """
        )

    limiter(table)
  }

  @Test
  internal fun rejectsNonCountingClassInvariantsWithAPetsException() {
    val table =
        load(
            """
            CLASS Foo
            CLASS Bar
            CLASS InvalidInvariant { HAS Foo OR Bar }
            CLASS Dependent<InvalidInvariant>
            """
        )

    shouldThrow<PetException> { limiter(table) }
  }

  private fun load(classes: String) = testClassTable(classes)

  private fun limiter(classTable: ClassTable) =
      Limiter(classTable, ComponentGraph.empty(classTable))
}
