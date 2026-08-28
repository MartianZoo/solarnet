package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SpecificationTest {
  private val game = Engine.newGame(canonicalPremise())

  @Test
  internal fun instructionsNarrowCompositionally() {
    test("5 OxygenStep!", "5 OxygenStep!")
    test("5 OxygenStep.", "5 OxygenStep.")
    test("5 OxygenStep?", "5 OxygenStep!")
    test("5 OxygenStep?", "5 OxygenStep.")
    test("-OxygenStep!", "-OxygenStep!")
    test("-OxygenStep.", "-OxygenStep.")
    test("-OxygenStep?", "-OxygenStep!")
    test("-OxygenStep?", "-OxygenStep.")
    test("5 GlobalParameter!", "5 OxygenStep!")
    test("5 GlobalParameter.", "5 OxygenStep.")
    test("5 GlobalParameter?", "5 OxygenStep!")
    test("5 GlobalParameter?", "5 OxygenStep.")
    test("X StandardResource?", "2 Plant!")
    test("MarsArea!", "Tharsis_5_5!")
    test("WaterArea!", "Tharsis_5_5!")
    test("Tharsis_5_5! OR Tharsis_6_6!", "Tharsis_5_5!")
    test("Tharsis_5_5! OR Tharsis_6_6!", "Tharsis_6_6!")
    test("Tharsis_5_5! OR Tharsis_6_6!", "Tharsis_5_5! OR Tharsis_6_6!")
    test("Tharsis_5_5! OR Tharsis_6_6! OR Tharsis_7_7!", "Tharsis_5_5! OR Tharsis_7_7!")
    test("Plant: 2 StandardResource?", "Plant: 2 Heat?")
    test("Plant: 2 StandardResource?", "Plant: 2 Heat!")
    test("Plant: 2 StandardResource?", "Plant: Heat!")
    test("Plant: 2 StandardResource?", "Plant: Ok")
    test("Plant: 2 StandardResource?", "Plant: StandardResource!")
    test("5 OxygenStep! / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep. / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")

    test("WaterArea(HAS MAX 0 Tile)!", "Tharsis_5_5!")
    test("StandardResource<!Player1>?", "Plant<Player2>!")
    testInvalid("WaterArea(HAS Tile)!", "Tharsis_5_5!")
    testInvalid("StandardResource<!Player1>?", "Plant<Player1>!")
    testInvalid("Plant: 2 StandardResource?", "Heat: 2 Heat!")
  }

  @Test
  internal fun noOpDoesNotNarrowAnUnresolvedPerInstruction() {
    testInvalid("2 Plant! / Player", "Ok")
  }

  @Test
  internal fun unresolvedInstructionsCanAlreadyBeNonAbstract() {
    parse<Instruction>("5 OxygenStep.").isAbstract(game.reader) shouldBe false
    parse<Instruction>("Plant: 5 OxygenStep!").isAbstract(game.reader) shouldBe false
    parse<Instruction>("5 OxygenStep! / Plant<Player1>").isAbstract(game.reader) shouldBe false
  }

  @Test
  internal fun refinedTypeNarrowingNeedsGameContext() {
    val concrete = game.reader.resolve(parse<Expression>("Tharsis_5_5"))
    val refined = game.reader.resolve(parse<Expression>("WaterArea(HAS MAX 0 Tile)"))

    shouldThrow<IllegalStateException> { concrete.isSubtypeOf(refined) }
    assertNarrows(concrete, refined, game.reader)
  }

  @Test
  internal fun unresolvedRefinedExpressionsNarrowBeforeResolution() {
    val wider = parse<Expression>("WaterArea(HAS MAX 0 Tile)")
    val narrower = parse<Expression>("Tharsis_5_5(HAS MAX 0 Tile)")

    assertNarrows(narrower, wider, game.reader)
  }

  @Test
  internal fun compactTransmutationLinksItsUnchangedArguments() {
    val wide = "Production<Player, Class<Steel FROM Heat>>?"

    test(
        wide,
        "Production<Player1, Class<Steel>> FROM Production<Player1, Class<Heat>>!",
    )
    testInvalid(
        wide,
        "Production<Player1, Class<Steel>> FROM Production<Player2, Class<Heat>>!",
    )
  }

  private fun test(widerText: String, narrowerText: String) {
    val narrower: Instruction = parse(narrowerText)
    val wider: Instruction = parse(widerText)
    assertNarrows(narrower, wider, game.reader)
  }

  private fun testInvalid(widerText: String, narrowerText: String) {
    val narrower: Instruction = parse(narrowerText)
    val wider: Instruction = parse(widerText)
    narrower.narrows(wider, game.reader) shouldBe false
    shouldThrow<NarrowingException> { narrower.ensureNarrows(wider, game.reader) }
  }

  private fun <S : Specification<S>> assertNarrows(narrower: S, wider: S, info: TypeInfo) {
    narrower.narrows(wider, info) shouldBe true
    narrower.ensureNarrows(wider, info)
  }
}
