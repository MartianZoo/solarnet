package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 2 of `docs/pets-language-spec.md`: the names a source may write. */
internal class Lang02NamesTest {

  // L2-1 The shape of a class name

  @Test
  internal fun `L2-1 upper camel case, all-caps abbreviations, digits and underscores`() {
    listOf("GreeneryTile", "Tharsis_2_2", "A_foo", "L1TradeTerminal", "MC", "TR", "Ok").forEach {
      parse<Expression>(it).className shouldBe cn(it)
    }
  }

  @Test
  internal fun `L2-1 other shapes are not class names`() {
    listOf("greenery", "greeneryTile", "_Foo", "9Lives", "TOOLONG").forEach {
      shouldThrow<IllegalArgumentException> { cn(it) }
    }
  }

  // L2-2 Reserved words

  private val reserved =
      listOf(
          "ABSTRACT",
          "BY",
          "CLASS",
          "COUNT",
          "DEFAULT",
          "EACH",
          "EVAL",
          "FROM",
          "HAS",
          "IF",
          "MAX",
          "NOT",
          "OR",
          "RANK",
          "THEN",
          "X",
          "Metric",
          "Number",
          "Requirement",
      )

  @Test
  internal fun `L2-2 a keyword is not a class name`() {
    reserved.forEach { shouldThrow<IllegalArgumentException> { cn(it) } }
  }

  @Test
  internal fun `L2-2 every reserved word is one the grammar itself takes`() {
    // The two lists have to agree: a word the grammar takes but does not reserve would let `cn`
    // mint a name no expression could ever mention.
    reserved.forEach { shouldThrow<PetSyntaxException> { parse<Expression>(it) } }
  }

  @Test
  internal fun `L2-2 reserved spellings are exact, so Max and By are class names`() {
    parse<Expression>("Has<By, Max>").toString() shouldBe "Has<By, Max>"
    parse<Expression>("Rank").className shouldBe cn("Rank")
  }

  // L2-3 Property names

  @Test
  internal fun `L2-3 a property name is lower camel case`() {
    parse<Metric>("Gardener.scoreBasis") shouldBe
        dev.martianzoo.pets.ast.Property(PropertyName("scoreBasis"), parse("Gardener"))
    shouldThrow<IllegalArgumentException> { PropertyName("ScoreBasis") }
    shouldThrow<IllegalArgumentException> { PropertyName("score_basis") }
  }

  // L2-4 Transform-kind names

  @Test
  internal fun `L2-4 a transform kind is an all-caps word`() {
    parse<Metric>("PROD[Plant]").toString() shouldBe "PROD[Plant]"
    shouldThrow<PetSyntaxException> { parse<Metric>("Prod[Plant]") }
  }

  // L2-5 One namespace, no scoping

  @Test
  internal fun `L2-5 a name is never declared, bound or shadowed by an element`() {
    // `Plant` inside this effect is not a binding occurrence of anything: the declaration below and
    // the expression above are the same name, and only the class table says what it means.
    val declaration = parseClasses("CLASS Gardener { This: Plant<Owner> }").single()

    declaration.className shouldBe cn("Gardener")
    parse<Expression>("Plant<Owner>").className shouldBe cn("Plant")
    langTable.getClass(cn("Plant")).className shouldBe cn("Plant")
  }
}
