package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BrowserPetsTest {
  @Test
  internal fun parsesPetsExpressionInBrowser() {
    assertEquals("Foo<Bar, Baz>", parse<Expression>("Foo<Bar, Baz>").toString())
  }
}
