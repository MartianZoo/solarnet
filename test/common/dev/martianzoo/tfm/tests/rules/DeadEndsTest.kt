package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.Tharsis
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.AerialMappers
import dev.martianzoo.tfm.tests.cards.cardnames.FloatingHabs
import dev.martianzoo.tfm.tests.cards.cardnames.L1TradeTerminal
import dev.martianzoo.tfm.tests.cards.cardnames.MonsInsurance
import dev.martianzoo.tfm.tests.cards.cardnames.OldMiningColony
import dev.martianzoo.tfm.tests.cards.cardnames.ProjectEden
import dev.martianzoo.tfm.tests.cards.cardnames.SmallAsteroid
import dev.martianzoo.tfm.tests.cards.cardnames.StrategicBasePlanning
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianInsects
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DeadEndsTest : CardTest() {
  @Test
  internal fun `Old Mining Colony must choose Pluto before its discard becomes due`() {
    newGame(
        ColoniesExpansion,
        Prelude2CardPack,
        colonyTiles = testColonyTiles(2, "Pluto", "Io"),
    )

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    try {
      p1.runOperation("$OldMiningColony") {
        hazardousChoiceAttempted = true
        doTask("Colony<Io>")
        doTasks("PROD[Titanium]", "PROD[Heat]")
        p1.count("Colony<Io>") shouldBe 1
        p1.count("ProjectCard") shouldBe 0
        shouldThrow<LimitsException> { doTask("-ProjectCard") }
        abort()
      }
    } catch (_: DeadEndException) {
      // A future pruning Agent may reject Colony<Io> before it commits.
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("$OldMiningColony") shouldBe 0
    p1.count("Colony<Io>") shouldBe 0

    p1.runOperation("$OldMiningColony") {
          doTask("Colony<Pluto>")
          autoExecNow(EAGER)
        }
        .expect("Colony<Pluto>, ProjectCard")
  }

  @Test
  internal fun `Amazonis resource choice must fund the Reds payment`() {
    newGame(Amazonis, TurmoilExpansion)
    admin.runOperation("Ruling<Reds> FROM Ruling")
    p1.runOperation("2 MC")
    admin.phase("Action")

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    try {
      p1.runOperation("GreeneryTile<Amazonis_05_03>") {
        hazardousChoiceAttempted = true
        doTask("Steel")
        doTasks("OxygenStep", "TerraformRating")
        p1.count("Steel") shouldBe 1
        p1.count("MC") shouldBe 2
        shouldThrow<LimitsException> { doTask("-3 MC<Player1>") }
        abort()
      }
    } catch (_: DeadEndException) {
      // A future pruning Agent may reject Steel before it commits.
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("GreeneryTile<Amazonis_05_03>") shouldBe 0
    p1.count("Steel") shouldBe 0

    p1.runOperation("GreeneryTile<Amazonis_05_03>") {
          doTask("MC")
          autoExecNow(EAGER)
        }
        .expect("GreeneryTile<Amazonis_05_03>, -2 MC")
  }

  @Test
  internal fun `Project Eden must reserve the sole city space before placing greenery`() {
    newGame(Tharsis, Prelude2CardPack)
    p1.runOperation("3 ProjectCard")
    val onlyCitySpace = "Tharsis_8_8"
    val otherOpenSpace = "Tharsis_3_3"
    val blockingCitySpace = "Tharsis_3_2"
    val filledLandSpaces =
        p1.list("LandArea")
            .map { it.className.toString() }
            .filterNot { it in setOf(onlyCitySpace, otherOpenSpace, blockingCitySpace) }
    p1.sneak(
        (listOf("NormalCityTile<$blockingCitySpace>") +
                filledLandSpaces.map { "GreeneryTile<$it>" })
            .joinToString()
    )

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    try {
      p1.runOperation("$ProjectEden") {
        hazardousChoiceAttempted = true
        doTask("GreeneryTile<$onlyCitySpace>")
        doTask("OceanTile<Tharsis_1_5>")
        p1.count("LandArea(HAS MAX 0 Tile)") shouldBe 1
        shouldThrow<NarrowingException> { doTask("CityTile<$otherOpenSpace>") }
        abort()
      }
    } catch (_: DeadEndException) {
      // A future pruning Agent may reject the greenery placement before it commits.
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("$ProjectEden") shouldBe 0
    p1.count("GreeneryTile<$onlyCitySpace>") shouldBe 0

    p1.runOperation("$ProjectEden") {
          doTasks(
              "CityTile<$onlyCitySpace>",
              "GreeneryTile<$otherOpenSpace>",
              "OceanTile<Tharsis_1_5>",
          )
          autoExecNow(EAGER)
        }
        .expect(
            "CityTile<$onlyCitySpace>, GreeneryTile<$otherOpenSpace>, " +
                "OceanTile<Tharsis_1_5>, -3 ProjectCard"
        )
  }

  @Test
  internal fun `Mons compensation must wait for the Small Asteroid Reds payment`() {
    newGame(PromoCardPack, TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("$MonsInsurance")
    p1.runOperation("-${p1.count("MC") - 3} MC")
    p2.runOperation("Plant")
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Action")

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    try {
      p1.runOperation("$SmallAsteroid") {
        doTask("-Plant<Player2>")
        hazardousChoiceAttempted = true
        doTasks(
            "3 MC<Player2> FROM MC<Player1>.",
            "TemperatureStep",
            "TerraformRating",
        )
        p1.count("MC") shouldBe 0
        shouldThrow<LimitsException> { doTask("-3 MC<Player1>") }
        abort()
      }
    } catch (_: DeadEndException) {
      // A future pruning Agent may reject the compensation before it commits.
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("$SmallAsteroid") shouldBe 0
    p1.count("MC") shouldBe 3
    p2.count("Plant") shouldBe 1

    p1.runOperation("$SmallAsteroid") {
          doTask("-Plant<Player2>")
          doTasks("TemperatureStep", "TerraformRating", "-3 MC<Player1>", "Ok")
          autoExecNow(EAGER)
        }
        .expect("-3 MC, -Plant<Player2>, 0 MC<Player2>")
  }

  @Test
  internal fun `Strategic Base Planning must avoid creating a second colony task`() {
    newGame(
        Cimmeria,
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.runOperation("8 MC, Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>")

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    try {
      p1.runOperation("$StrategicBasePlanning") {
        hazardousChoiceAttempted = true
        doTask("CityTile<Cimmeria_3_3>")
        doTasks(
            "CimmeriaPlacementBonus",
            "Colony<Callisto>",
            "-3 MC<Player1>",
            "-5 MC<Player1>",
        )
        p1.count("Colony<Callisto>") shouldBe 1
        p1.count("ColonyTile(HAS MAX 0 Colony)") shouldBe 0
        shouldThrow<DependencyException> { doTask("Colony<Callisto>") }
        abort()
      }
    } catch (_: DeadEndException) {
      // A future pruning Agent may reject the MSL Curiosity placement before it commits.
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("$StrategicBasePlanning") shouldBe 0
    p1.count("CityTile<Cimmeria_3_3>") shouldBe 0
    p1.count("Colony<Callisto>") shouldBe 0
    p1.count("MC") shouldBe 8

    p1.runOperation("$StrategicBasePlanning") {
          doTasks("CityTile<Cimmeria_3_4>", "Colony<Callisto>")
          autoExecNow(EAGER)
        }
        .expect("CityTile<Cimmeria_3_4>, Colony<Callisto>, -3 MC")
  }

  @Test
  internal fun `Cimmeria placement with no legal colony rolls back with either player policy`() {
    listOf(NONE, EAGER).forEach { policy ->
      newGame(
          Cimmeria,
          ColoniesExpansion,
          colonyTiles = testColonyTiles(2),
      )
      p1.runOperation(
          "5 MC, Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>, Colony<Callisto>"
      )
      p1.autoExecPolicy = policy

      shouldThrow<GameplayException> {
        p1.runOperation("CityTile<Cimmeria_3_3>") {
          if (policy == NONE) doTask("CimmeriaPlacementBonus")
          doTask("Colony<Luna>")
        }
      }

      p1.count("CityTile<Cimmeria_3_3>") shouldBe 0
      p1.count("Colony") shouldBe 5
      p1.count("MC") shouldBe 5
    }
  }

  @Test
  internal fun `L1 Trade Terminal must give to every eligible card`() {
    newGame(Prelude2CardPack, ColoniesExpansion, VenusNextExpansion)
    p1.runOperation(
        "$FloatingHabs, Floater<$FloatingHabs>, " +
            "$AerialMappers, Floater<$AerialMappers>, " +
            "$VenusianInsects, Microbe<$VenusianInsects>"
    )

    p1.autoExecPolicy = NONE
    var hazardousChoiceAttempted = false
    shouldThrow<DeadEndException> {
      p1.runOperation("$L1TradeTerminal") {
        doTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        addCardResources(FloatingHabs)
        addCardResources(VenusianInsects)
        hazardousChoiceAttempted = true
        declineTask("Floater<$AerialMappers>?")
        p1.tasks.isEmpty() shouldBe true
        p1.count("L1Gift") shouldBe 1
      }
    }
    hazardousChoiceAttempted shouldBe true
    p1.count("$L1TradeTerminal") shouldBe 0
    p1.count("L1Gift") shouldBe 0
    p1.count("Floater<$FloatingHabs>") shouldBe 1
    p1.count("Microbe<$VenusianInsects>") shouldBe 1
    p1.count("Floater<$AerialMappers>") shouldBe 1

    p1.runOperation("$L1TradeTerminal") {
          doTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
          addCardResources(FloatingHabs)
          addCardResources(VenusianInsects)
          addCardResources(AerialMappers)
          autoExecNow(EAGER)
        }
        .expect(
            "Floater<$FloatingHabs>, Microbe<$VenusianInsects>, " +
                "Floater<$AerialMappers>, 0 L1Gift"
        )
  }
}
