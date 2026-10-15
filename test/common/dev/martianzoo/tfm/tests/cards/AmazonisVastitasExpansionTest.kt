package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DomainException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
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
        doTask("ClaimMilestone<Class<Merchant3>>")
        doTask("8 Pay<> FROM MC")
        doTask("Merchant3")
      }
    }
    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "1! MC FROM ProjectCard")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Merchant3>>", "8 Pay<> FROM MC")
      doTask("Merchant3")
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
      doTasks("FundAward<Class<Manufacturer>>", "8 Pay<> FROM MC")
      doTask("Manufacturer")
    }

    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Amazonis delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "AmazonisMap")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "25 Pay<> FROM MC",
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
              "25 Pay<> FROM MC",
              "PROD[MC]",
              "CityTile<Amazonis_02_02>",
          )
          doTask("PartyLeader<Scientists>")
        }
        .expect("0 LobbyDelegate")
  }

  @Test
  internal fun `Olympus Mons sends both delegates to the chosen party`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "25 Pay<> FROM MC",
              "PROD[MC]",
              "CityTile<Amazonis_08_09>",
          )
          doTasks("PartyLeader<Scientists>", "PartyFollower<Scientists>")
        }
        .expect("PartyLeader<Scientists>, Dominant<Scientists>")
  }

  @Test
  internal fun `Olympus Mons cannot split its delegates between parties`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "UseStandardProject<CityProject>",
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "CityTile<Amazonis_08_09>",
            "PartyLeader<Scientists>",
            "PartyLeader<Unity>",
        )
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("PartyDelegate") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `Olympus Mons requires two available delegates`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    kim.exMachina("PartyLeader<Unity>, 5 PartyFollower<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTask("UseStandardProject<CityProject>")
        doTask("25 Pay<> FROM MC")
        doTask("PROD[MC]")
        doTask("CityTile<Amazonis_08_09>")
        doTasks("PartyFollower<MarsFirst>", "PartyLeader<MarsFirst>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `A Vastitas delegate bonus leaves the Lobby delegate available`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "25 Pay<> FROM MC",
              "PROD[MC]",
              "CityTile<Vastitas_4_8>",
          )
          doTask("PartyLeader<Greens>")
        }
        .expect("0 LobbyDelegate")
  }

  @Test
  internal fun `Vastitas delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "VastitasMap")

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_4_8>",
      )
    }
  }

  @Test
  internal fun `Vastitas delegate spaces require an available delegate`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")
    kim.exMachina("PartyLeader<Unity>, 6 PartyFollower<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTask("UseStandardProject<CityProject>")
        doTask("25 Pay<> FROM MC")
        doTask("PROD[MC]")
        doTask("CityTile<Vastitas_4_8>")
        doTask("PartyLeader<Greens>")
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
        doTask("ClaimMilestone<Class<Geologist>>")
        doTask("8 Pay<> FROM MC")
        doTask("Geologist")
      }
    }
    kim.exMachina("CommercialDistrict_SpecialTile<Vastitas_4_2>")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Geologist>>", "8 Pay<> FROM MC")
      doTask("Geologist")
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
      doTasks("FundAward<Class<Landscaper>>", "8 Pay<> FROM MC")
      doTask("Landscaper")
    }
  }

  @Test
  internal fun `The Vastitas north pole adds four MC to placement cost and raises temperature`() {
    newTestGame(addOptions = "VastitasMap")

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_5_5>",
      )
      doTask("TemperatureStep")
      doTasks("-4 MC", "TerraformRating")
    }
  }

  @Test
  internal fun `An unaffordable north pole payment rolls back the whole city project`() {
    newTestGame(addOptions = "VastitasMap")
    kim.setToExMachina(28, "MC")

    shouldThrow<DomainException> {
      kim.inTurn {
        doTask("UseStandardProject<CityProject>")
        doTask("25 Pay<> FROM MC")
        doTask("PROD[MC]")
        doTask("CityTile<Vastitas_5_5>")
        doTask("TemperatureStep")
        doTask("TerraformRating")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 28
    admin.count("TemperatureStep") shouldBe 0
  }
}
