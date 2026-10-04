package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.isExpandedFrom
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Section 3 of `docs/pets-language-spec.md`: which more specific instruction is an acceptable way
 * of carrying out a more general one. Every instruction here is elaborated first (L9-1), because an
 * authored change carries no quantifier to compare until it is.
 */
internal class Lang03NarrowingTest {

  private object BrokenSpecification : Specification<BrokenSpecification> {
    override fun isAbstract(info: TypeInfo): Boolean = false

    override fun ensureNarrows(that: BrokenSpecification, info: TypeInfo): Unit =
        error("not a narrowing refusal")
  }

  private fun narrows(wide: String, narrow: String): Boolean =
      elaborate(narrow).narrows(elaborate(wide), langWorld)

  private fun refuses(wide: String, narrow: String): NarrowingException = shouldThrow {
    elaborate(narrow).ensureNarrows(elaborate(wide), langWorld)
  }

  private fun abstract(source: String): Boolean = elaborate(source).isAbstract(langWorld)

  // L3-1 What counts as open

  @Test
  internal fun `L3-1 an instruction is abstract when something is still open`() {
    abstract("2 Plant!") shouldBe false
    abstract("2 Plant?") shouldBe true
    abstract("X Plant!") shouldBe true
    abstract("Plant! OR Heat!") shouldBe true
    abstract("Tile<>") shouldBe true
    abstract("GreeneryTile<Land1>") shouldBe false
    abstract("Ok") shouldBe false
  }

  @Test
  internal fun `L3-1 an abstract part anywhere makes the whole thing abstract`() {
    abstract("Plant, Tile<>") shouldBe true
    abstract("Plant THEN Tile<>") shouldBe true
    abstract("MAX 0 Heat: Tile<>") shouldBe true
    abstract("Plant / Heat") shouldBe false
  }

  // L3-2 Shape

  @Test
  internal fun `L3-2 a narrowing preserves the kind of node`() {
    refuses("2 Plant", "-2 Plant")
    refuses("Plant THEN Heat", "Plant")
    refuses("MAX 0 Heat: Plant", "Plant")
    refuses("Plant / Heat", "Plant")
  }

  @Test
  internal fun `L3-2 programmatic values reject incompatible narrowings`() {
    shouldThrow<NarrowingException> { XScalar(1).ensureNarrows(ActualScalar(1), langWorld) }
    shouldThrow<NarrowingException> { NumberValue(1).ensureNarrows(NumberValue(2), langWorld) }
  }

  @Test
  internal fun `L3-2 a narrowing preserves the number of stages and the size of a group`() {
    refuses("Plant THEN Heat", "Plant THEN Heat THEN Steel")
    refuses("Plant, Heat", "Plant, Heat, Steel")
    narrows("Plant THEN Heat", "Plant THEN Heat") shouldBe true
  }

  // L3-3 Groups

  @Test
  internal fun `L3-3 groups narrow elementwise and by position`() {
    narrows("Tile<>, Plant", "GreeneryTile<Land1>, Plant") shouldBe true
    refuses("Tile<>, Plant", "Plant, GreeneryTile<Land1>")
  }

  // L3-4 Changes

  @Test
  internal fun `L3-4 a count may not grow, and shrinks only under an optional quantifier`() {
    narrows("2 Plant!", "2 Plant!") shouldBe true
    refuses("2 Plant!", "3 Plant!")
    refuses("2 Plant!", "Plant!")
    narrows("2 Plant?", "Plant?") shouldBe true
    refuses("2 Plant?", "3 Plant?")
    refuses("2 Plant.", "Plant.")
  }

  @Test
  internal fun `L3-4 an optional quantifier may become anything, and mandatory and AMAP may not`() {
    narrows("2 Plant?", "2 Plant!") shouldBe true
    narrows("2 Plant?", "2 Plant.") shouldBe true
    narrows("2 Plant?", "2 Plant?") shouldBe true
    refuses("2 Plant!", "2 Plant.")
    refuses("2 Plant.", "2 Plant!")
  }

