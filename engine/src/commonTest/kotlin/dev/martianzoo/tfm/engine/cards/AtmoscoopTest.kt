package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AtmoscoopTest : CardTest() {
  // FAQ: "you can choose to raise Temperature or Venus even if that parameter is maxed"
  @Test
  fun `may choose an already-maxed Venus track instead of raising temperature`() {
    val p1 = newGame("BMVR", 2).tfm(PLAYER1)
    p1.sneak("15 VenusStep, AerialMappers")

    p1.manual("Atmoscoop") {
      doTask("Ok THEN VenusStep")
      doTask("2 Floater<AerialMappers>")
    }

    p1.count("TemperatureStep") shouldBe 0
    p1.count("VenusStep") shouldBe 15
  }
}
