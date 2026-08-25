package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ReifyTest {
  private val game = Engine.newGame(canonicalPremise())

  @Test
  internal fun testVarious() {
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
    shouldThrow<NarrowingException> { test("WaterArea(HAS Tile)!", "Tharsis_5_5!") }
  }

  @Test
  internal fun refinedTypeNarrowingNeedsGameContext() {
    val concrete = game.reader.resolve(parse<Expression>("Tharsis_5_5"))
    val refined = game.reader.resolve(parse<Expression>("WaterArea(HAS MAX 0 Tile)"))

    shouldThrow<IllegalStateException> { concrete.isSubtypeOf(refined) }
    concrete.narrows(refined, game.reader) shouldBe true
  }

  @Test
  internal fun compactTransmutationLinksItsUnchangedArguments() {
    val wide = "Production<Player, Class<Steel FROM Heat>>?"

    test(
        wide,
        "Production<Player1, Class<Steel>> FROM Production<Player1, Class<Heat>>!",
    )
    shouldThrow<NarrowingException> {
      test(
          wide,
          "Production<Player1, Class<Steel>> FROM Production<Player2, Class<Heat>>!",
      )
    }
  }

  private fun test(original: String, replacement: String) {
    val narrower: Instruction = parse(replacement)
    val wider: Instruction = parse(original)
    narrower.ensureNarrows(wider, game.reader)
  }
}
