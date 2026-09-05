package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.AstraMechanica
import dev.martianzoo.tfm.tests.cards.cardnames.InvestmentLoan
import dev.martianzoo.tfm.tests.cards.cardnames.MineralDeposit
import kotlin.test.Test

internal class AstraMechanicaTest : CardTest() {
  @Test
  internal fun `Can choose zero played events`() {
    newGameWithAutoWorkflow(PromoCardPack)
    playUntilFirstActionPhase()

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            // Decline both opportunities to retrieve a played event.
            doTask("Ok")
            doTask("Ok")
          }
        }
        .expect("$AstraMechanica, -7 MC, -ProjectCard, 0 PlayedEvent")
  }

  @Test
  internal fun `Can return two differently typed played events`() {
    newGameWithAutoWorkflow(PromoCardPack)
    playUntilFirstActionPhase()

    p1.turn {
      playProject(MineralDeposit, 5)
      playProject(InvestmentLoan, 3)
    }
    requireP2().pass()

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            doTask("ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>")
            doTask("ProjectCard FROM PlayedEvent<Class<$InvestmentLoan>>")
          }
        }
        .expect(
            "$AstraMechanica, ProjectCard, " +
                "-PlayedEvent<Class<$MineralDeposit>>, -PlayedEvent<Class<$InvestmentLoan>>"
        )
  }
}
