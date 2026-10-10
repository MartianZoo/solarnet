package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SuitableInfrastructureTest : TfmGameplayTest() {
  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Pays once when a Prelude raises two production tracks`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.playPrelude(SuitableInfrastructure)

    kim.playPrelude(DomeFarming).expect("PROD[Plant, 2 MC], 2 MC")
  }

  @Test
  internal fun `Pays again for the second action in the same turn`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.playPrelude(SuitableInfrastructure)
    kim.playPrelude(Donation)
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)

    kim.turn {
      stdProject("PowerPlantProject").expect("PROD[Energy], -9 MC")
      stdProject("PowerPlantProject").expect("PROD[Energy], -9 MC")
    }
  }

  @Test
  internal fun `Pays for production inside Valley Trusts required action`() {
    newTestGame(
        "PreludeExpansion, SuitableInfrastructure",
        playerCount = 2,
        kimCorporation = ValleyTrust,
    )
    kim.playPrelude(SuitableInfrastructure)
    kim.playPrelude(Donation)
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)

    kim.stdAction("DoRequiredActionsAction") { kim.playPrelude(DomeFarming) }
        .expect("PROD[Plant, 2 MC], 2 MC")
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Pays for corporation production when Merger is played during Preludes`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.playPrelude(SuitableInfrastructure)

    kim.playPrelude(Merger) { kim.playCorp(Manutech) }.expect("PROD[Steel], Steel, -5 MC")
  }
}
