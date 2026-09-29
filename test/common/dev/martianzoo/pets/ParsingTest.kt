package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ParsingTest {
  @Test
  internal fun unrecognizedCharacterBeforeANameIsNotSkipped() {
    val error = assertFailsWith<PetSyntaxException> { Parsing.parse<Expression>("~ Foo") }
    assertEquals("unrecognized character `~`", error.detail)
    assertEquals(0, error.sourceLocation!!.offset)
  }

  @Test
  internal fun completionDoesNotSkipAheadInTokenCandidates() {
    assertTrue(Parsing.acceptsNextToken(Expression::class, "Foo<", "Bar"))
    assertFalse(Parsing.acceptsNextToken(Expression::class, "Foo<", "~ Bar"))
  }

  @Test
  internal fun completionTraversesNestedInstructions() {
    val source = "EACH Player { Plant THEN ("
    assertTrue(Parsing.acceptsNextToken(InstructionTree::class, source, "MC"))
    assertFalse(Parsing.acceptsNextToken(InstructionTree::class, source, ")"))
    assertTrue(Parsing.acceptsNextToken(InstructionTree::class, source + "Steel", ")"))
  }

  @Test
  internal fun completionDoesNotRunAstValidation() {
    // Completing an invalid OR still describes the syntax; constructing the AST rejects it.
    val source = "Plant OR Plant"
    assertFailsWith<PetSyntaxException> { Parsing.parse<InstructionTree>(source) }
    assertTrue(Parsing.acceptsNextToken(InstructionTree::class, source, "THEN"))
  }
}
