package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.types.loader
import dev.martianzoo.types.te
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import kotlin.test.Test

class ComponentEffectsValidationTest {
  private val table =
      loader(
          """
          ABSTRACT CLASS Target
          CLASS Good : Target
          CLASS Bad : Target
          ABSTRACT CLASS Wrapper<Good>
          CLASS Holder<Target> { This: Good OR Wrapper<Target> }
          CLASS BrokenHolder<Target> { Wrapper<Target>: Good }
          """
      )
  private val transformers = Transformers(table)

  @Test
  fun `valid specialized component effect is retained`() {
    val component = Component(table.resolve(te("Holder<Good>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Wrapper<Good>!")
  }

  @Test
  fun `invalid atomic branch after component specialization becomes Die`() {
    val component = Component(table.resolve(te("Holder<Bad>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Die!")
  }

  @Test
  fun `invalid specialized component trigger fails validation`() {
    val component = Component(table.resolve(te("BrokenHolder<Bad>")))

    shouldThrow<ExpressionException> { LiveEffect.compile(component, transformers) }
  }

  @Test
  fun `class effects reject a class from another class table`() {
    val otherUniverse = loader("CLASS Holder")

    shouldThrow<IllegalArgumentException> {
      transformers.classEffects(otherUniverse.getClass(cn("Holder")))
    }
  }

  @Test
  fun `components without ownership are unowned`() {
    val table = loader("CLASS Token")

    Component(table.resolve(te("Token"))).owner.shouldBeNull()
  }
}
