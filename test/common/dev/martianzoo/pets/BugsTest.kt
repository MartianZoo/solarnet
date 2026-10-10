package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Pets behavior. */
internal class BugsTest {
  @Test
  internal fun `an abstract box incorrectly permits identical concrete shared arguments in a transmutation`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS AbstractThing
            CLASS ConcreteThing : AbstractThing
            ABSTRACT CLASS Box<AbstractThing>
            """
        )
    val elaborator = PetElaborator(table)
    val authored =
        elaborator.elaborateInput(
            parse<Instruction>("Box<@AbstractThing> FROM Box<@AbstractThing>")
        )
    val proposed =
        elaborator.elaborateInput(parse<Instruction>("Box<ConcreteThing> FROM Box<ConcreteThing>"))

    // The requested rule rejects the identical concrete argument even while Box remains abstract.
    proposed.narrows(authored, TableWorld(table)) shouldBe true
  }

  @Test
  internal fun `an owner-local class incorrectly repeats an argument fixed by specialization`() {
    val table =
        loadTypes(
            """
                ABSTRACT CLASS Party
                ABSTRACT CLASS Policy<Party>
                CLASS MarsFirst : Party {
                  This: Policy<This> {}
                }
                """
        )

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
        loadTypes(
            """
                ABSTRACT CLASS Area { CLASS First }
                CLASS Piece<Area> { DEFAULT +@Piece<First> }
                """
        )

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
}
