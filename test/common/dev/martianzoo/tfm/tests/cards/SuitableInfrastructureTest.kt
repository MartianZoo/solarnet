package dev.martianzoo.tfm.tests.cards

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
    newTestGame("PreludeExpansion, $SuitableInfrastructure", playerCount = 2)
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>",
        "$SuitableInfrastructure FROM PreludeCard",
        "5 Steel",
    )

    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
      )
      kim.doTask("PROD[2 MC]").expect("0 MC")
      kim.doTask("PROD[Plant]").expect("0 MC")
      // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
      selectTask("2 MC")
      doTask("2 MC")
    }

    shouldThrow<TaskException> { kim.doTask("2 MC") }
  }

  @Test
  internal fun `Pays again for the second action without player autoexecution`() {
    newTestGame("$SuitableInfrastructure", playerCount = 2)
    kim.exMachina("$SuitableInfrastructure") // Only the continuing bonus is under test here.

    kim.completeOperation {
          doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC")
          kim.doTask("PROD[Energy]").expect("0 MC")
          // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
          selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("-9 MC")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
    kim.completeOperation {
          doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC")
          kim.doTask("PROD[Energy]").expect("0 MC")
          // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
          selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("-9 MC")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
    stan.doTask("Pass") // Both actions finished and the turn reached Stan.
  }

  @Test
  internal fun `An unused bonus expires before the next action`() {
    newTestGame("$SuitableInfrastructure", playerCount = 2)
    kim.exMachina("$SuitableInfrastructure") // Only the continuing bonus is under test here.

    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")

    kim.count("SuitableInfrastructureBonus") shouldBe 0
    kim.completeOperation {
          doTasks(
              *standardProjectTasks("PowerPlantProject"),
              "11 Pay<> FROM MC",
              "PROD[Energy]",
              "2 MC",
          )
        }
        .expect("-9 MC")
  }

  @Test
  internal fun `Pays for production inside Valley Trusts required action`() {
    newTestGame(
        "$SuitableInfrastructure, $ValleyTrust, prelude:cards",
        playerCount = 2,
        kimCorporation = ValleyTrust,
    )
    kim.exMachina("$SuitableInfrastructure") // Only the continuing bonus is under test here.

    kim.completeOperation {
          doTasks(
              "DoRequiredActions",
              "3 PreludeCard<Selecting>",
              "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
              "$DomeFarming FROM PreludeCard<Selecting>",
          )
          kim.doTask("PROD[2 MC]").expect("0 MC")
          kim.doTask("PROD[Plant]").expect("0 MC")
          // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
          selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("-RequiredAction, 0 PreludeCard<Selecting>")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Pays for corporation production when Merger is played during Preludes`() {
    newTestGame("PreludeExpansion, $SuitableInfrastructure", playerCount = 2)
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>",
        "$SuitableInfrastructure FROM PreludeCard",
        "5 Steel",
    )

    kim.completeOperation {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$Merger>>",
              "$Merger FROM PreludeCard",
              "4 CorporationCard<Selecting>",
              "PlayCard<Class<CorporationCard>, Class<$Manutech>>",
              "$Manutech FROM CorporationCard<Selecting>",
              "35 MC",
              "-42 MC",
          )
          kim.doTask("PROD[Steel]").expect("0 MC, 0 Steel")
          doTask("Steel")
          // Select the full reward so smaller tasks cannot satisfy the check as a combined total.
          selectTask("2 MC")
          doTask("2 MC")
        }
        .expect("-5 MC, 0 CorporationCard<Selecting>")
    shouldThrow<TaskException> { kim.doTask("2 MC") }
  }
}
