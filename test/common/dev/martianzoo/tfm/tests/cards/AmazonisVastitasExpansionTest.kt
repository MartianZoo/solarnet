package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Merchant3")) }
    kim.sellPatents(1)
    kim.claimMilestone(cn("Merchant3")).expect("-8 MC, Merchant3")
  }

  @Test
  internal fun `Manufacturer adds steel and heat production when ranking players`() {
    newTestGame(addOptions = "AmazonisMap", playerCount = 2)
    kim.setToExMachina(3, "PROD[Steel]")
    kim.setToExMachina(2, "PROD[Heat]")
    stan.setToExMachina(0, "PROD[Steel]")
    stan.setToExMachina(4, "PROD[Heat]")
    kim.fundAward(cn("Manufacturer"), 8)

    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Amazonis delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "AmazonisMap")

    kim.stdProject("CityProject") { placeTile(2, 2) }.expect("0 ProjectCard, 0 Titanium")
  }

  @Test
  internal fun `An Amazonis delegate bonus leaves the Lobby delegate available`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    kim.stdProject("CityProject") {
          placeTile(2, 2)
          doTask("PartyDelegate<Scientists>")
        }
        .expect("PartyDelegate<Scientists>, 0 LobbyActionAvailable")
  }

  @Test
  internal fun `Olympus Mons sends both delegates to the chosen party`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")

    kim.stdProject("CityProject") {
          placeTile(8, 9)
          doTask("2 PartyDelegate<Scientists>")
        }
        .expect("2 PartyDelegate<Scientists>, PartyLeader<Scientists>, Dominant<Scientists>")
  }

  @Test
  internal fun `Olympus Mons requires two available delegates`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    kim.exMachina("6 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.stdProject("CityProject") {
        placeTile(8, 9)
        doTask("2 PartyDelegate<MarsFirst>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `A Vastitas delegate bonus leaves the Lobby delegate available`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")

    kim.stdProject("CityProject") {
          placeTile(4, 8)
          doTask("PartyDelegate<Greens>")
        }
        .expect("PartyDelegate<Greens>, 0 LobbyActionAvailable")
  }

  @Test
  internal fun `Vastitas delegate bonuses are inert without Turmoil`() {
    newTestGame(addOptions = "VastitasMap")

    kim.stdProject("CityProject") { placeTile(4, 8) }.expect("CityTile")
  }

  @Test
  internal fun `Vastitas delegate spaces require an available delegate`() {
    newTestGame(addOptions = "VastitasMap, TurmoilExpansion")
    kim.exMachina("7 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.stdProject("CityProject") {
        placeTile(4, 8)
        doTask("PartyDelegate<Greens>")
      }
    }
    kim.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Geologist counts owned tiles that have owned neighbors`() {
    newTestGame(addOptions = "VastitasMap")
    kim.exMachina("LavaFlows_SpecialTile<Vastitas_4_1>, RestrictedArea_SpecialTile<Vastitas_3_1>")

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Geologist")) }
    kim.exMachina("CommercialDistrict_SpecialTile<Vastitas_4_2>")
    kim.claimMilestone(cn("Geologist")).expect("Geologist")
  }

  @Test
  internal fun `Landscaper counts the largest connected group instead of all owned tiles`() {
    newTestGame(addOptions = "VastitasMap")
    kim.exMachina(
        "LavaFlows_SpecialTile<Vastitas_4_1>, RestrictedArea_SpecialTile<Vastitas_3_1>, CommercialDistrict_SpecialTile<Vastitas_4_2>, NormalCityTile<Vastitas_8_7>"
    )

    kim.count("TileInLargestGroup") shouldBe 3
    kim.fundAward(cn("Landscaper"), 8).expect("Landscaper")
  }

  @Test
  internal fun `The Vastitas north pole adds four MC to placement cost and raises temperature`() {
    newTestGame(addOptions = "VastitasMap")

    kim.stdProject("CityProject") { placeTile(5, 5) }.expect("-29 MC, TemperatureStep")
  }

  @Test
  internal fun `An unaffordable north pole payment rolls back the whole city project`() {
    newTestGame(addOptions = "VastitasMap")
    kim.setToExMachina(28, "MC")

    shouldThrow<GameplayException> {
      kim.stdProject("CityProject") { placeTile(5, 5) }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 28
    admin.count("TemperatureStep") shouldBe 0
  }
}
