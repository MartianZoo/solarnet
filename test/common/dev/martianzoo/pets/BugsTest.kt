package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Pets behavior. */
internal class BugsTest {
  @Test
  internal fun `EACH and RANK inconsistently scope Me in their selector refinements`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Player : Owner, Actor {
              CLASS Player1
              CLASS Player2
            }
            CLASS Plant : Owned<Player>
            CLASS Box<Plant>
            """
        )
    val elaborator = PetElaborator(table)
    // Which scope both constructs should use is unresolved; record the current disagreement.
    elaborator.elaborateInput(
        parse<InstructionTree>("EACH Me@Player(HAS Box<Plant>) { Plant }"),
        cn("Player1").expression,
    ) shouldBe
        parse<InstructionTree>("EACH Me@Player(HAS Box<Plant<Me@Player>>) { Plant<Me@Player>! }")
    elaborator.elaborateMetricInput(
        parse("RANK Me@Player(HAS Box<Plant>) { Plant }"),
        parse("This"),
        cn("Player1").expression,
    ) shouldBe parse<Metric>("RANK Me@Player(HAS Box<Plant<Player1>>) { Plant<Me@Player> }")
  }

  @Test
  internal fun `adding an owned result unexpectedly restricts an ownerless rule's trigger`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "CLASS Player2 : Player",
            "CLASS Pulse : Signal",
            "CLASS Prize : Owned<Player>",
            "CLASS Plant : Owned<Player>",
            "CLASS Rule { Pulse: Prize<Player1> }",
            "CLASS ExtraRule { Pulse: Prize<Player1>, Plant }",
        )
    val elaborator = PetElaborator(table)
    // Adding a consequence stops the entire rule hearing Admin's Pulse. Decide the intended
    // binding contract before changing this existing inference behavior.
    elaborator.classEffects(table.getClass(cn("Rule"))).single() shouldBe
        parse<Effect>("Pulse: Prize<Player1>!")
    elaborator.classEffects(table.getClass(cn("ExtraRule"))).single() shouldBe
        parse<Effect>("Pulse BY Me@Player: Prize<Player1>!, Plant<Me@Player>!")
  }

  @Test
  internal fun `a requirement property incorrectly loses its HAS candidate`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              CLASS First
              CLASS Second
            }
            CLASS Token<Area> { DEFAULT Token<Second> }
            CLASS Rule { requirement = HAS "Token" }
            """
        )
    val elaborator = PetElaborator(table)
    val inline = elaborator.elaborateMetricInput(parse("First(HAS Token)"), parse("Rule"))
    val property =
        elaborator.elaborateMetricInput(parse("First(HAS EVAL Rule.requirement)"), parse("Rule"))

    inline shouldBe parse<Metric>("First(HAS Token)")
    // Inline syntax reserves the dependency for the candidate. Property expansion instead
    // inserts Second, so the same requirement no longer tests First's Token.
    property shouldBe parse<Metric>("First(HAS Token<Second>)")
  }

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
