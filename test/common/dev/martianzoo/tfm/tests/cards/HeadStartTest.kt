package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
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

      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
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
      doTask("RequiredActionsSignal")
      doTask("3 PreludeCard<Kim, Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
      doTask("MartianIndustries FROM PreludeCard<Selecting>")
      doTasks(
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
          "UseStandardProject<PowerPlantProject>",
          "-11 MC",
          "PROD[Energy]",
      )
    }
  }

  @Test
  internal fun `Fake Head Start allows money and steel after both actions without splitting steel`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards")
    kim.setToExMachina(2, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
      )
      shouldThrow<TaskException> { doTask("Steel") }
      doTasks(
          "UseStandardProject<SellPatentsProject>",
          "MC FROM ProjectCard",
          "UseStandardProject<SellPatentsProject>",
          "MC FROM ProjectCard",
      )
      doTasks("2 MC / ProjectCard", "2 Steel")
    }
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Fake Head Start offers both actions independently.
  @Test
  internal fun `Cannot begin its second action before completing the first`() {
    shouldThrow<TaskException> { interleaveHeatAndAquifer() }
    kim.assertCounts(4 to "MC", 10 to "Heat", 2 to "PreludeCard", 0 to "$FakeHeadStart")
  }

  @Test
  internal fun `BUG - Can interleave its two actions`() {
    interleaveHeatAndAquifer().expect("TemperatureStep, OceanTile, 2 TerraformRating")
  }

  private fun interleaveHeatAndAquifer(): TaskResult {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards")
    kim.setToExMachina(4, "MC")
    kim.exMachina("10 Heat")
    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("20 MC")
      doTasks("ConvertHeat", "-8 Heat")
      doTask("TemperatureStep")
      doTasks(
          "TerraformRating",
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<Tharsis_5_5>",
          "TerraformRating",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Suitable Infrastructure pays separately for Head Start's nested actions`() {
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
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      // Resolve the bonus marker through Admin before collecting the standard-project refund.
      selectTask(
          "(SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: -SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>! THEN 2! MC<Kim>) OR (MAX 0 SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: Ok)"
      )
      doTasks("2 MC", "UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      // Resolve the bonus marker through Admin before collecting the standard-project refund.
      selectTask(
          "(SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: -SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>! THEN 2! MC<Kim>) OR (MAX 0 SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: Ok)"
      )
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
      doTasks("4 MC", "ConvertHeat", "-8 Heat")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
  }
}