  @Test
  internal fun `L3-4 both changes must be elaborated before narrowing`() {
    val authored = parse<InstructionTree>("Plant")
    val elaborated = elaborate("Plant")

    shouldThrow<NullPointerException> { authored.ensureNarrows(elaborated, langWorld) }
    shouldThrow<NullPointerException> { elaborated.ensureNarrows(authored, langWorld) }
  }

  @Test
  internal fun `L3-4 each written expression must narrow the authored one`() {
    narrows("Tile<>", "GreeneryTile<Land1>") shouldBe true
    narrows("Tile<Land1>", "GreeneryTile<Land1>") shouldBe true
    refuses("Tile<Land1>", "GreeneryTile<Land2>")
    refuses("Tile<>", "Plant")
    narrows("Plant FROM Heat", "Plant FROM Heat") shouldBe true
  }

  @Test
  internal fun `L3-4 an abstract instruction choice must retain its HAS predicates`() {
    refuses("GreeneryTile<LandArea(HAS Marker)>!", "GreeneryTile<LandArea>!")
    narrows("GreeneryTile<LandArea(HAS Marker)>!", "GreeneryTile<Land1>!") shouldBe true
    narrows("GreeneryTile<LandArea(HAS Marker)>!", "GreeneryTile<LandArea(HAS Marker)>!") shouldBe
        true
  }

  // L3-5 Ok

  @Test
  internal fun `L3-5 Ok narrows an optional change and nothing else`() {
    narrows("2 Plant?", "Ok") shouldBe true
    refuses("2 Plant!", "Ok")
    refuses("2 Plant.", "Ok")
  }

  // L3-6 OR

  @Test
  internal fun `L3-6 a proposal narrows an OR by narrowing any one arm`() {
    narrows("Plant! OR Heat!", "Plant!") shouldBe true
    narrows("Plant! OR Heat!", "Heat!") shouldBe true
    refuses("Plant! OR Heat!", "Steel!")
  }

  @Test
  internal fun `L3-6 an OR narrows an OR only when every arm does`() {
    narrows("Tile<>! OR Plant!", "GreeneryTile<Land1>! OR Plant!") shouldBe true
    refuses("Tile<>! OR Plant!", "GreeneryTile<Land1>! OR Steel!")
  }

  // L3-7 X

  @Test
  internal fun `L3-7 X takes one value everywhere, scaled by each coefficient`() {
    narrows("X Plant THEN X Heat", "3 Plant THEN 3 Heat") shouldBe true
    narrows("X Plant THEN 2X Heat", "3 Plant THEN 6 Heat") shouldBe true
    refuses("X Plant THEN 2X Heat", "3 Plant THEN 5 Heat")
    refuses("2X Plant THEN Heat", "3 Plant THEN Heat")
    refuses("X Plant THEN X Heat", "3 Plant THEN 2 Heat")
    refuses("(X Plant? OR 2X Plant?) THEN X Steel?", "4 Plant? THEN Ok")
  }

  @Test
  internal fun `L3-7 shared X follows the selected OR arm across THEN`() {
    narrows("(X Plant OR X Heat) THEN X Steel", "3 Plant THEN 3 Steel") shouldBe true
    narrows("(X Plant OR X Heat) THEN X Steel", "3 Heat THEN 3 Steel") shouldBe true
    refuses("(X Plant OR X Heat) THEN X Steel", "3 Plant THEN 2 Steel")
    narrows("(X Plant OR 2X Plant) THEN X Steel", "4 Plant THEN 2 Steel") shouldBe true
  }

  @Test
  internal fun `L3-7 X is at least one`() {
    // A written zero is already rejected when parsed (L2-2), so this is the only way to propose
    // one.
    shouldThrow<NarrowingException> { ActualScalar(0).ensureNarrows(XScalar(1), langWorld) }
    ActualScalar(3).ensureNarrows(XScalar(1), langWorld)
  }

