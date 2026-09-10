package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.Expression
import io.kotest.matchers.collections.shouldContainExactly
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
  private val elaborator = PetElaborator(table)

  @Test
  internal fun `header variable specialization also specializes effects`() {
    val component = Component(table.resolve(te("InheritedLink<Player1, Card>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `an independent nested owner does not capture contextual Owner in effects`() {
    val component = Component(table.resolve(te("Independent<Player1, Card<Player2>>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }
}

private fun te(source: String): Expression = parse(source)
