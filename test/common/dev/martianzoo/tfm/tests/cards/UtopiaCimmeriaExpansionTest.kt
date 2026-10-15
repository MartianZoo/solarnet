package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class UtopiaCimmeriaExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `MSL Curiosity bonus is inert without Colonies`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.setToExMachina(35, "MC")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "25 Pay<> FROM MC",
              "PROD[MC]",
              "CityTile<Cimmeria_3_3>",
          )
          doTask("CimmeriaPlacementBonus!")
        }
        .expect("0 Colony")
  }

  @Test
  internal fun `MSL Curiosity bonus can buy a colony with Colonies`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.setToExMachina(35, "MC")

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Cimmeria_3_3>",
      )
      doTask("CimmeriaPlacementBonus!")
      doTasks("-5 MC", "Colony<Luna>", "PROD[2 MC]")
    }
  }

  // https://boardgamegeek.com/thread/3242862/msl-curiosity-question
  @Test
  internal fun `MSL Curiosity cannot be covered when its owner has no legal colony`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>, Colony<Callisto>")

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        doTasks(
            "UseStandardProject<CityProject>",
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "CityTile<Cimmeria_3_3>",
        )
        doTask("CimmeriaPlacementBonus!")
        doTask("-5 MC")
      }
    }

    kim.count("CityTile<Cimmeria_3_3>") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `Incorporator rewards inexpensive active and automated projects, not events or corporations`() {
    newTestGame(addOptions = "UtopiaMap", playerCount = 2)
    kim.exMachina("$Ecoline, $EarthCatapult")
    stan.exMachina("$Mine")
    kim.inTurn {
      doTasks("PlayProject<Class<SmallAsteroid>>", "8 Pay<> FROM MC")
      doTasks(
          "SmallAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      // Resolve the optional attack: no unprotected plant target is available.
      selectTask("-2? Plant<Anyone>")
      doTask("TerraformRating")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }

    kim.inTurn {
      doTasks("FundAward<Class<Incorporator>>", "8 Pay<> FROM MC")
      doTask("Incorporator")
    }
    victoryPoints() shouldBe listOf(23, 25)
  }

  @Test
  internal fun `Suburbian rewards a tile on the map edge over an interior tile`() {
    newTestGame(addOptions = "UtopiaMap", playerCount = 2)
    kim.exMachina("NormalCityTile<Utopia_1_1>")
    stan.exMachina("NormalCityTile<Utopia_5_5>")

    kim.inTurn {
      doTasks("FundAward<Class<Suburbian>>", "8 Pay<> FROM MC")
      doTask("Suburbian")
    }
    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Founder counts a tile once when it neighbors multiple opponents' special tiles`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.exMachina("NormalCityTile<Cimmeria_3_3>")
    stan.exMachina(
        "MiningRights_SpecialTile<Cimmeria_3_2>, NaturalPreserve_SpecialTile<Cimmeria_3_4>"
    )

    kim.inTurn {
      doTasks("FundAward<Class<Founder>>", "8 Pay<> FROM MC")
      doTask("Founder")
    }
    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Metallurgist combines steel and titanium production`() {
    newTestGame(addOptions = "UtopiaMap")
    kim.setToExMachina(2, "PROD[Steel]")
    kim.setToExMachina(4, "PROD[Titanium]")

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Metallurgist>>", "8 Pay<> FROM MC")
      doTask("Metallurgist")
    }
  }

  @Test
  internal fun `Trader counts different card resource types`() {
    newTestGame(addOptions = "UtopiaMap")
    kim.exMachina(
        "$SearchForLife, Science<$SearchForLife>, $Predators, Animal<$Predators>, $RegolithEaters, Microbe<$RegolithEaters>"
    )

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Trader>>", "8 Pay<> FROM MC")
      doTask("Trader")
    }
  }

  @Test
  internal fun `Fundraiser requires printed mc production of twelve`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.setToExMachina(11, "PROD[MC]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Fundraiser>>")
        doTask("8 Pay<> FROM MC")
        doTask("Fundraiser")
      }
    }

    kim.setToExMachina(12, "PROD[MC]")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Fundraiser>>", "8 Pay<> FROM MC")
      doTask("Fundraiser")
    }
  }
}