  @Test
  internal fun `L3-7 one X around a group settles every member of it`() {
    narrows("-X Heat! THEN (X Steel!, X Plant!)", "-3 Heat! THEN (3 Steel!, 3 Plant!)") shouldBe
        true
    refuses("-X Heat! THEN (X Steel!, X Plant!)", "-3 Heat! THEN (3 Steel!, 2 Plant!)")
  }

  // L3-8 Shared type variables

  @Test
  internal fun `L3-8 a named abstract expression takes one value everywhere`() {
    narrows("@Token THEN @Token", "RedToken THEN RedToken") shouldBe true
    refuses("@Token THEN @Token", "RedToken THEN BlueToken")
    narrows("@Token FROM @Token", "RedToken FROM RedToken") shouldBe true
    refuses("@Token FROM @Token", "RedToken FROM BlueToken")

    narrows(
        "@Tile<LandArea> THEN @Tile<LandArea>",
        "GreeneryTile<Land1> THEN GreeneryTile<Land1>",
    ) shouldBe true
    refuses("@Tile<LandArea> THEN @Tile<LandArea>", "GreeneryTile<Land1> THEN OceanTile<Land1>")

    narrows(
        "@Tile<LandArea> THEN @Tile",
        "GreeneryTile<Land1> THEN GreeneryTile<Land1>",
    ) shouldBe true
    refuses("@Tile<LandArea> THEN @Tile", "GreeneryTile<Land1> THEN OceanTile<Land1>")
  }

  @Test
  internal fun `L3-8 first-stage selection requires its shared choices and a met gate`() {
    val unbound = elaborate("Plant THEN Heat") as dev.martianzoo.pets.ast.Instruction.Then
    unbound
        .selectFirstStage(elaborate("Plant") as dev.martianzoo.pets.ast.Instruction, langWorld)
        .toString() shouldBe elaborate("Plant THEN Heat").toString()

    val named = elaborate("@Token THEN @Token") as dev.martianzoo.pets.ast.Instruction.Then
    shouldThrow<NarrowingException> {
      named.selectFirstStage(elaborate("Token") as dev.martianzoo.pets.ast.Instruction, langWorld)
    }

    val gated =
        elaborate("MAX 0 Heat: Plant THEN Steel") as dev.martianzoo.pets.ast.Instruction.Then
    shouldThrow<NarrowingException> {
      gated.selectFirstStage(
          elaborate("Plant") as dev.martianzoo.pets.ast.Instruction,
          TableWorld(langTable, answer = false),
      )
    }
  }

  @Test
  internal fun `L3-8 selecting an OR arm binds later THEN stages`() {
    val sequence =
        elaborate("(@Token OR Plant) THEN @Token") as dev.martianzoo.pets.ast.Instruction.Then
    val proposal = elaborate("RedToken") as dev.martianzoo.pets.ast.Instruction

    sequence.selectFirstStage(proposal, langWorld).toString() shouldBe
        elaborate("RedToken THEN RedToken").toString()
  }

  @Test
  internal fun `L3-8 expansion matching ignores an occurrence unavailable in its universe`() {
    val table = testCatalog("ABSTRACT CLASS Shade\nCLASS Token<Shade>").classTable
    val expanded = parse<Expression>("Token<Shade>")
    val unavailable = parse<Expression>("Token<PremiseShade>")

    expanded.isExpandedFrom(unavailable, table) shouldBe false
  }

