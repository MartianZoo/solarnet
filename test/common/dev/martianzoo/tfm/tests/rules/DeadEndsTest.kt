package dev.martianzoo.tfm.tests.rules

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
      kim.inTurn {
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<OldMiningColony>>",
            "OldMiningColony FROM PreludeCard",
        )
        selectTask("PROD[Titanium]")
        selectTask("Colony<Kim, ColonyTile(HAS MAX 0 Colony)>!")
        doTask("Colony<Io>")
        selectTask("PROD[Heat]")
        selectTask("-ProjectCard<Kim, Hand>!")
      }
    }
    kim.count("$OldMiningColony") shouldBe 0
    kim.count("Colony") shouldBe 0
    kim.count("PreludeCard") shouldBe 2

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<OldMiningColony>>",
              "OldMiningColony FROM PreludeCard",
              "PROD[Titanium]",
              "Colony<Pluto>",
          )
          selectTask("2! ProjectCard<Kim, Hand>")
          selectTask("-ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Amazonis placement can fund Reds but choosing steel leaves the action unpaid`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    // The correction changes government after ActionPhase began, so include its policy.
    kim.exMachina("Ruling<Reds FROM Greens>, RedsPolicy, 8 Plant")
    kim.setToExMachina(2, "MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("ConvertPlants")
        selectTask("-X? Plant")
        doTask("-Plant / Owed<Class<Plant>>")
        selectOnlyTask()
        selectOnlyTask()
        doTasks("GreeneryTile<Amazonis_05_03>", "Steel", "OxygenStep", "TerraformRating", "-3 MC")
      }
    }
    kim.count("GreeneryTile") shouldBe 0
    kim.count("Plant") shouldBe 8
    kim.count("MC") shouldBe 2
    kim.count("Steel") shouldBe 0

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTasks("GreeneryTile<Amazonis_05_03>", "MC")
          selectTask("OxygenStep*")
          doTasks("TerraformRating", "-3 MC")
        }
        .expect("-8 Plant")
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
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<ProjectEden>>", "ProjectEden FROM PreludeCard")
        selectTask("-3! ProjectCard<Kim, Hand>")
        doTask("GreeneryTile<Tharsis_8_8>")
        selectTask("OxygenStep*")
        selectTask("TerraformRating!")
        doTask("CityTile<Tharsis_3_3>")
      }
    }
    kim.count("$ProjectEden") shouldBe 0
    kim.count("GreeneryTile<$city>") shouldBe 0
    kim.count("ProjectCard") shouldBe 3

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<ProjectEden>>",
              "ProjectEden FROM PreludeCard",
          )
          selectTask("-3! ProjectCard<Kim, Hand>")
          doTasks("CityTile<Tharsis_8_8>", "GreeneryTile<Tharsis_3_3>")
          selectTask("OxygenStep*")
          doTasks("TerraformRating", "OceanTile<Tharsis_1_5>", "TerraformRating")
        }
        .expect("-3 ProjectCard")
  }

  @Test
  internal fun `Mons compensation must wait until Small Asteroid pays Reds`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MonsInsurance")
    // The correction changes government after ActionPhase began, so include its policy.
    kim.exMachina("Ruling<Reds FROM Greens>, RedsPolicy")
    kim.setToExMachina(13, "MC")
    stan.exMachina("Plant")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<SmallAsteroid>>", "-10 MC")
        doTasks(
            "SmallAsteroid FROM ProjectCard",
            "-Plant<Stan>",
            "3* MC<Stan FROM Kim>",
            "TemperatureStep",
            "TerraformRating",
            "-3 MC",
        )
      }
    }
    kim.count("MC") shouldBe 13
    kim.count("PlayedEvent<Class<$SmallAsteroid>>") shouldBe 0
    stan.count("Plant") shouldBe 1

    kim.inTurn {
          doTasks("PlayProject<Class<SmallAsteroid>>", "-10 MC")
          doTasks(
              "SmallAsteroid FROM ProjectCard",
              "-Plant<Stan>",
              "TemperatureStep",
              "TerraformRating",
              "-3 MC",
              "0 MC<Stan FROM Kim>",
          )
          doTask("PlayedEvent<Class<$SmallAsteroid>> FROM $SmallAsteroid")
        }
        .expect("0 MC<Stan>")
  }

  @Test
  internal fun `Strategic Base Planning must avoid a city bonus requiring a second colony`() {
    newTestGame(
        addOptions = "PreludeExpansion, CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>")
    kim.setToExMachina(8, "MC")
    shouldThrow<DependencyException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<StrategicBasePlanning>>",
            "StrategicBasePlanning FROM PreludeCard",
        )
        selectTask("-3! MC")
        doTask("CityTile<Cimmeria_3_3>")
        selectTask("CimmeriaPlacementBonus!")
        selectTask("-5! MC")
        doTask("Colony<Callisto>")
        selectTask("PROD[Energy]")
        selectOnlyTask()
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("Colony") shouldBe 4
    kim.count("MC") shouldBe 8

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<StrategicBasePlanning>>",
          "StrategicBasePlanning FROM PreludeCard",
          "-3 MC",
          "CityTile<Cimmeria_3_4>",
          "Colony<Callisto>",
          "PROD[Energy]",
      )
    }
  }

  @Test
  internal fun `Cimmeria city with no legal colony rolls back`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>, Colony<Callisto>")
    kim.setToExMachina(30, "MC")
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("UseStandardProject<CityProject>", "-25 MC")
        doTasks("CityTile<Cimmeria_3_3>", "CimmeriaPlacementBonus", "Colony<Luna>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("Colony") shouldBe 5
    kim.count("MC") shouldBe 30
  }
}
