package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Pets elaboration behavior. */
internal class BugsTest {
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
