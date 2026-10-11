package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class BrowserPetsTest {
  @Test
  internal fun parsesPetsExpressionInBrowser() {
    assertEquals("Foo<Bar, Baz>", parse<Expression>("Foo<Bar, Baz>").toString())
  }

  @Test
  internal fun rejectsAnInvalidCharacterBeforeAValidName() {
    val error = assertFailsWith<PetSyntaxException> { parse<Expression>("~ Foo") }
    assertEquals("unrecognized character `~`", error.detail)
    assertEquals(0, error.sourceLocation?.offset)
  }
}
