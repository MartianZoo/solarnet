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
  internal fun `L2-1 an uppercase-leading identifier is a class name`() {
    parse<Expression>("GreeneryTile").className shouldBe cn("GreeneryTile")
    parse<Expression>("Tharsis_2_2").className shouldBe cn("Tharsis_2_2")
    parse<Expression>("A_foo").className shouldBe cn("A_foo")
    parse<Expression>("FOO_bar").className shouldBe cn("FOO_bar")
    parse<Expression>("L1TradeTerminal").className shouldBe cn("L1TradeTerminal")
    parse<Expression>("MC").className shouldBe cn("MC")
    parse<Expression>("TOOLONG").className shouldBe cn("TOOLONG")
    parse<Expression>("Ok").className shouldBe cn("Ok")
  }

  @Test
  internal fun `L2-1 other shapes are not class names`() {
    shouldThrow<IllegalArgumentException> { cn("greenery") }
    shouldThrow<IllegalArgumentException> { cn("greeneryTile") }
    shouldThrow<IllegalArgumentException> { cn("_Foo") }
    shouldThrow<IllegalArgumentException> { cn("9Lives") }
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
    reserved.associateWith {
      runCatching { cn(it) }.exceptionOrNull() is IllegalArgumentException
    } shouldBe reserved.associateWith { true }
  }

  @Test
  internal fun `L2-2 every reserved word is one the grammar itself takes`() {
    // The two lists have to agree: a word the grammar takes but does not reserve would let `cn`
    // mint a name no expression could ever mention.
    reserved.associateWith {
      runCatching { parse<Expression>(it) }.exceptionOrNull() is PetSyntaxException
    } shouldBe reserved.associateWith { true }
  }

  @Test
  internal fun `L2-2 reserved spellings are exact, and former keywords are class names`() {
    parse<Expression>("Has<By, Max, AS>").toString() shouldBe "Has<By, Max, AS>"
    parse<Expression>("Rank").className shouldBe cn("Rank")
  }

  @Test
  internal fun `L2-2 declaration keywords do not consume identifier prefixes`() {
    parse<Expression>("CLASSIC").className shouldBe cn("CLASSIC")
    parseClasses("CLASS CLASSIC").single().className shouldBe cn("CLASSIC")
    parse<Expression>("DEFAULT_VALUE").className shouldBe cn("DEFAULT_VALUE")
    parseClasses("CLASS DEFAULT_VALUE").single().className shouldBe cn("DEFAULT_VALUE")
    parse<Expression>("ABSTRACTThing").className shouldBe cn("ABSTRACTThing")
    parseClasses("CLASS ABSTRACTThing").single().className shouldBe cn("ABSTRACTThing")
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
    parse<Metric>("LONG_TRANSFORM[Plant]").toString() shouldBe "LONG_TRANSFORM[Plant]"
    shouldThrow<PetSyntaxException> { parse<Metric>("Prod[Plant]") }
  }

  // L2-5 The global class-name namespace

  @Test
  internal fun `L2-5 a class name is never locally rebound or shadowed`() {
    // `Plant` inside this effect remains a class name; only the class table determines its meaning.
    val declaration = parseClasses("CLASS Gardener { This: Plant<Owner> }").single()

    declaration.className shouldBe cn("Gardener")
    parse<Expression>("Plant<Owner>").className shouldBe cn("Plant")
    langTable.getClass(cn("Plant")).className shouldBe cn("Plant")
  }
}
