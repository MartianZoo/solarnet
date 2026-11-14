package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SuitableInfrastructureTest : TfmGameplayTest() {
  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Queues a single reward for multiple production increases without player autoexecution`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")

    kim.autoExecPolicy = NONE
    kim.completeOperation {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
              "$DomeFarming FROM PreludeCard",
          )
          kim.doTask("PROD[2 MC]").expect("0 MC")
          kim.doTask("PROD[Plant]").expect("0 MC")
          // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
          kim.selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("PROD[Plant, 2 MC], 2 MC")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
  }

  @Test
  internal fun `Pays again for the second action without player autoexecution`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.autoExecPolicy = NONE
    kim.completeOperation {
          doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC")
          kim.doTask("PROD[Energy]").expect("0 MC")
          kim.selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("PROD[Energy], -9 MC")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
    kim.completeOperation {
          doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC")
          kim.doTask("PROD[Energy]").expect("0 MC")
          kim.selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("PROD[Energy], -9 MC")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
    kim.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `An unused bonus expires before the next action`() {
    newTestGame("PreludeExpansion, SuitableInfrastructure", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
        .expect("MC, -ProjectCard")
    kim.count("SuitableInfrastructureBonus") shouldBe 0
    kim.doTasks(*standardProjectTasks("PowerPlantProject")).expect("PROD[Energy], -9 MC")
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
          doTask("DoRequiredActions")
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
