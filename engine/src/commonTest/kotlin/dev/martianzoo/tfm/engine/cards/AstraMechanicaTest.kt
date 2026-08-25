package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class AstraMechanicaTest : CardTest() {
  @Test
  internal fun `Can choose zero played events`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 Megacredit, ProjectCard")

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            // Decline both opportunities to retrieve a played event.
            doTask("Ok")
            doTask("Ok")
          }
        }
        .expect("-7, -ProjectCard")

    p1.assertCounts(1 to "$AstraMechanica", 0 to "PlayedEvent")
  }

  @Test
  internal fun `Can return two differently typed played events`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual(
        "7 Megacredit, ProjectCard, PlayedEvent<Class<$Flooding>>, " +
            "PlayedEvent<Class<$InventionContest>>"
    )

    p1.playProject(AstraMechanica, 7) {
      doWithoutAutoExec(p1) {
        doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
        doTask("ProjectCard FROM PlayedEvent<Class<$InventionContest>>")
      }
    }

    p1.assertCounts(1 to "$AstraMechanica", 2 to "ProjectCard", 0 to "PlayedEvent")
  }
}
