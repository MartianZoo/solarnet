package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class MergerTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Merger) { doTask("PlayCard<Class<CorporationCard>, Class<$Celestic>>") }
  }

  @Test
  fun `Can choose Celestic after Valley Trust`() {
    p1.assertCounts(0 to "PreludeCard", 6 to "ProjectCard")
  }

  @Test
  fun `Resolves both corporations' starting benefits`() {
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

      doTask("PlayCard<Class<PreludeCard>, Class<$SocietySupport>>")
      p1.assertProds(
          -1 to "Megacredit",
          0 to "Steel",
          0 to "Titanium",
          1 to "Plant",
          1 to "Energy",
          1 to "Heat",
      )
    }
  }

  @Test
  fun `Can pay for Merger before playing the second corporation`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    engine.phase("Prelude")
    p1.manual("PreludeCard")
    p1.autoExecMode = NONE

    p1.playPrelude(Merger) {
      doTask("$Merger FROM PreludeCard")
      doTask("4 CorporationCard")
      doTask("-3 CorporationCard")
      doTask("-42 Megacredit")
      doTask("PlayCard<Class<CorporationCard>, Class<$Celestic>>")
      p1.autoExecMode = FIRST
    }

    p1.assertCounts(1 to "$Celestic")
  }
}
