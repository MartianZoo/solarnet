package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class HeadStartTest : CardTest() {
  @Test
  internal fun `Head Start grants two mandatory actions`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    admin.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard")
    p1.turn {
      playPrelude(FakeHeadStart) {
        p1.assertCounts(2 to "Steel", 24 to "MC")

        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")

        p1.assertCounts(2 to "MC")
        p1.production(cn("Energy")) shouldBe 2
      }
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.runOperation("10 ProjectCard, PreludeCard")

    p1.turn {
      playPrelude(FakeHeadStart) {
        p1.playPrelude(MartianIndustries) {
          useStdProject("PowerPlantProject")
        }
      }
    }
  }

  @Test
  internal fun `Fake Head Start allows money and steel after both actions without splitting steel`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    admin.phase("Prelude")
    p1.runOperation("2 ProjectCard")
    p1.autoExecPolicy = CONCRETE

    p1.playPrelude(FakeHeadStart) {
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
    p1.assertCounts(4 to "MC", 10 to "Heat", 1 to "PreludeCard", 0 to "$FakeHeadStart")
  }

  @Test
  internal fun `BUG - Can interleave its two actions`() {
    interleaveHeatAndAquifer().expect("TemperatureStep, OceanTile, 2 TerraformRating")
  }

  private fun interleaveHeatAndAquifer(): TaskResult {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard, 10 Heat")
    return p1.playPrelude(FakeHeadStart) {
      doTask("ConvertHeat")
      doTask("-8 Heat")
      doTask("UseStandardProject<AquiferProject>")
      doTask("-18 MC")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Suitable Infrastructure pays separately for Head Start's nested actions`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.runOperation("$SuitableInfrastructure")
    admin.phase("Prelude")
    p1.runOperation("30 MC, PreludeCard")
    val startingMoney = p1.count("MC")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")
      }
    }

    p1.assertProds(2 to "Energy")
    p1.count("MC") shouldBe startingMoney - 18
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Head Start's nested action`() {
    acquireDuringHeadStart()
    p1.count("MC") shouldBe 39
  }

  @Test
  internal fun `BUG - Misses Merger during Head Start's nested action`() {
    acquireDuringHeadStart()
    p1.count("MC") shouldBe 35
  }

  private fun acquireDuringHeadStart() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle, Unsafe)
    p1.runOperation("$BoardOfDirectors, 54 MC, 8 Heat")
    admin.phase("Prelude")
    p1.runOperation("2 PreludeCard")

    p1.turn {
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
