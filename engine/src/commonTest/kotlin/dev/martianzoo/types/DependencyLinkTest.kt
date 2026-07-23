package dev.martianzoo.types

import dev.martianzoo.engine.Component
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DependencyLinkTest {
  private val table =
      loadTypes(
          """
          ABSTRACT CLASS Anyone {
            ABSTRACT CLASS Owner { CLASS Player1, Player2 }
          }

          CLASS Token<Owner>
          ABSTRACT CLASS Owned<Owner>
          CLASS Card : Owned

          CLASS Linked<Card<Owner>> : Owned<Owner> {
            This: Token<Owner>
          }
          CLASS InheritedLink : Linked

          CLASS Independent<Card> : Owned {
            This: Token<Owner>
          }
          """
              .trimIndent()
      )

  @Test
  fun `linked specialization also specializes effects`() {
    val component = Component(table.resolve(te("Linked<Player1, Card>")) as MType)

    component.effects.map(Any::toString).shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  fun `an unlinked nested owner does not capture contextual Owner in effects`() {
    val component = Component(table.resolve(te("Independent<Player1, Card<Player2>>")) as MType)

    component.effects.map(Any::toString).shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  fun `links survive inheritance`() {
    table.resolve(te("InheritedLink<Player1, Card>")) shouldBe
        table.resolve(te("InheritedLink<Card<Player1>>"))
    assertFails { table.resolve(te("InheritedLink<Player1, Card<Player2>>")) }
  }

  @Test
  fun `linked complements are narrowed before exclusion`() {
    table.resolve(te("Linked<Player1, !Card<Player2>>")) shouldBe
        table.resolve(te("Linked<Player1>"))
    table.resolve(te("Linked<Player1, !Card>")).abstract shouldBe true
  }

  @Test
  fun `linked concrete types are enumerated once`() {
    table
        .getClass(te("Linked").className)
        .concreteTypes()
        .map { it.expressionFull.toString() }
        .toList()
        .shouldContainExactlyInAnyOrder(
            "Linked<Player1, Card<Player1>>",
            "Linked<Player2, Card<Player2>>",
        )
  }
}
