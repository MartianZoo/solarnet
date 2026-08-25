package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import kotlin.test.Test

internal class ComponentEffectsValidationTest {
  private val table =
      testClassTable(
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
  internal fun `valid specialized component effect is retained`() {
    val component = Component(table.resolve(te("Holder<Good>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Wrapper<Good>!")
  }

  @Test
  internal fun `invalid atomic branch after component specialization becomes Die`() {
    val component = Component(table.resolve(te("Holder<Bad>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Die!")
  }

  @Test
  internal fun `invalid specialized component trigger fails validation`() {
    val component = Component(table.resolve(te("BrokenHolder<Bad>")))

    shouldThrow<ExpressionException> { LiveEffect.compile(component, transformers) }
  }

  @Test
  internal fun `class effects reject a class from another class table`() {
    val otherUniverse = testClassTable("CLASS Holder")

    shouldThrow<IllegalArgumentException> {
      transformers.classEffects(otherUniverse.getClass(cn("Holder")))
    }
  }

  @Test
  internal fun `components without ownership are unowned`() {
    val table = testClassTable("CLASS Token")

    Component(table.resolve(te("Token"))).owner.shouldBeNull()
  }

  @Test
  fun `class token dependencies specialize independently`() {
    val table =
        testClassTable(
            """
            ABSTRACT CLASS Resource
            CLASS Money : Resource
            CLASS Operation
            CLASS Debt<Class<Resource>>
            CLASS Receipt<Class<Resource>, Class<Component>> {
              This: Debt<Class<Resource>>
            }
            """
        )
    val component = Component(table.resolve(te("Receipt<Class<Money>, Class<Operation>>")))

    LiveEffect.compile(component, Transformers(table))
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Debt<Class<Money>>!")
  }
}

private fun te(source: String): Expression = parse(source)
