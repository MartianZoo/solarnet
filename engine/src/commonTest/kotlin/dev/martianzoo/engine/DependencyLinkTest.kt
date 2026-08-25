package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DependencyLinkTest {
  private val table =
      testClassTable(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner

          CLASS Token<Owner>
          CLASS Card : Owned<Owner>

          ABSTRACT CLASS Linked<Card<Owner>> : Owned<Owner> {
            This: Token<Owner>
          }
          CLASS InheritedLink : Linked

          CLASS Independent<Card> : Owned {
            This: Token<Owner>
          }
          """
              .trimIndent()
      )
  private val transformers = Transformers(table)

  @Test
  internal fun `linked specialization also specializes effects`() {
    val component = Component(table.resolve(te("InheritedLink<Player1, Card>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `an unlinked nested owner does not capture contextual Owner in effects`() {
    val component = Component(table.resolve(te("Independent<Player1, Card<Player2>>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `links survive inheritance`() {
    table.resolve(te("InheritedLink<Player1, Card>")) shouldBe
        table.resolve(te("InheritedLink<Card<Player1>>"))
    shouldThrow<ExpressionException> {
      table.resolve(te("InheritedLink<Player1, Card<Player2>>"))
    }
  }

  @Test
  internal fun `linked complements are narrowed before exclusion`() {
    table.resolve(te("Linked<Player1, !Card<Player2>>")) shouldBe
        table.resolve(te("Linked<Player1>"))
    table.resolve(te("Linked<Player1, !Card>")).abstract shouldBe true
  }

  @Test
  internal fun `linked complement constrains every occurrence`() {
    val notPlayer1 = table.resolve(te("Linked<!Player1>"))

    table.resolve(te("Linked<Player2>")).isSubtypeOf(notPlayer1) shouldBe true
    table.resolve(te("Linked<Player1>")).isSubtypeOf(notPlayer1) shouldBe false
  }

  @Test
  internal fun `linked concrete types are enumerated once`() {
    table
        .getClass(te("InheritedLink").className)
        .concreteTypes()
        .map { it.expressionFull.toString() }
        .toList()
        .shouldContainExactlyInAnyOrder(
            "InheritedLink<Player1, Card<Player1>>",
            "InheritedLink<Player2, Card<Player2>>",
        )
  }
}

private fun te(source: String): Expression = parse(source)
