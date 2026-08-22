package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class PolderTechDutchTest : CardTest() {
  @Test
  fun `Initial action places adjacent ocean and greenery without an existing owned tile`() {
    newGame(PromoCardPack)
    p1.playCorp(PolderTechDutch, 0)
    engine.phase("Action")

    p1.stdAction("HandleMandates") {
          placeTile(1, 4)
          shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_1_5>") }
          shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_2_1>") }
          placeTile(1, 3)
        }
        .expect("OceanTile, GreeneryTile, OxygenStep, Energy, Plant")

    p1.assertCounts(1 to "Energy", 1 to "Plant")
  }

  @Test
  fun `Later ocean and greenery placements grant their resources`() {
    newGame(PromoCardPack)
    p1.playCorp(PolderTechDutch, 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates") {
      placeTile(1, 4)
      placeTile(1, 3)
    }

    p1.manual("OceanTile<Tharsis_2_3>").expect("Energy")
    p1.manual("GreeneryTile<Tharsis_2_2>").expect("Plant")
  }
}
