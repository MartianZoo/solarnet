package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AmazonisVastitasExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `Merchant requires three MC remaining after its claim payment`() {
    newTestGame(addOptions = "AmazonisMap")
    kim.setToExMachina(10, "MC")
    kim.exMachina("3 Steel, 3 Titanium, 3 Plant, 3 Energy, 3 Heat")

    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Merchant3>>")
        selectTask("-X? MC")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "1! MC FROM ProjectCard")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Merchant3>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `Manufacturer adds steel and heat production when ranking players`() {
    newTestGame(addOptions = "AmazonisMap", playerCount = 2)
    kim.setToExMachina(3, "PROD[Steel]")
    kim.setToExMachina(2, "PROD[Heat]")
    stan.setToExMachina(0, "PROD[Steel]")
    stan.setToExMachina(4, "PROD[Heat]")
    kim.inTurn {
      doTasks("FundAward<Class<Manufacturer>>", "-8 MC")
      selectOnlyTask()
    }

    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Amazonis delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "AmazonisMap")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Amazonis_02_02>",
          )
        }
        .expect("0 ProjectCard, 0 Titanium")
  }

  @Test
  internal fun `An Amazonis delegate bonus leaves the Lobby delegate available`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Amazonis_02_02>",
          )
          selectOnlyTask()
          doTask("PartyDelegate<Scientists>")
        }
        .expect("0 LobbyActionAvailable")
  }

  @Test
  internal fun `Olympus Mons sends both delegates to the chosen party`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Amazonis_08_09>",
          )
          selectOnlyTask()
          doTask("2 PartyDelegate<Scientists>")
        }
        .expect("PartyLeader<Scientists>, Dominant<Scientists>")
  }

  @Test
  internal fun `Olympus Mons requires two available delegates`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    kim.exMachina("6 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<CityProject>")
        selectTask("-X? MC")
        doTask("-25 MC")
        selectTask("PROD[MC]")
        selectOnlyTask()
        doTask("CityTile<Amazonis_08_09>")
        selectOnlyTask()
        doTask("2 PartyDelegate<MarsFirst>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `A Vastitas delegate bonus leaves the Lobby delegate available`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")

    kim.inTurn {
          doTasks("UseStandardProject<CityProject>", "-25 MC", "PROD[MC]", "CityTile<Vastitas_4_8>")
          selectOnlyTask()
          doTask("PartyDelegate<Greens>")
        }
        .expect("0 LobbyActionAvailable")
  }

  @Test
  internal fun `Vastitas delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "VastitasMap")

    kim.inTurn {
      doTasks("UseStandardProject<CityProject>", "-25 MC", "PROD[MC]", "CityTile<Vastitas_4_8>")
    }
  }

  @Test
  internal fun `Vastitas delegate spaces require an available delegate`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")
    kim.exMachina("7 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<CityProject>")
        selectTask("-X? MC")
        doTask("-25 MC")
        selectTask("PROD[MC]")
        selectOnlyTask()
        doTask("CityTile<Vastitas_4_8>")
        selectOnlyTask()
        doTask("PartyDelegate<Greens>")
      }
    }
    kim.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Geologist counts owned tiles that have owned neighbors`() {
    newTestGame(addOptions = "VastitasMap")
    kim.exMachina("LavaFlows_SpecialTile<Vastitas_4_1>, RestrictedArea_SpecialTile<Vastitas_3_1>")

    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Geologist>>")
        selectTask("-X? MC")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
    kim.exMachina("CommercialDistrict_SpecialTile<Vastitas_4_2>")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Geologist>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `Landscaper counts the largest connected group instead of all owned tiles`() {
    newTestGame(addOptions = "VastitasMap")
    kim.exMachina(
        "LavaFlows_SpecialTile<Vastitas_4_1>, RestrictedArea_SpecialTile<Vastitas_3_1>, CommercialDistrict_SpecialTile<Vastitas_4_2>, NormalCityTile<Vastitas_8_7>"
    )

    kim.count("TileInLargestGroup") shouldBe 3
    kim.inTurn {
      doTasks("FundAward<Class<Landscaper>>", "-8 MC")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `The Vastitas north pole adds four MC to placement cost and raises temperature`() {
    newTestGame(addOptions = "VastitasMap")

    kim.inTurn {
      doTasks("UseStandardProject<CityProject>", "-25 MC", "PROD[MC]", "CityTile<Vastitas_5_5>")
      selectTask("TemperatureStep*")
      doTasks("-4 MC", "TerraformRating")
    }
  }

  @Test
  internal fun `An unaffordable north pole payment rolls back the whole city project`() {
    newTestGame(addOptions = "VastitasMap")
    kim.setToExMachina(28, "MC")

    shouldThrow<GameplayException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<CityProject>")
        selectTask("-X? MC")
        doTask("-25 MC")
        selectTask("PROD[MC]")
        selectOnlyTask()
        doTask("CityTile<Vastitas_5_5>")
        selectTask("TemperatureStep*")
        selectTask("TryTerraformRating!")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 28
    admin.count("TemperatureStep") shouldBe 0
  }
}
