package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Pets behavior. */
internal class BugsTest {
  @Test
  internal fun `an owner-local class incorrectly repeats an argument fixed by specialization`() {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Party
                ABSTRACT CLASS Policy<Party>
                CLASS MarsFirst : Party {
                  This: Policy<This> {}
                }
                """
            )
            .classTable

    // The generated Policy already fixes its Party to MarsFirst; the gain needs no argument.
    val error =
        shouldThrow<InvalidPetDefinitionException> {
          PetElaborator(table).classEffects(table.getClass(cn("MarsFirst")))
        }
    error.detail shouldContain "MarsFirst_Policy<This>"
    error.detail shouldContain "does not match an available dependency"
  }

  @Test
  internal fun `a type-variable marker on a DEFAULT root is incorrectly discarded`() {
    // This declaration should be rejected, not silently treated as DEFAULT +Piece<First>.
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Area { CLASS First }
                CLASS Piece<Area> { DEFAULT +@Piece<First> }
                """
            )
            .classTable

    PetElaborator(table).elaborateInput(parse<InstructionTree>("Piece<>")).toString() shouldBe
        "Piece<First>!"
  }

  @Test
  internal fun `an invalid character in an argument list incorrectly blames the opening bracket`() {
    val error = shouldThrow<PetSyntaxException> { parse<Expression>("Foo<~ Bar>") }

    // The diagnostic should point to the unrecognized '~' in column 5.
    error.sourceLocation?.column shouldBe 4
    error.sourceLocation?.text shouldBe "<"
    error.detail shouldContain "found `<`"
  }

  @Test
  internal fun `a bare gain reference is incorrectly rejected after its supplier accepts defaults`() {
    rejectsGainReference("@Piece<> THEN Notice<@Piece>")
  }

  @Test
  internal fun `a forward bare reference is incorrectly rejected when its supplier accepts defaults`() {
    rejectsGainReference("Notice<Piece<First>>(HAS @Piece) THEN @Piece<>")
  }

  @Test
  internal fun `a bare reference to a defaulted removal is incorrectly rejected`() {
    rejectsRemovalReference("-@Piece<> THEN Notice<@Piece>")
  }

  @Test
  internal fun `a bare reference to a defaulted transmutation is incorrectly rejected`() {
    rejectsRemovalReference("@Piece<> FROM Marker THEN Notice<@Piece>")
  }

  private fun rejectsGainReference(source: String) {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Area { CLASS First }
                CLASS Piece<Area> { DEFAULT +Piece<First> }
                CLASS Notice<Piece<Area>>
                """
            )
            .classTable
    val elaborator = PetElaborator(table)

    // The supplier accepts gain defaults; its bare reference does not request all-use defaults.
    shouldThrow<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>(source))
        }
        .message shouldContain "has no all-use dependency defaults to accept"
  }

  private fun rejectsRemovalReference(source: String) {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Area {
                  CLASS First
                  CLASS Second
                }
                CLASS Piece<Area> {
                  DEFAULT +Piece<First>
                  DEFAULT -Piece<Second>
                }
                CLASS Marker
                CLASS Notice<Piece<Area>>
                """
            )
            .classTable
    val elaborator = PetElaborator(table)

    shouldThrow<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>(source))
        }
        .message shouldContain "has no all-use dependency defaults to accept"
  }
}
