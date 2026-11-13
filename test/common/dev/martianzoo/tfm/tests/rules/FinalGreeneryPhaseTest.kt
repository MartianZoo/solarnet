package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.MandatoryVenusVariant
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest : TfmGameplayTest(NONE) {
  // Omit starting production and expansion solar work; these scenarios isolate the base phase flow.
  private val options = "-QuickStartVariant, -ColoniesExpansion, -VenusNextExpansion"

  @Test
  internal fun normalGreeneryRaisesOxygen() {
    newTestGame(options, playerCount = 2)
    // Supply plants for the conversion; how they were collected is irrelevant here.
    kim.exMachina("8 Plant")
    kim.completeOperation {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_6>",
          "OxygenStep",
          "TerraformRating",
      )
    }
  }

  @Test
  internal fun finalGreeneryDoesNotRaiseOxygen() {
    newTestGame(options, playerCount = 2)
    // Supply plants for the final conversion without playing a resource-gathering history.
    kim.exMachina("8 Plant")
    // End with oxygen still low so that the final greenery's lack of an oxygen reward is
    // observable.
    admin.exMachina(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("20 MC")
    stan.doTask("20 MC")

    admin.count("FinalGreeneryPhase") shouldBe 1
    kim.completeOperation {
          doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Tharsis_3_5>",
          )
        }
        .expect("0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun soloLossSkipsFinalGreeneryAndScoring() {
    newTestGame(options, playerCount = 1)
    // Reach the final solo generation without filling the global tracks.
    admin.exMachina("-13 SoloGenerationsLeft")
    kim.doTask("Pass")
    kim.doTask("14 MC")

    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("End") shouldBe 0
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
    admin.count("TemperatureStep") shouldBe 0
    admin.count("OxygenStep") shouldBe 0
    admin.count("OceanTile") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.events.changesSinceSetup().last().change.removing?.type?.className shouldBe cn("GameScope")
  }

  @Test
  internal fun multiplayerDoesNotTreatAbsentCountdownAsGameEnd() {
    newTestGame(options, playerCount = 2)
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("20 MC")
    stan.doTask("20 MC")

    game.classTable.allClassNames.shouldNotContain(cn("SoloGenerationsLeft"))
    admin.count("ResearchPhase") shouldBe 1
    admin.count("FinalGreeneryPhase") shouldBe 0
  }

  @Test
  internal fun elevenPlantsCanBecomeTwoGreeneriesWithEcolineAndTheElysiumBonus() {
    newTestGame(
        "ElysiumMap, $options",
        playerCount = 2,
        kimCorporation = Ecoline,
    )
    // Ecoline starts with three plants and will produce two more: six additional plants make 11.
    kim.exMachina("6 Plant")
    // End this generation without completing the parameters; test the final-greenery conversions.
    admin.exMachina(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("20 MC", "2 Plant")
    stan.doTask("20 MC")

    kim.count("Plant") shouldBe 11
    kim.completeOperation {
      // 11 - 7 with Ecoline + the 3-plant placement bonus = 7.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_6>",
          "3 Plant",
      )
    }
    kim.count("Plant") shouldBe 7
    kim.completeOperation {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_5>",
          "2 Plant",
      )
    }
    kim.selectTask("PassBaton")
    kim.count("GreeneryTile") shouldBe 2
  }

  @Test
  internal fun tenPlantsCanBecomeTwoGreeneriesWithPhilaresNeighborsAndTheElysiumBonus() {
    newTestGame(
        "ElysiumMap, $options",
        playerCount = 2,
        kimCorporation = Philares,
    )
    // Establish the neighboring opponent tiles; their placement is not under test.
    stan.exMachina(
        "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, GreeneryTile<Elysium_6_6>"
    )
    // The required greenery's placement and adjacency will bring these eight plants to ten.
    kim.exMachina("8 Plant")
    // End this generation without completing the parameters; test the final-greenery conversions.
    admin.exMachina(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )
    kim.completeOperation {
      doTasks(
          "DoRequiredActions",
          "GreeneryTile<Elysium_4_5>",
          "OxygenStep",
          "TerraformRating",
      )
      // Bare Plant matches both rewards; select the fixed placement bonus before Philares' choice.
      selectTask("Plant!")
      doTasks("Plant", "Plant")
    }
    kim.selectTask("PassBaton")
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTask("21 MC")
    stan.doTask("20 MC")

    kim.count("Plant") shouldBe 10
    kim.count("GreeneryTile") shouldBe 1
    kim.completeOperation {
      // 10 - 8 + the 3-plant bonus + a Philares plant per opponent adjacency = 8.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_6>",
          "3 Plant",
      )
      doTasks("Plant", "Plant", "Plant")
    }
    kim.count("Plant") shouldBe 8
    kim.completeOperation {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_6_7>",
      )
      // Bare Plant matches both rewards; select the fixed placement bonus before Philares' choices.
      selectTask("Plant!")
      doTasks("Plant", "Plant", "Plant")
    }
    kim.selectTask("PassBaton")
    kim.count("GreeneryTile") shouldBe 3
  }

  // These checks exercise the end conditions directly, without running turns or phases.
  internal class EndConditions : TfmSandboxTest() {
    @Test
    internal fun multiplayerEndConditionIgnoresVenusCompletion() {
      game = setUpGame(VenusNextExpansion)
      admin.runOperation(
          "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
      )
      admin.count("GpComplete<Class<VenusStep>>") shouldBe 0
      admin.count("GameEndBarrier") shouldBe 0
    }

    @Test
    internal fun mandatoryVenusVariantKeepsItsOwnBarrierUntilVenusIsComplete() {
      game = setUpGame(VenusNextExpansion, MandatoryVenusVariant)
      admin.runOperation(
          "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
      )
      admin.count("GameEndBarrier") shouldBe 1
      admin.runOperation("GpComplete<Class<VenusStep>>")
      admin.count("GameEndBarrier") shouldBe 0
    }
  }
}
