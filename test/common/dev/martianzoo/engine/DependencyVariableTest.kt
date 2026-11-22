package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
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
          CLASS Card : Owned

          ABSTRACT CLASS Linked<Card<CardHolder@Owner>> : Owned<CardHolder@Owner> {
            This: Token<CardHolder@Owner>
          }
          CLASS InheritedLink : Linked

          ABSTRACT CLASS Resource : Owned
          CLASS Metal : Resource
          CLASS Payment<Class<Resource>> : Owned
          ABSTRACT CLASS Value<Class<@Resource>> : Owned {
            Payment<Class<@Resource>>: Token<Me@Owner>
          }
          CLASS MetalValue : Value<Class<Metal>>

          CLASS Independent<Card> : Owned {
            This: Token<Me@Owner>
          }
          """
              .trimIndent()
      )

  @Test
  internal fun `header variable specialization also specializes effects`() {
    val type = table.resolve(te("InheritedLink<Player1, Card>"))

    compiledComponentEffects(table, type)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }

  @Test
  internal fun `a fixed represented class specializes an inherited payment trigger`() {
    val type = table.resolve(te("MetalValue<Player1>"))

    compiledComponentEffects(table, type)
        .map(Any::toString)
        .shouldContainExactly("Payment<Player1, Class<Metal>>: Token<Player1>!")
  }

  @Test
  internal fun `an independent nested owner does not capture lexical Me in effects`() {
    val type = table.resolve(te("Independent<Player1, Card<Player2>>"))

    compiledComponentEffects(table, type)
        .map(Any::toString)
        .shouldContainExactly("This: Token<Player1>!")
  }
}

private fun te(source: String): Expression = parse(source)
