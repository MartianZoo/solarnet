package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MiningGuildTest : CardTest() {
  @Test
  fun `Raises steel production for metal areas but not card-bonus areas`() {
    newGame()
    p1.manual("$MiningGuild")
    p1.count("PROD[Steel]") shouldBe 1

    p1.manual("CityTile<Tharsis_1_1>").expect("PROD[Steel]") // LSS
    p1.count("PROD[Steel]") shouldBe 2

    p1.manual("CityTile<Tharsis_8_9>").expect("PROD[Steel]") // Titanium
    p1.count("PROD[Steel]") shouldBe 3

    p1.manual("CityTile<Tharsis_2_1>") // L
    p1.count("PROD[Steel]") shouldBe 3
  }
}
