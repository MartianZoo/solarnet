package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
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
    kim.turn {
      playPrelude(FakeHeadStart) {
        kim.assertCounts(2 to "Steel", 24 to "MC")

        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")

        kim.assertCounts(2 to "MC")
        kim.production(cn("Energy")) shouldBe 3
      }
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle", kimCorporation = ValleyTrust)

    kim.turn {
      playPrelude(FakeHeadStart) {
        kim.playPrelude(MartianIndustries) {
          useStdProject("PowerPlantProject")
        }
      }
    }
  }

  @Test
  internal fun `Fake Head Start allows money and steel after both actions without splitting steel`() {
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle")
    kim.setToExMachina(2, "ProjectCard")
    kim.autoExecPolicy = CONCRETE

    kim.playPrelude(FakeHeadStart) {
          shouldThrow<TaskException> { doTask("Steel") }
          doTask("UseStandardProject<SellPatentsProject>")
          doTask("MC FROM ProjectCard")
          doTask("UseStandardProject<SellPatentsProject>")
          doTask("MC FROM ProjectCard")
          doTask("2 MC / ProjectCard")
        }
        .expect("2 MC, 2 Steel, -2 ProjectCard")
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
    return kim.playPrelude(FakeHeadStart) {
      doTask("ConvertHeat")
      doTask("-8 Heat")
      doTask("UseStandardProject<AquiferProject>")
      doTask("-18 MC")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Suitable Infrastructure pays separately for Head Start's nested actions`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack, FakeStuffBundle")
    kim.exMachina("$SuitableInfrastructure")
    kim.setToExMachina(30, "MC")
    kim.setToExMachina(0, "ProjectCard")
    val startingMoney = kim.count("MC")

    kim.turn {
      playPrelude(FakeHeadStart) {
        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")
      }
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
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack, FakeStuffBundle, Unsafe")
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 8 Heat")
    kim.setToExMachina(54, "MC")
    kim.setToExMachina(0, "ProjectCard")

    kim.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("UseCardAction<$BoardOfDirectors, Action1>", payment = {}) {
          doTask("-12 MC")
          playPrelude(Merger) { playCorp(SagittaFrontierServices) }
        }
        useStdAction("ConvertHeat", payment = { doTask("-8 Heat") })
      }
    }
  }
}
