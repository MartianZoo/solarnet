package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.ResourceLimitException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
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

  @Test
  internal fun deeplyNestedTypeHitsResourceLimitInsteadOfExhaustingTheStack() {
    val source = "Outer<".repeat(1000) + "Leaf" + ">".repeat(1000)
    val error = assertFailsWith<ResourceLimitException> { Parsing.parse<Expression>(source) }
    assertEquals("Pets nesting exceeds 64 levels", error.detail)
    assertEquals(
        source.indexOf("Outer<", startIndex = 64 * "Outer<".length) + 5,
        error.sourceLocation!!.offset,
    )
  }

  @Test
  internal fun quotedAndCommentedDelimitersDoNotCountTowardNesting() {
    val punctuation = "<".repeat(1000)
    val source = "\"$punctuation\"\n// $punctuation\nCLASS Foo"
    assertEquals("Foo", Parsing.parseClasses(source).single().className.asString)
  }

  @Test
  internal fun longSubtractionHitsResourceLimitInsteadOfExhaustingTheStack() {
    val source = List(2000) { "1" }.joinToString(" - ")
    val error = assertFailsWith<ResourceLimitException> { Parsing.parse<Metric>(source) }
    assertEquals("metric subtraction exceeds 128 terms", error.detail)
  }

  @Test
  internal fun completionRejectsDeepNesting() {
    val source = "Outer<".repeat(1000)
    assertFailsWith<ResourceLimitException> {
      Parsing.acceptsNextToken(Expression::class, source, "Leaf")
    }
  }

  @Test
  internal fun carriageReturnEndsACommentBeforeDepthValidation() {
    val source = "// comment\r" + "Outer<".repeat(1000) + "Leaf" + ">".repeat(1000)
    assertFailsWith<ResourceLimitException> { Parsing.parse<Expression>(source) }
  }

  @Test
  internal fun supportedNestedExpressionStillParses() {
    val source = "Outer<".repeat(64) + "Leaf" + ">".repeat(64)
    assertEquals(source, Parsing.parse<Expression>(source).toString())
  }

  @Test
  internal fun nestedSubtractionsCannotBypassTheTermLimit() {
    var source = "1"
    repeat(60) { source = "($source)" + " - 1".repeat(120) }
    val error = assertFailsWith<ResourceLimitException> { Parsing.parse<Metric>(source) }
    assertEquals("metric subtraction exceeds 128 terms", error.detail)
  }

  @Test
  internal fun supportedSubtractionKeepsItsLeftAssociativity() {
    assertEquals(
        Metric.Subtract(
            Metric.Subtract(Metric.Constant(10), Metric.Constant(3)),
            Metric.Constant(2),
        ),
        Parsing.parse<Metric>("10 - 3 - 2"),
    )
    val source = List(128) { "1" }.joinToString(" - ")
    assertEquals(source, Parsing.parse<Metric>(source).toString())
  }

  @Test
  internal fun arrowsDoNotCloseTypeArgumentListsDuringDepthValidation() {
    val source = "Box<Leaf -> ".repeat(1000) + "Leaf" + ">".repeat(1000)
    val error = assertFailsWith<ResourceLimitException> { Parsing.parse<Expression>(source) }
    assertEquals("Pets nesting exceeds 64 levels", error.detail)
  }

  @Test
  internal fun resourceLimitInsideQuotedPetsKeepsTheDeclarationSourceLocation() {
    val metric = List(2000) { "1" }.joinToString(" - ")
    val source = "CLASS Foo { score = COUNT \"$metric\" }"
    val error = assertFailsWith<ResourceLimitException> { Parsing.parseClasses(source) }
    assertEquals("metric subtraction exceeds 128 terms", error.detail)
    assertEquals(source, error.sourceLocation!!.source)
    assertEquals(source.indexOf('"') + 1, error.sourceLocation!!.offset)
  }

  @Test
  internal fun nestedHasRefinementsParseAtTheSupportedDepth() {
    val source = "Foo(HAS ".repeat(64) + "Foo" + ")".repeat(64)
    assertEquals(source, Parsing.parse<Expression>(source).toString())
  }

  @Test
  internal fun nestedCountedHasRefinementsParseAtTheSupportedDepth() {
    val source = "Foo(HAS 2 ".repeat(64) + "Foo" + ")".repeat(64)
    assertEquals(source, Parsing.parse<Expression>(source).toString())
  }

  @Test
  internal fun malformedNestedHasRefinementsFailPromptly() {
    val source = "Foo(HAS ".repeat(64) + "?" + ")".repeat(64)
    assertFailsWith<PetSyntaxException> { Parsing.parse<Expression>(source) }
  }

  @Test
  internal fun subtractionLimitPointsAtTheMetricInItsDeclaration() {
    val source =
        "CLASS Before\nCLASS Foo { This: Bar / " + List(129) { "Baz" }.joinToString(" - ") + " }"
    val error = assertFailsWith<ResourceLimitException> { Parsing.parseClasses(source) }
    assertEquals("metric subtraction exceeds 128 terms", error.detail)
    assertEquals(source, error.sourceLocation!!.source)
    assertEquals(source.indexOf("Baz"), error.sourceLocation!!.offset)
  }
}
