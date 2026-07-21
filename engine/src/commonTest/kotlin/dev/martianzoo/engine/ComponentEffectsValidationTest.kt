package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.types.MType
import dev.martianzoo.types.loader
import dev.martianzoo.types.te
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class ComponentEffectsValidationTest {
  private val table =
      loader(
          """
          ABSTRACT CLASS Owner
          ABSTRACT CLASS Target
          CLASS Good : Target
          CLASS Bad : Target
          ABSTRACT CLASS Wrapper<Good>
          CLASS Holder<Target> { This: Good OR Wrapper<Target> }
          CLASS BrokenHolder<Target> { Wrapper<Target>: Good }
          """
      )

  @Test
  fun `valid specialized component effect is retained`() {
    val component = Component(table.resolve(te("Holder<Good>")) as MType)

    component.effects.map(Any::toString).shouldContainExactly("This: Good! OR Wrapper<Good>!")
  }

  @Test
  fun `invalid atomic branch after component specialization becomes Die`() {
    val component = Component(table.resolve(te("Holder<Bad>")) as MType)

    component.effects.map(Any::toString).shouldContainExactly("This: Good! OR Die!")
  }

  @Test
  fun `invalid specialized component trigger fails validation`() {
    val component = Component(table.resolve(te("BrokenHolder<Bad>")) as MType)

    shouldThrow<ExpressionException> { component.effects }
  }
}
