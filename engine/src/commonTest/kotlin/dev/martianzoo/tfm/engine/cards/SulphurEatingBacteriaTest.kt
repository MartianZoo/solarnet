package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import kotlin.test.Test

class SulphurEatingBacteriaTest : CardTest() {

  @Test
  fun sulphurEatingBacteria() {
    val game = newGame("BMV", 2)
    with(game.tfm(PLAYER1)) {
      phase("Action")

      sneak("5 ProjectCard, SulphurEatingBacteria")
      assertCounts(0 to "Microbe", 0 to "Megacredit")

      manual("UseAction1<SulphurEatingBacteria>")
      assertCounts(1 to "Microbe", 0 to "Megacredit")

      manual("UseAction2<SulphurEatingBacteria>") {
        doTask("-Microbe<SulphurEatingBacteria> THEN 3")
      }
      assertCounts(0 to "Microbe", 3 to "Megacredit")

      sneak("4 Microbe<SulphurEatingBacteria>")
      assertCounts(4 to "Microbe", 3 to "Megacredit")

      cardAction2("C251") {
        fun assertTaskFails(task: String, desc: String) =
            withClue(desc) { shouldThrow<Exception> { doTask(task) } }

        assertTaskFails("-Microbe<C251> THEN 4", "greed")
        assertTaskFails("-Microbe<C251> THEN 2", "shortchanged")
        assertTaskFails("-Microbe<C251>", "no get paid")
        assertTaskFails("-3 Microbe THEN 9", "which microbe")
        assertTaskFails("-5 Microbe<C251> THEN 15", "more than have")
        assertTaskFails("-0 Microbe<C251> THEN 0", "x can't be zero")
        assertTaskFails("-3 Resource<C251> THEN 9", "what kind")
        assertTaskFails("9 THEN -3 Microbe<C251>", "out of order")
        assertTaskFails("2 Microbe<C251> THEN -6", "inverse")

        assertCounts(4 to "Microbe", 3 to "Megacredit")

        doTask("-3 Microbe<C251> THEN 9")
        assertCounts(1 to "Microbe", 12 to "Megacredit")
      }
    }
  }
}
