package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Instruction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReifyTest {
  val game = Engine.newGame(Canon.SIMPLE_GAME)

  @Test
  fun testVarious() {
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
    test("MarsArea!", "M55!")
    test("WaterArea!", "M55!")
    test("M55! OR M66!", "M55!")
    test("M55! OR M66!", "M66!")
    test("M55! OR M66!", "M55! OR M66!")
    test("M55! OR M66! OR M77!", "M55! OR M77!")
    test("Plant: 2 StandardResource?", "Plant: 2 Heat?")
    test("Plant: 2 StandardResource?", "Plant: 2 Heat!")
    test("Plant: 2 StandardResource?", "Plant: Heat!")
    test("Plant: 2 StandardResource?", "Plant: Ok")
    // test("Plant: 2 StandardResource?", "Ok") TODO
    test("Plant: 2 StandardResource?", "Plant: StandardResource!")
    test("5 OxygenStep! / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep. / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")

    test("WaterArea(HAS MAX 0 Tile)!", "M55!")
    assertThrows<NarrowingException> { test("WaterArea(HAS Tile)!", "M55!") }
  }

  fun test(original: String, replacement: String) {
    val narrower: Instruction = parse(replacement)
    val wider: Instruction = parse(original)
    narrower.ensureNarrows(wider, game.reader)
  }
}
