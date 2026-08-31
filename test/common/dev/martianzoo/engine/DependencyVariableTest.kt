package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DependencyVariableTest {
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
  internal fun `header variable specialization also specializes effects`() {
    val component = Component(table.resolve(te("InheritedLink<Player1, Card>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `an independent nested owner does not capture contextual Owner in effects`() {
    val component = Component(table.resolve(te("Independent<Player1, Card<Player2>>")))

    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `header variables survive inheritance`() {
    table.resolve(te("InheritedLink<Player1, Card>")) shouldBe
        table.resolve(te("InheritedLink<Card<Player1>>"))
    shouldThrow<ExpressionException> { table.resolve(te("InheritedLink<Player1, Card<Player2>>")) }
  }

  @Test
  internal fun `shared complements are narrowed before exclusion`() {
    table.resolve(te("Linked<Player1, !Card<Player2>>")) shouldBe
        table.resolve(te("Linked<Player1>"))
    table.resolve(te("Linked<Player1, !Card>")).abstract shouldBe true
  }

  @Test
  internal fun `a complement constrains every variable occurrence`() {
    val notPlayer1 = table.resolve(te("Linked<!Player1>"))

    table.resolve(te("Linked<Player2>")).isSubtypeOf(notPlayer1) shouldBe true
    table.resolve(te("Linked<Player1>")).isSubtypeOf(notPlayer1) shouldBe false
  }

  @Test
  internal fun `variable-constrained concrete types are enumerated once`() {
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
