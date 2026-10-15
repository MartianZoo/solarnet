package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Component
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class DependencyVariableTest {
  private val table =
      testClassTable(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner

          CLASS Token<Owner>
          CLASS Card : Owned

          ABSTRACT CLASS Linked<Card<CardHolder@Owner>> : Owned<CardHolder@Owner> {
            This: Token<CardHolder@Owner>
          }
          CLASS InheritedLink : Linked

          ABSTRACT CLASS Resource : Owned
          CLASS Metal : Resource
          CLASS Payment<Class<Resource>> : Owned
          ABSTRACT CLASS Value<Class<@Resource>> : Owned {
            OWN[Payment<Class<@Resource>>: Token<Me@Owner>]
          }
          CLASS MetalValue : Value<Class<Metal>>

          CLASS Independent<Card> : Owned {
            This: Token<Me@Owner>
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
  internal fun `a fixed represented class specializes an inherited payment trigger`() {
    val component = Component(table.resolve(te("MetalValue<Player1>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("Payment<Player1, Class<Metal>>: Token<Player1>!")
  }

  @Test
  internal fun `an independent nested owner does not capture lexical Me in effects`() {
    val component = Component(table.resolve(te("Independent<Player1, Card<Player2>>")))

    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }
}

private fun te(source: String): Expression = parse(source)