  @Test
  internal fun `L3-8 a later OR supplier is bound before its earlier observer is tested`() {
    val authored =
        elaborate("Heat<Player(HAS Selected@Token)> THEN (Selected@Token OR 2 Selected@Token)")
    val info =
        object : TypeInfo by langWorld {
          override fun has(requirement: Requirement): Boolean = "RedToken" in requirement.toString()

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            langTable.resolve(narrow).ensureNarrows(langTable.resolve(wide), this)
          }

          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
            langTable.resolve(narrow).ensureSelectionNarrows(langTable.resolve(wide), this)
          }
        }
    elaborate("Heat<Player1> THEN RedToken").narrows(authored, info) shouldBe true
    elaborate("Heat<Player1> THEN BlueToken").narrows(authored, info) shouldBe false
  }

  @Test
  internal fun `L3-8 abstract proposals keep every surviving shared occurrence`() {
    refuses("Chosen@Token THEN Chosen@Token", "Token THEN Token")
    narrows("Chosen@Token THEN Chosen@Token", "Chosen@Token THEN Chosen@Token") shouldBe true
    refuses(
        "Heat<Player(HAS Chosen@Token)> THEN Chosen@Token THEN Chosen@Token",
        "Heat<Player1> THEN Chosen@Token THEN Chosen@Token",
    )
    narrows("Heat<Player(HAS Chosen@Token)> THEN Chosen@Token", "Heat<Player1> THEN Ok") shouldBe
        false
    narrows("Heat<Player(HAS Chosen@Token)> THEN Chosen@Token?", "Heat<Player1> THEN Ok") shouldBe
        true
  }

  @Test
  internal fun `L3-8 a full proposal can disambiguate OR capture through its other stages`() {
    val source = "(Heat<Chosen@Player> OR Heat<Player1>) THEN Steel<Chosen@Player>"
    narrows(source, "Heat<Player1> THEN Steel<Player1>") shouldBe true
    narrows(source, "Heat<Player1> THEN Steel<Player2>") shouldBe true
    narrows(
        "(Plant: (Heat<Chosen@Player> OR Heat<Player1>)) THEN Steel<Chosen@Player>",
        "(Plant: Heat<Player1>) THEN Steel<Player2>",
    ) shouldBe true
    val sequence = elaborate(source) as Then
    shouldThrow<NarrowingException> {
      sequence.selectFirstStage(elaborate("Heat<Player1>") as Instruction, langWorld)
    }
  }

  @Test
  internal fun `L3-8 partial binding keeps shared identity through subsequent choices`() {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Place {
                  CLASS First
                  CLASS Second
                }
                CLASS Pair<Place, Place>
                CLASS Notice<Pair<Place, Place>>
                """
                    .trimIndent()
            )
            .classTable
    val elaborator = PetElaborator(table)
    fun instruction(source: String) = elaborator.elaborateInput(parse<InstructionTree>(source))
    val authored = instruction("Chosen@Pair<Place, Place> THEN Notice<Chosen@Pair>") as Then
    val variable = authored.typeVariables.variables.single()
    val partial =
        authored.typeVariables
            .bind(mapOf(variable to table.resolve(parse("Pair<Place, First>"))))
            .transformInstruction(authored) as Then
    val info = TableWorld(table)
    partial.typeVariables.variables.single() shouldBe variable
    instruction("Pair<First, First> THEN Notice<Pair<Second, First>>")
        .narrows(partial, info) shouldBe false
    instruction("Pair<First, First> THEN Notice<Pair<First, First>>")
        .narrows(partial, info) shouldBe true
    instruction("Chosen@Pair<Place, First> THEN Notice<Chosen@Pair>")
        .narrows(authored, info) shouldBe true
    instruction("Pair<Place, First> THEN Notice<Pair<Place, First>>")
        .narrows(authored, info) shouldBe false
  }

  @Test
  internal fun `L3-8 conflicting captures within one expression are narrowing refusals`() {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Place {
                  CLASS First
                  CLASS Second
                }
                CLASS Pair<Place, Place>
                CLASS Notice<Place>
                """
                    .trimIndent()
            )
            .classTable
    val elaborator = PetElaborator(table)
    val authored =
        elaborator.elaborateInput(
            parse<InstructionTree>("Pair<Chosen@Place, Chosen@Place> THEN Notice<Chosen@Place>")
        )
    val proposed =
        elaborator.elaborateInput(parse<InstructionTree>("Pair<First, Second> THEN Notice<First>"))
    proposed.narrows(authored, TableWorld(table)) shouldBe false
    shouldThrow<NarrowingException> { proposed.ensureNarrows(authored, TableWorld(table)) }
  }

  @Test
  internal fun `L3-8 OR compatibility does not test an unbound aggregate HAS MAX predicate`() {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Place {
                  CLASS First
                  CLASS Second
                }
                CLASS Marker<Place>
                CLASS Notice<Place>
                CLASS Other
                """
                    .trimIndent()
            )
            .classTable
    val elaborator = PetElaborator(table)
    val authored =
        elaborator.elaborateInput(
            parse<InstructionTree>(
                "(Chosen@Place(HAS MAX 0 Marker) OR Other) THEN Notice<Chosen@Place>"
            )
        )
    val proposed = elaborator.elaborateInput(parse<InstructionTree>("Second THEN Notice<Second>"))
    val info =
        object : TypeInfo by TableWorld(table) {
          override fun has(requirement: Requirement): Boolean =
              "Marker<Second>" in requirement.toString()

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            table.resolve(narrow).ensureNarrows(table.resolve(wide), this)
          }

          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
            table.resolve(narrow).ensureSelectionNarrows(table.resolve(wide), this)
          }
        }
    proposed.narrows(authored, info) shouldBe true
  }

  @Test
  internal fun `L3-8 predicates involving another choice are checked after all captures`() {
    val authored =
        elaborate(
            "Heat<Chosen@Player(HAS MAX 0 Selected@Token)> THEN Selected@Token THEN Steel<Chosen@Player>"
        )
    val proposed = elaborate("Heat<Player1> THEN BlueToken THEN Steel<Player1>")
    val info =
        object : TypeInfo by langWorld {
          override fun has(requirement: Requirement): Boolean =
              "MAX 0 BlueToken<Player1>" == requirement.toString()

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            langTable.resolve(narrow).ensureNarrows(langTable.resolve(wide), this)
          }

          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
            langTable.resolve(narrow).ensureSelectionNarrows(langTable.resolve(wide), this)
          }
        }
    proposed.narrows(authored, info) shouldBe true
  }

  @Test
  internal fun `L3-8 a full transmutation cannot erase an unsettled shared alias`() {
    refuses("Chosen@Token FROM Chosen@Token", "Token FROM Token")
    narrows("Chosen@Token FROM Chosen@Token", "Chosen@Token FROM Chosen@Token") shouldBe true
    narrows("Chosen@Token FROM Chosen@Token", "RedToken FROM RedToken") shouldBe true
    refuses("Chosen@Token FROM Chosen@Token", "RedToken FROM BlueToken")
  }

  @Test
  internal fun `L3-8 a partial proposal preserves a structural observer's shared alias`() {
    narrows(
        "Chosen@Token THEN Token(NOT Chosen@Token)",
        "Chosen@Token THEN Token(NOT Chosen@Token)",
    ) shouldBe true
  }

  // L3-9 What is not a choice

  @Test
  internal fun `L3-9 a gate, a metric, an actor and a selector must be reproduced exactly`() {
    narrows("MAX 0 Heat: Tile<>", "MAX 0 Heat: GreeneryTile<Land1>") shouldBe true
    refuses("MAX 0 Heat: Tile<>", "MAX 0 Plant: GreeneryTile<Land1>")

    narrows("Tile<> / Heat", "GreeneryTile<Land1> / Heat") shouldBe true
    refuses("Tile<> / Heat", "GreeneryTile<Land1> / Plant")

    narrows("Tile<> BY Player1", "GreeneryTile<Land1> BY Player1") shouldBe true
    refuses("Tile<> BY Player1", "GreeneryTile<Land1> BY Player2")

    narrows("EACH Player { Tile<> }", "EACH Player { GreeneryTile<Land1> }") shouldBe true
    refuses("EACH Player { Tile<> }", "EACH Area { GreeneryTile<Land1> }")
  }

  // L3-10 The two spellings

  @Test
  internal fun `L3-10 narrows answers and ensureNarrows explains`() {
    narrows("2 Plant!", "3 Plant!") shouldBe false
    refuses("2 Plant!", "3 Plant!").message!!.contains("does not narrow") shouldBe true
  }

  @Test
  internal fun `L3-10 narrows propagates failures other than a narrowing refusal`() {
    shouldThrow<IllegalStateException> {
      BrokenSpecification.narrows(BrokenSpecification, langWorld)
    }
  }
}
