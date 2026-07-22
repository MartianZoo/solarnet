package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ManutechTest : CardTest() {

  @Test
  fun manutech() {
    val game = newGame("BMV", 2)
    with(game.tfm(PLAYER1)) {
      playCorp("Manutech", 5)
      assertCounts(1 to "PROD[Steel]", 1 to "Steel")

      manual("PROD[8, 6T, 7P, 5E, 3H]")
      production().values.shouldContainExactly(8, 1, 6, 7, 5, 3)
      assertCounts(28 to "M", 1 to "S", 6 to "T", 7 to "P", 5 to "E", 3 to "H")

      manual("-7 Plant")
      assertCounts(0 to "Plant")

      manual("Moss")
      production().values.shouldContainExactly(8, 1, 6, 8, 5, 3)
      assertCounts(28 to "M", 1 to "S", 6 to "T", 0 to "P", 5 to "E", 3 to "H")
    }
  }

  @Test
  fun `Nitrophilic Moss can spend the plants produced by Manutech`() {
    val p1 = newGame("BMV", 2).tfm(PLAYER1)
    p1.sneak("Manutech")

    p1.manual("NitrophilicMoss")

    p1.count("PROD[Plant]") shouldBe 2
    p1.count("Plant") shouldBe 0
  }
}
