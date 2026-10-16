package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class HeadStartTest : TfmSandboxTest() {
  @Test
  internal fun `Head Start grants two mandatory actions`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards")
    kim.setToExMachina(4, "MC")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
          "20 MC",
      )

      shouldThrow<TaskException> { doTask("Pass") }
      doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
      shouldThrow<TaskException> { doTask("Pass") }
      doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newTestGame(
        addOptions = "PreludeExpansion, fakes:cards, ValleyTrust",
        kimCorporation = ValleyTrust,
    )

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("10 MC")
      shouldThrow<TaskException> { doTask("UseStandardProject<PowerPlantProject>") }
      doTask("DoRequiredActions")
      doTask("3 PreludeCard<Kim, Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
      doTask("MartianIndustries FROM PreludeCard<Selecting>")
      doTasks(
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
          "UseStandardProject<PowerPlantProject>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }
  }

  @Test
  internal fun `Head Start calculates its money before either granted action can sell cards`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards")
    kim.setToExMachina(2, "ProjectCard")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
              "FakeHeadStart FROM PreludeCard",
          )
          shouldThrow<TaskException> { doTask("Steel") }
          shouldThrow<TaskException> { doTask("UseStandardProject<SellPatentsProject>") }
          doTask("2 Steel")
          shouldThrow<TaskException> { doTask("UseStandardProject<SellPatentsProject>") }
          doTask("4 MC")
          doTasks(
              "UseStandardProject<SellPatentsProject>",
              "MC FROM ProjectCard",
              "UseStandardProject<SellPatentsProject>",
              "MC FROM ProjectCard",
          )
        }
        .expect("6 MC")
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Test
  internal fun `Cannot begin its second action before completing the first`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards")
    kim.setToExMachina(4, "MC")
    kim.exMachina("10 Heat")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("20 MC")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat")
      doTask("TemperatureStep")
      shouldThrow<TaskException> { doTask("UseStandardProject<AquiferProject>") }
      kim.assertCounts(24 to "MC", 2 to "Heat", 20 to "TerraformRating")
      doTasks(
          "TerraformRating",
          "UseStandardProject<AquiferProject>",
          "18 Pay<> FROM MC",
          "OceanTile<Tharsis_5_5>",
          "TerraformRating",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Suitable Infrastructure pays separately for Head Start's sequential actions`() {
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards, fakes:cards")
    kim.exMachina("$SuitableInfrastructure")
    kim.setToExMachina(30, "MC")
    kim.setToExMachina(0, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("0 MC")
      doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
      // The production reaction must hand its bonus removal to Admin before paying Kim.
      selectOnlyTask()
      doTasks(
          "2 MC",
          "UseStandardProject<PowerPlantProject>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
      // The next action has a fresh bonus with the same Admin handoff.
      selectOnlyTask()
      doTask("2 MC")
    }

    kim.assertProds(3 to "Energy")
    kim.count("MC") shouldBe 12
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Head Start's nested action`() {
    acquireDuringHeadStart()
    kim.count("MC") shouldBe 39
  }

  @Test
  internal fun `BUG - Misses Merger during Head Start's nested action`() {
    acquireDuringHeadStart()
    kim.count("MC") shouldBe 35
  }

  private fun acquireDuringHeadStart() {
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards, fakes:cards, Unsafe")
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 8 Heat")
    kim.setToExMachina(54, "MC")
    kim.setToExMachina(0, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("0 MC")
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("-42 MC")
      doTask("PlayCard<Class<CorporationCard>, Class<SagittaFrontierServices>>")
      doTask("SagittaFrontierServices FROM CorporationCard<Selecting>")
      doTasks("31 MC", "PROD[Energy]", "PROD[2 MC]")
      doTask("SearchForCard<Kim, NoTagsFilter>!")
      doTasks("4 MC", "ConvertHeat", "8 Pay<Class<Heat>> FROM Heat")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Both granted actions finish before resuming the Prelude sequence`() {
      newTestGame("PreludeExpansion, fakes:cards", playerCount = 2)
      kim.completeOperation {
            doTask("PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>")
            autoExecNow(EAGER)
          }
          .expect("2 Steel, 20 MC")

      shouldThrow<NarrowingException> {
        kim.doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      }
      kim.doTask("UseStandardProject<PowerPlantProject>").expect("PROD[Energy], -11 MC")
      kim.count("Baton") shouldBe 1
      stan.count("Baton") shouldBe 0
      shouldThrow<NarrowingException> {
        kim.doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      }
      kim.doTask("UseStandardProject<PowerPlantProject>").expect("PROD[Energy], -11 MC")
      kim.count("Baton") shouldBe 1
      stan.count("Baton") shouldBe 0

      kim.doTask("PlayCard<Class<PreludeCard>, Class<Donation>>").expect("21 MC")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<PowerGeneration>>").expect("PROD[3 Energy]")
    }

    @Test
    internal fun `Head Start during the first ordinary action preserves the second action`() {
      prepareBoardOfDirectors()
      playHeadStartThroughBoard()
      shouldThrow<NarrowingException> { kim.doTask("Pass") }

      repeat(2) {
        kim.doTask("UseStandardProject<PowerPlantProject>").expect("PROD[Energy], -11 MC")
        kim.count("Baton") shouldBe 1
        stan.count("Baton") shouldBe 0
      }

      // Both mandatory actions finished; Kim may now decline the ordinary second action.
      shouldThrow<NarrowingException> { kim.doTask("Pass") }
      kim.selectTask("-Baton")
      stan.doTask("Pass")
    }

    @Test
    internal fun `Head Start during the second ordinary action releases the baton after its grants`() {
      prepareBoardOfDirectors()
      kim.doTask("UseStandardProject<PowerPlantProject>").expect("PROD[Energy], -11 MC")
      playHeadStartThroughBoard()

      repeat(2) {
        kim.doTask("UseStandardProject<PowerPlantProject>").expect("PROD[Energy], -11 MC")
      }
      kim.count("Baton") shouldBe 0
      stan.count("Baton") shouldBe 1
      stan.doTask("Pass")
    }

    private fun prepareBoardOfDirectors() {
      newTestGame("PreludeExpansion, BoardOfDirectors, fakes:cards", playerCount = 2)
      kim.doTask("PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>")
      kim.doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<PowerGeneration>>")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<AlliedBank>>")
      stan.autoExecNow(EAGER)
    }

    private fun playHeadStartThroughBoard() {
      kim.completeOperation {
            doTask("UseCardAction<BoardOfDirectors, Action1>")
            doTask("PreludeCard")
            doTask("-12 MC")
            doTask("PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>")
            autoExecNow(EAGER)
          }
          .expect("2 Steel, 8 MC, -Director<BoardOfDirectors>")
    }
  }
}
