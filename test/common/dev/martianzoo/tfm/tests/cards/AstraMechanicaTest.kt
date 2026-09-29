package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrowAny
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

  @Test
  internal fun `Cannot recover Lava Flows or Deimos Down promo`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$LavaFlows") { placeTile(2, 2) }
    p1.runOperation("$DeimosDownPromo") { placeTile(4, 5) }
    p1.runOperation("7 MC, ProjectCard")

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            shouldThrowAny { doTask("ProjectCard FROM PlayedEvent<Class<$LavaFlows>>") }
            shouldThrowAny { doTask("ProjectCard FROM PlayedEvent<Class<$DeimosDownPromo>>") }
            // Neither retrieval has an eligible event.
            doTask("Ok")
            doTask("Ok")
          }
        }
        .expect("0 PlayedEvent, 0 SpecialTile")
  }

  @Test
  internal fun `Cannot recover flipped Pharmacy Union`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    admin.phase("Action")
    p1.runOperation("$PharmacyUnion")
    p1.runOperation("$Research")
    p1.runOperation("$PhysicsComplex") {
      doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion THEN 3 TerraformRating")
    }

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            shouldThrowAny { doTask("ProjectCard FROM PlayedEvent<Class<$PharmacyUnion>>") }
            // The flipped corporation is ineligible for either retrieval.
            doTask("Ok")
            doTask("Ok")
          }
        }
        .expect("0 PlayedEvent<Class<$PharmacyUnion>>")
  }

  @Test
  internal fun `Cannot recover another player's event`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    requireP2().runOperation("$MineralDeposit")
    p1.runOperation("7 MC, ProjectCard")

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            shouldThrowAny {
              doTask("ProjectCard FROM PlayedEvent<Player2, Class<$MineralDeposit>>")
            }
            // Neither retrieval has an event belonging to this player.
            doTask("Ok")
            doTask("Ok")
          }
        }
        .expect("0 PlayedEvent<Player2, Class<$MineralDeposit>>")
  }

  @Test
  internal fun `Can recover an ocean event`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$Flooding") { placeTile(5, 5) }
    p1.runOperation("7 MC, ProjectCard")

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
            // There is only one event to recover.
            doTask("Ok")
          }
        }
        .expect("0 OceanTile")
  }
}
