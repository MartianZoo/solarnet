package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LavaFlowsTest : CardTest() {
  @Test
  internal fun `Can place its tile on Tharsis`() {
    newGame()
    p1.manual("$LavaFlows") { placeTile(2, 2) }.expect("2 TemperatureStep")
  }

  @Test
  internal fun `Can place its tile on Hellas`() {
    newGame(Hellas)
    p1.manual("$LavaFlows") { placeTile(1, 5) }.expect("2 TemperatureStep")
  }

  @Test
  internal fun `Cannot be played when every volcanic area is occupied`() {
    newGame()
    p1.manual(
        "GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_3_1>, " +
            "GreeneryTile<Tharsis_4_1>, GreeneryTile<Tharsis_5_1>"
    )

    shouldThrow<NotNowException> { p1.manual("$LavaFlows") }

    p1.count("Tile<Tharsis_2_3>") shouldBe 0
    p1.temperatureC() shouldBe -30
  }
}
