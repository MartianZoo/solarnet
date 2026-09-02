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
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Merger) { doTask("PlayCard<Class<CorporationCard>, Class<$Celestic>>") }
  }

  @Test
  internal fun `Can choose Celestic after Valley Trust`() {
    p1.assertCounts(0 to "PreludeCard", 6 to "ProjectCard")
  }

  @Test
  internal fun `Resolves both corporations' starting benefits`() {
    engine.phase("Action")

    p1.stdAction("HandleMandates") {
      p1.assertCounts(8 to "ProjectCard", 1 to "PreludeCard")
      p1.assertProds(
          0 to "MC",
          0 to "Steel",
          0 to "Titanium",
          0 to "Plant",
          0 to "Energy",
          0 to "Heat",
      )

      doTask("PlayCard<Class<PreludeCard>, Class<$SocietySupport>>")
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
    p1.playCorp(CrediCor, 0)
    engine.phase("Prelude")
    p1.manual("PreludeCard")

    p1.playPrelude(Merger) { doTask("PlayCard<Class<CorporationCard>, Class<$Celestic>>") }

    p1.assertCounts(1 to "$Celestic")
  }

  @Test
  internal fun `Polyphemos then Merger into TerraLabs still buys cards for three`() {
    newGame(
        ColoniesExpansion,
        TurmoilCardPack,
        PreludeExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.playCorp(Polyphemos, 0)
    engine.phase("Prelude")
    p1.playPrelude(Merger) {
      doTask("PlayCard<Class<CorporationCard>, Class<$TerraLabsResearch>>")
    }

    p1.manual("Selecting THEN ProjectCard<Selecting> THEN BuySelectedCards") {
          p1.pay(mc = 3)
        }
        .expect("ProjectCard, -3 MC")
  }
}
