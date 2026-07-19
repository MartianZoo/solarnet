package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LavaFlowsTest : CardTest() {
  @Test
  fun `places its tile on a volcanic area on Tharsis`() {
    val game = newGame(Canon.SIMPLE_GAME)
    val p1 = game.tfm(PLAYER1)

    p1.godMode().manual("LavaFlows") { doTask("LfTile<Tharsis_2_2>") }.expect("2 TemperatureStep")
  }

  @Test
  fun `uses an ordinary land area on a map without volcanic areas`() {
    val game = newGame(Canon.fromOptionCodes("BRH", 2))
    val p1 = game.tfm(PLAYER1)

    p1.godMode().manual("LavaFlows") { doTask("LfTile<Hellas_1_5>") }.expect("2 TemperatureStep")
  }

  @Test
  fun `does not fall back to ordinary land when every Tharsis volcanic area is occupied`() {
    val game = newGame(Canon.SIMPLE_GAME)
    val p1 = game.tfm(PLAYER1)
    p1.godMode()
        .manual(
            "GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_3_1>, " +
                "GreeneryTile<Tharsis_4_1>, GreeneryTile<Tharsis_5_1>"
        )

    p1.godMode().manual("LavaFlows") {
      shouldThrow<NarrowingException> { doTask("LfTile<Tharsis_2_3>") }
      abort()
    }

    p1.assertCounts(0 to "LavaFlows", 0 to "LfTile")
    p1.temperatureC() shouldBe -30
  }
}
