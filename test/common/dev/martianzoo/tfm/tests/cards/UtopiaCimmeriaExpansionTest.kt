package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class UtopiaCimmeriaExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `MSL Curiosity bonus is inert without Colonies`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.setToExMachina(35, "MC")

    kim.stdProject("CityProject") { placeTile(3, 3) }.expect("-25 MC, 0 Colony")
  }

  @Test
  internal fun `MSL Curiosity bonus can buy a colony with Colonies`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.setToExMachina(35, "MC")

    kim.stdProject("CityProject") {
          placeTile(3, 3)
          doTask("Colony<Luna>")
        }
        .expect("-30 MC, Colony<Luna>")
  }

  // https://boardgamegeek.com/thread/3242862/msl-curiosity-question
  @Test
  internal fun `MSL Curiosity cannot be covered when its owner has no legal colony`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>, Colony<Callisto>")

    shouldThrowAny { kim.stdProject("CityProject") { placeTile(3, 3) } }

    kim.count("CityTile<Cimmeria_3_3>") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `Incorporator rewards inexpensive active and automated projects, not events or corporations`() {
    newTestGame(addOptions = "UtopiaMap", playerCount = 2)
    kim.exMachina("$Ecoline, $EarthCatapult")
    stan.exMachina("$Mine")
    kim.playProject(SmallAsteroid, 8)

    kim.stdAction("FundAward<Class<Incorporator>>")
    victoryPoints() shouldBe listOf(23, 25)
  }

  @Test
  internal fun `Suburbian rewards a tile on the map edge over an interior tile`() {
    newTestGame(addOptions = "UtopiaMap", playerCount = 2)
    kim.exMachina("NormalCityTile<Utopia_1_1>")
    stan.exMachina("NormalCityTile<Utopia_5_5>")

    kim.stdAction("FundAward<Class<Suburbian>>")
    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Founder counts a tile once when it neighbors multiple opponents' special tiles`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.exMachina("NormalCityTile<Cimmeria_3_3>")
    stan.exMachina(
        "MiningRights_SpecialTile<Cimmeria_3_2>, NaturalPreserve_SpecialTile<Cimmeria_3_4>"
    )

    kim.stdAction("FundAward<Class<Founder>>")
    victoryPoints() shouldBe listOf(25, 20)
  }

  @Test
  internal fun `Metallurgist combines steel and titanium production`() {
    newTestGame(addOptions = "UtopiaMap")
    kim.setToExMachina(2, "PROD[Steel]")
    kim.setToExMachina(4, "PROD[Titanium]")

    kim.stdAction("ClaimMilestone<Class<Metallurgist>>").expect("-8 MC, Metallurgist")
  }

  @Test
  internal fun `Trader counts different card resource types`() {
    newTestGame(addOptions = "UtopiaMap")
    kim.exMachina(
        "$SearchForLife, Science<$SearchForLife>, $Predators, Animal<$Predators>, $RegolithEaters, Microbe<$RegolithEaters>"
    )

    kim.stdAction("ClaimMilestone<Class<Trader>>").expect("-8 MC, Trader")
  }

  @Test
  internal fun `Fundraiser requires printed mc production of twelve`() {
    newTestGame(addOptions = "CimmeriaMap, -ColoniesExpansion", playerCount = 2)
    kim.setToExMachina(11, "PROD[MC]")

    shouldThrow<RequirementException> {
      kim.stdAction("ClaimMilestone<Class<Fundraiser>>")
    }

    kim.setToExMachina(12, "PROD[MC]")
    kim.stdAction("ClaimMilestone<Class<Fundraiser>>").expect("Fundraiser")
  }
}
