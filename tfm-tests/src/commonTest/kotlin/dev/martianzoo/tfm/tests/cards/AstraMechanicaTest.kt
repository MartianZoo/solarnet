package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.AstraMechanica
import kotlin.test.Test

internal class AstraMechanicaTest : CardTest() {
  @Test
  internal fun `Can choose zero played events`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 Megacredit, ProjectCard")

    p1.playProject(AstraMechanica, 7) {
          // Decline retrieving played events.
          declineTask()
        }
        .expect("-7, -ProjectCard")

    p1.assertCounts(1 to "$AstraMechanica", 0 to "PlayedEvent")
  }
}
