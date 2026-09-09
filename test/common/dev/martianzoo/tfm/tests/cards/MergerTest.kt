package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MergerTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(
        VenusNextExpansion,
        PreludeExpansion,
        PromoCardPack,
        retainedStartingProjects = 5,
    )
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Merger) {
      p1.playCorp(Celestic)
    }
  }

  @Test
  internal fun `Can choose Celestic after Valley Trust`() {
    p1.assertCounts(0 to "PreludeCard", 6 to "ProjectCard")
  }

  @Test
  internal fun `Resolves both corporations' starting benefits`() {
    admin.phase("Action")

    p1.stdAction("DoRequiredActions") {
      p1.assertCounts(8 to "ProjectCard", 0 to "PreludeCard")
      p1.assertProds(
          0 to "MC",
          0 to "Steel",
          0 to "Titanium",
          0 to "Plant",
          0 to "Energy",
          0 to "Heat",
      )

      p1.playPrelude(SocietySupport)
      p1.assertProds(
          -1 to "MC",
          0 to "Steel",
          0 to "Titanium",
          1 to "Plant",
          1 to "Energy",
          1 to "Heat",
      )
    }
  }

  @Test
  internal fun `Can resolve Merger payment and the second corporation`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")
    p1.manual("PreludeCard")

    p1.playPrelude(Merger) {
      p1.playCorp(Celestic)
    }

    p1.assertCounts(1 to "$Celestic")
  }

  @Test
  internal fun `New Partner can play Merger while both card families are being selected`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")

    p1.playPrelude(NewPartner) {
      p1.playPrelude(Merger) {
        p1.playCorp(Celestic)
      }
    }

    p1.assertCounts(
        0 to "PreludeCard<Selecting>",
        0 to "CorporationCard<Selecting>",
        1 to "$Merger",
        1 to "$Celestic",
    )
  }

  @Test
  internal fun `Polyphemos then Merger into TerraLabs still buys cards for three`() {
    newGame(
        ColoniesExpansion,
        TurmoilExpansion,
        PreludeExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    playCorporationWithoutStartingProjects(p1, Polyphemos)
    admin.phase("Prelude")
    p1.playPrelude(Merger) {
      p1.playCorp(TerraLabsResearch)
    }

    p1.manual("ProjectCard<Selecting> THEN BuySelectedCards") {
          p1.pay(mc = 3)
        }
        .expect("ProjectCard, -3 MC")
  }
}
