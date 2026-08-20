package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LavaFlowsTest : CardTest() {
  @Test
  fun `on Tharsis, resolves Lava Flows`() {
    newGame()
    p1.manual("$LavaFlows") { doTask("Card140_SpecialTile<Tharsis_2_2>") }
        .expect("2 TemperatureStep")
  }

  @Test
  fun `on Hellas, resolves Lava Flows`() {
    newGame(HellasMapOption)
    p1.manual("$LavaFlows") { doTask("Card140_SpecialTile<Hellas_1_5>") }
        .expect("2 TemperatureStep")
  }

  @Test
  fun `with every volcanic area occupied, tries to place Lava Flows`() {
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
