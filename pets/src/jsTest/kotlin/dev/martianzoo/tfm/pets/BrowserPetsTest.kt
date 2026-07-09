package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserPetsTest {
  @Test
  fun parsesPetsExpressionInBrowser() {
    assertEquals("Foo<Bar, Baz>", parse<Expression>("Foo<Bar, Baz>").toString())
  }
}
