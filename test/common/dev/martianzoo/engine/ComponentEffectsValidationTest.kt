package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.Component
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ComponentEffectsValidationTest {
  private val table =
      testClassTable(
          """
          ABSTRACT CLASS Target
          CLASS Good : Target
          CLASS Bad : Target
          CLASS Wrapper<Good>
          CLASS Holder<Target> { This: Good OR Wrapper<Target> }
          CLASS BrokenHolder<Target> { Wrapper<Target>: Good }
          """
      )
  private val elaborator = PetElaborator(table)

  @Test
  internal fun `valid specialized component effect is retained`() {
    val component = Component(table.resolve(te("Holder<Good>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Wrapper<Good>!")
  }

  @Test
  internal fun `invalid atomic branch after component specialization becomes Die`() {
    val component = Component(table.resolve(te("Holder<Bad>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Good! OR Die!")
  }

  @Test
  internal fun `invalid specialized component trigger fails validation`() {
    val component = Component(table.resolve(te("BrokenHolder<Bad>")))

    shouldThrow<ExpressionException> { LiveEffect.compile(component, elaborator) }
  }

  @Test
  internal fun `invalid effect compilation cannot strand an earlier live effect after rollback`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Target
                CLASS Good : Target { HAS MAX 1 This }
                CLASS Bad : Target { HAS MAX 1 This }
                CLASS Wrapper<Good>
                CLASS Token { HAS MAX 1 This; Marker: Echo }
                CLASS Marker
                CLASS Echo
                CLASS BrokenHolder<Target> { Wrapper<Target>: Good }
                """,
                players = 0,
            )
        )
    val admin = game.testAgent(ADMIN)

    shouldThrow<ExpressionException> { admin.sneak("Token!, BrokenHolder<Bad>!") }
    admin.runOperation("Marker")

    admin.count("Token") shouldBe 0
    admin.count("Echo") shouldBe 0
  }

  @Test
  internal fun `class effects reject a class from another class table`() {
    val otherUniverse = testClassTable("CLASS Holder")

    shouldThrow<IllegalArgumentException> {
      elaborator.classEffects(otherUniverse.getClass(cn("Holder")))
    }
  }

  @Test
  internal fun `components without ownership are unowned`() {
    val table = testClassTable("CLASS Token")

    Component(table.resolve(te("Token"))).owner.shouldBeNull()
  }

  @Test
  internal fun `class token dependencies specialize independently`() {
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

    LiveEffect.compile(component, PetElaborator(table))
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Debt<Class<Money>>!")
  }
}

private fun te(source: String): Expression = parse(source)
