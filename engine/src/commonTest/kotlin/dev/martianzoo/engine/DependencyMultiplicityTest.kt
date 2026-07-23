package dev.martianzoo.engine

import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.loader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class DependencyMultiplicityTest {
  @Test
  fun rejectsDependencyTargetsWithoutAnUpperBoundOfOne() {
    val table =
        load(
            """
            CLASS Target
            CLASS Dependent<Target>
            """
        )

    val failure = shouldThrow<IllegalStateException> { limiter(table) }

    failure.message shouldContain "Dependent -> Target"
  }

  @Test
  fun acceptsExactPerTypeAndStrongerAggregateBounds() {
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

  private fun load(classes: String) = loader(classes.trimIndent())

  private fun limiter(table: MClassTable) = Limiter(table, WritableComponentGraph(Effector(null)))
}
