package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.AstraMechanica
import kotlin.test.Test

class AstraMechanicaTest : CardTest() {
  @Test
  fun `Can choose zero played events`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 Megacredit, ProjectCard")

    p1.playProject(AstraMechanica, 7) { doTask("Ok") }.expect("-7, -ProjectCard")

    p1.assertCounts(1 to "$AstraMechanica", 0 to "PlayedEvent")
  }
}
