package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import kotlin.test.BeforeTest
import kotlin.test.Test

class MergerTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("TerraformingMars,TharsisMap,VenusNextExpansion,PreludeExpansion,PromoCardPack")
    p1.playCorp("ValleyTrust", 5)
    engine.phase("Prelude")
    p1.playPrelude("UnmiContractor")
    p1.playPrelude("Merger") { doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>") }
  }

  @Test
  fun `after Valley Trust, plays Merger choosing Celestic`() {
    p1.assertCounts(2 to "Mandate", 0 to "PreludeCard", 6 to "ProjectCard")
  }

  @Test
  fun `after Merger adds Celestic, handles both mandates`() {
    engine.phase("Action")

    p1.stdAction("HandleMandates") {
      p1.assertCounts(8 to "ProjectCard", 1 to "PreludeCard")
      p1.assertProds(
          0 to "Megacredit",
          0 to "Steel",
          0 to "Titanium",
          0 to "Plant",
          0 to "Energy",
          0 to "Heat",
      )

      doTask("PlayCard<Class<PreludeCard>, Class<SocietySupport>>")
      p1.assertProds(
          -1 to "Megacredit",
          0 to "Steel",
          0 to "Titanium",
          1 to "Plant",
          1 to "Energy",
          1 to "Heat",
      )
    }
    p1.assertCounts(0 to "Mandate")
  }
}
