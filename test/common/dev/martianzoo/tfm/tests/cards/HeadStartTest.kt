package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle")
    kim.setToExMachina(4, "MC")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
          "20 MC",
      )
      kim.assertCounts(2 to "Steel", 24 to "MC")

      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")

      kim.assertCounts(2 to "MC")
      kim.production(cn("Energy")) shouldBe 3
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle", kimCorporation = ValleyTrust)

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      selectTask("2 MC! / ProjectCard<Kim, Hand>")
      selectTask(
          "(MAX 0 RequiredAction: StandardAction!) OR (RequiredAction: RequiredActionsSignal!)"
      )
      selectTask("3 PreludeCard<Kim, Selecting>!")
      selectTask("-2 PreludeCard<Kim, Selecting>!")
      doTask("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
      selectTask("MAX 0 Barrier: MartianIndustries FROM PreludeCard<Kim, Selecting>!")
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
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle")
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
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle")
    kim.setToExMachina(4, "MC")
    kim.exMachina("10 Heat")
    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      selectTask("2 MC! / ProjectCard<Kim, Hand>")
      doTasks("ConvertHeat", "-8 Heat")
      selectTask("MAX 0 Billing: TemperatureStep.")
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
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards, FakeStuffBundle")
    kim.exMachina("$SuitableInfrastructure")
    kim.setToExMachina(30, "MC")
    kim.setToExMachina(0, "ProjectCard")
    val startingMoney = kim.count("MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      selectTask("2 MC! / ProjectCard<Kim, Hand>")
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      selectTask(
          "(SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: -SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>! THEN 2 MC<Kim>!) OR (MAX 0 SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: Ok)"
      )
      doTasks("2 MC", "UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
      selectTask(
          "(SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: -SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>! THEN 2 MC<Kim>!) OR (MAX 0 SuitableInfrastructureBonus<SuitableInfrastructure<Kim>>: Ok)"
      )
      doTask("2 MC")
    }

    kim.assertProds(3 to "Energy")
    kim.count("MC") shouldBe startingMoney - 18
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
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards, FakeStuffBundle, Unsafe")
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 8 Heat")
    kim.setToExMachina(54, "MC")
    kim.setToExMachina(0, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      selectTask("2 MC! / ProjectCard<Kim, Hand>")
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      selectTask("PreludeCard<Kim, Hand>!")
      doTask("-12 MC")
      selectTask("-Director<BoardOfDirectors>!")
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4 CorporationCard<Kim, Selecting>!")
      doTask("-42 MC")
      selectTask("-3 CorporationCard<Kim, Selecting>!")
      doTask("PlayCard<Class<CorporationCard>, Class<SagittaFrontierServices>>")
      selectTask("MAX 0 Barrier: SagittaFrontierServices FROM CorporationCard<Kim, Selecting>!")
      doTasks("31 MC", "PROD[Energy]", "PROD[2 MC]")
      selectTask("SearchForCard<Kim, NoTagsFilter>!")
      doTasks("4 MC", "ConvertHeat", "-8 Heat")
      selectTask("MAX 0 Billing: TemperatureStep.")
      doTask("TerraformRating")
    }
  }
}
