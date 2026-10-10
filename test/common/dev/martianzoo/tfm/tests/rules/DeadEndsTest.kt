package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DeadEndsTest : TfmSandboxTest() {
  @Test
  internal fun `Old Mining Colony needs Pluto's cards when the hand is empty`() {
    newTestGame(addOptions = "PreludeExpansion, OldMiningColony, Pluto, Io, Luna, Ceres, Triton")
    kim.setToExMachina(0, "ProjectCard")
    shouldThrow<LimitsException> {
      kim.playPrelude(OldMiningColony) { doTask("Colony<Io>") }
    }
    kim.count("$OldMiningColony") shouldBe 0
    kim.count("Colony") shouldBe 0
    kim.count("PreludeCard") shouldBe 2

    kim.playPrelude(OldMiningColony) { doTask("Colony<Pluto>") }
        .expect("Colony<Pluto>, ProjectCard, PROD[Titanium]")
  }

  @Test
  internal fun `Amazonis placement can fund Reds but choosing steel leaves the action unpaid`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    kim.exMachina("Ruling<Reds> FROM Ruling<Greens>, 8 Plant")
    kim.setToExMachina(2, "MC")
    shouldThrow<LimitsException> {
      kim.convertPlants {
        doWithoutAutoExec(kim) {
          placeTile(5, 3)
          doTask("Steel")
          doTasks("OxygenStep", "TerraformRating", "-3 MC<Kim>")
        }
      }
    }
    kim.count("GreeneryTile") shouldBe 0
    kim.count("Plant") shouldBe 8
    kim.count("MC") shouldBe 2
    kim.count("Steel") shouldBe 0

    kim.convertPlants {
          doWithoutAutoExec(kim) {
            placeTile(5, 3)
            doTask("MC")
          }
        }
        .expect("GreeneryTile, -8 Plant, -2 MC, TerraformRating")
  }

  @Test
  internal fun `Project Eden must reserve the only legal city space`() {
    newTestGame(addOptions = "PreludeExpansion, ProjectEden")
    val city = "Tharsis_8_8"
    val greenery = "Tharsis_3_3"
    val blockingCity = "Tharsis_3_2"
    val filled =
        kim.list("LandArea")
            .map { it.className.toString() }
            .filterNot { it in setOf(city, greenery, blockingCity) }
    kim.exMachina(
        (listOf("NormalCityTile<$blockingCity>") + filled.map { "GreeneryTile<$it>" })
            .joinToString()
    )
    kim.setToExMachina(3, "ProjectCard")

    shouldThrow<NarrowingException> {
      kim.playPrelude(ProjectEden) {
        doTask("GreeneryTile<$city>")
        doTask("CityTile<$greenery>")
      }
    }
    kim.count("$ProjectEden") shouldBe 0
    kim.count("GreeneryTile<$city>") shouldBe 0
    kim.count("ProjectCard") shouldBe 3

    kim.playPrelude(ProjectEden) {
          doTasks("CityTile<$city>", "GreeneryTile<$greenery>", "OceanTile<Tharsis_1_5>")
        }
        .expect("CityTile<$city>, GreeneryTile<$greenery>, OceanTile, -3 ProjectCard")
  }

  @Test
  internal fun `Mons compensation must wait until Small Asteroid pays Reds`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MonsInsurance")
    kim.exMachina("Ruling<Reds> FROM Ruling<Greens>")
    kim.setToExMachina(13, "MC")
    stan.exMachina("Plant")
    kim.autoExecPolicy = AutoExecPolicy.NONE
    shouldThrow<LimitsException> {
      kim.playProject(SmallAsteroid, 10) {
        doWithoutAutoExec(kim) {
          doTask("$SmallAsteroid FROM ProjectCard")
          doTask("-Plant<Stan>")
          doTasks("3 MC<Stan> FROM MC<Kim>.", "TemperatureStep", "TerraformRating", "-3 MC<Kim>")
        }
      }
    }
    kim.count("MC") shouldBe 13
    kim.count("PlayedEvent<Class<$SmallAsteroid>>") shouldBe 0
    stan.count("Plant") shouldBe 1

    kim.playProject(SmallAsteroid, 10) {
          doWithoutAutoExec(kim) {
            doTask("$SmallAsteroid FROM ProjectCard")
            doTask("-Plant<Stan>")
            doTasks("TemperatureStep", "TerraformRating", "-3 MC<Kim>", "Ok")
            autoExecNow(AutoExecPolicy.EAGER)
          }
        }
        .expect("-13 MC, -Plant<Stan>, 0 MC<Stan>, TerraformRating")
  }

  @Test
  internal fun `Strategic Base Planning must avoid a city bonus requiring a second colony`() {
    newTestGame(
        addOptions = "PreludeExpansion, CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>")
    kim.setToExMachina(8, "MC")
    shouldThrow<DependencyException> {
      kim.playPrelude(StrategicBasePlanning) {
        doTask("CityTile<Cimmeria_3_3>")
        doTask("Colony<Callisto>")
        doTask("Colony<Callisto>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("Colony") shouldBe 4
    kim.count("MC") shouldBe 8

    kim.playPrelude(StrategicBasePlanning) {
          doTasks("CityTile<Cimmeria_3_4>", "Colony<Callisto>")
        }
        .expect("CityTile<Cimmeria_3_4>, Colony<Callisto>, -3 MC")
  }

  @Test
  internal fun `Cimmeria city with no legal colony rolls back under eager execution`() =
      rejectCimmeriaCity(AutoExecPolicy.EAGER)

  @Test
  internal fun `Cimmeria city with no legal colony rolls back under manual execution`() =
      rejectCimmeriaCity(AutoExecPolicy.NONE)

  private fun rejectCimmeriaCity(policy: AutoExecPolicy) {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>, Colony<Callisto>")
    kim.setToExMachina(30, "MC")
    kim.autoExecPolicy = policy
    shouldThrow<DependencyException> {
      kim.stdProject("CityProject") {
        doTask("CityTile<Cimmeria_3_3>")
        if (policy == AutoExecPolicy.NONE) doTask("CimmeriaPlacementBonus")
        doTask("Colony<Luna>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("Colony") shouldBe 5
    kim.count("MC") shouldBe 30
  }
}
