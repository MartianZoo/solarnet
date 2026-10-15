package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SuitableInfrastructureTest : TfmGameplayTest() {
  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Pays once when a Prelude raises two production tracks`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")

    kim.completeOperation {
          doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")
          autoExecNow(EAGER)
        }
        .expect("PROD[Plant, 2 MC], 2 MC")
  }

  @Test
  internal fun `Pays again for the second action in the same turn`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.doTasks("UseStandardProject<PowerPlantProject>", "-11 MC").expect("PROD[Energy], -9 MC")
    kim.doTasks("UseStandardProject<PowerPlantProject>", "-11 MC").expect("PROD[Energy], -9 MC")
  }

  @Test
  internal fun `Pays for production inside Valley Trusts required action`() {
    newTestGame(
        "PreludeExpansion, SuitableInfrastructure, ValleyTrust",
        playerCount = 2,
        kimCorporation = ValleyTrust,
    )
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.completeOperation {
          doTask("RequiredActionsSignal")
          doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")
          autoExecNow(EAGER)
        }
        .expect("PROD[Plant, 2 MC], 2 MC")
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Pays for corporation production when Merger is played during Preludes`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")

    kim.completeOperation {
          doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
          doTask("PlayCard<Class<CorporationCard>, Class<$Manutech>>")
          autoExecNow(EAGER)
        }
        .expect("PROD[Steel], Steel, -5 MC")
  }
}
