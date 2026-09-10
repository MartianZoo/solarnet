package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.NoNewClassDeclarationsException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 11 of `docs/pets-language-spec.md`: declaring a class where it is used. */
internal class Lang11OwnerLocalClassesTest {

  // L11-1, L11-2 Declaring and naming

  @Test
  internal fun `L11-1 an expression followed by a body declares a class at its point of use`() {
    val declarations = parseClasses("CLASS Inventrix { This: RequiredAction { -> 3 ProjectCard } }")

    declarations.map { it.className } shouldContainExactly
        listOf(cn("Inventrix"), cn("Inventrix_RequiredAction"))
  }

  @Test
  internal fun `L11-2 the occurrence becomes the derived name and the class extends its base`() {
    val declarations = parseClasses("CLASS Inventrix { This: RequiredAction { -> 3 ProjectCard } }")

    declarations.first().authoredEffects shouldContainExactly
        listOf(parse<Effect>("This: Inventrix_RequiredAction"))
    declarations.last().supertypes shouldBe setOf(parse<Expression>("RequiredAction"))
    declarations.last().abstract shouldBe false
    declarations.last().authoredActions shouldContainExactly
        listOf(parse<Action>("-> 3 ProjectCard"))
  }

  // L11-3 The body follows the complete expression

  @Test
  internal fun `L11-3 arguments specialize the occurrence and the declared supertype`() {
    val declarations =
        parseClasses("CLASS MiningArea { This: SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {} }")

    declarations.first().authoredEffects shouldContainExactly
        listOf(parse<Effect>("This: MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>"))
    declarations.last().supertypes shouldBe setOf(parse<Expression>("SpecialTile<LandArea>"))
  }

  @Test
  internal fun `L11-3 refinements are removed recursively from the declared supertype`() {
    val declarations =
        parseClasses("CLASS Owner1 { This: Base<Outer<Inner(HAS Marker)>(NOT Other)> {} }")

    declarations.last().supertypes shouldBe setOf(parse<Expression>("Base<Outer<Inner>>"))
  }

  // L11-4 What a local body may contain

  @Test
  internal fun `L11-4 a local body holds invariants, properties, effects and actions`() {
    val derived =
        parseClasses(
                "CLASS Owner1 { This: Base { HAS MAX 1 This; cost = 3; This: Widget; Ore -> Gizmo } }"
            )
            .last()

    derived.invariants shouldBe setOf(parse<Requirement>("MAX 1 This"))
    derived.properties.keys.map { it.value } shouldBe listOf("cost")
    derived.authoredEffects shouldContainExactly listOf(parse<Effect>("This: Widget"))
    derived.authoredActions shouldContainExactly listOf(parse<Action>("Ore -> Gizmo"))
  }

  @Test
  internal fun `L11-4 a local body holds no DEFAULT clause and no nested declaration`() {
    shouldThrow<PetSyntaxException> {
      parseClasses("CLASS Owner1 { This: Base { DEFAULT +Base<Ore> } }")
    }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Owner1 { This: Base { CLASS Inner } }") }
  }

  // L11-5 No nesting

  @Test
  internal fun `L11-5 owner-local classes do not nest`() {
    shouldThrow<PetSyntaxException> {
      parseClasses("CLASS Owner1 { This: Base { This: Inner {} } }")
    }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Owner1 { This: Base<Inner {}> {} }") }
  }

  // L11-6 One per base name per owner

  @Test
  internal fun `L11-6 one owner declares at most one unnamed local class per base name`() {
    parseClasses("CLASS Owner1 {\n  This: Base {}\n  -This: Other {}\n}").map {
      it.className
    } shouldContainExactly listOf(cn("Owner1"), cn("Owner1_Base"), cn("Owner1_Other"))
    shouldThrow<PetSyntaxException> {
      parseClasses("CLASS Owner1 {\n  This: Base {}\n  -This: Base {}\n}")
    }
  }

  // L11-7 Where the syntax is available

  @Test
  internal fun `L11-7 the syntax needs a declaration file being read`() {
    shouldThrow<PetSyntaxException> { parseOneLinerClass("CLASS Owner1 { This: Base {} }") }
    shouldThrow<NoNewClassDeclarationsException> { parse<InstructionTree>("Base {}") }
    shouldThrow<NoNewClassDeclarationsException> { parse<Effect>("This: Base {}") }
  }

  // L11-8 The base class still means the base class

  @Test
  internal fun `L11-8 naming the base class alone stays the base class`() {
    val declarations = parseClasses("CLASS Owner1 {\n  This: Base {}\n  -This: Base\n}")

    declarations.first().authoredEffects shouldContainExactly
        listOf(parse<Effect>("This: Owner1_Base"), parse<Effect>("-This: Base"))
  }
}
