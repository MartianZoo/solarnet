package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import org.junit.jupiter.api.Test

class ReifyTest {
  val game = Engine.newGame(GameSetup(Canon, "BM", 4))

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
    test("M55! OR M66!", "M55!")
    test("M55! OR M66!", "M66!")
    test("5 OxygenStep! / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep. / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep! / Plant<Anyone>")
    test("5 OxygenStep? / Plant<Anyone>", "5 OxygenStep. / Plant<Anyone>")
  }

  fun test(original: String, replacement: String) =
      instruction(replacement).ensureReifies(instruction(original), game.einfo)
}
