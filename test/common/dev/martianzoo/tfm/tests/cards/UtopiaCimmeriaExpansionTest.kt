package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class UtopiaCimmeriaExpansionTest : CardTest() {
  @Test
  internal fun `MSL Curiosity bonus is inert without Colonies`() {
    newGame(Cimmeria)
    p1.manual("10 MC")

    p1.manual("CityTile<Cimmeria_3_3>")

    p1.count("MC") shouldBe 10
    p1.count("Colony") shouldBe 0
  }

  @Test
  internal fun `MSL Curiosity bonus can buy a colony with Colonies`() {
    newGame(
        ColoniesExpansion,
        Cimmeria,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("10 MC")

    p1.manual("CityTile<Cimmeria_3_3>") { doTask("Colony<Luna>") }

    p1.count("MC") shouldBe 5
    p1.count("Colony<Luna>") shouldBe 1
  }

  @Test
  internal fun `Incorporator rewards inexpensive active and automated projects, not events or corporations`() {
    newGame(Utopia)
    val p2 = requireP2()
    p1.manual("8 MC, $Ecoline, $EarthCatapult, Asteroid")
    p2.manual("$Mine")
    engine.phase("Action")

    p1.stdAction("FundAward") { doTask("Incorporator") }
    engine.manual("End FROM Phase")

    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(25 to "VictoryPoint")
  }

  @Test
  internal fun `Suburbian rewards a tile on the map edge over an interior tile`() {
    newGame(Utopia)
    val p2 = requireP2()
    p1.manual("8 MC, CityTile<Utopia_1_1>")
    p2.manual("CityTile<Utopia_5_5>")
    engine.phase("Action")

    p1.stdAction("FundAward") { doTask("Suburbian") }
    engine.manual("End FROM Phase")

    p1.assertCounts(25 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }

  @Test
  internal fun `Founder counts a tile once when it neighbors multiple opponents' special tiles`() {
    newGame(Cimmeria)
    val p2 = requireP2()
    p1.manual("8 MC, CityTile<Cimmeria_3_3>")
    p2.manual("MiningRights_SpecialTile<Cimmeria_3_2>, NaturalPreserve_SpecialTile<Cimmeria_3_4>")
    engine.phase("Action")

    p1.stdAction("FundAward") { doTask("Founder") }
    engine.manual("End FROM Phase")

    p1.assertCounts(
        1 to "AwardTally<Player1, Founder>",
        1 to "FirstPlace<Player1, Founder>",
    )
    p2.assertCounts(0 to "AwardTally<Player2, Founder>")
  }

  @Test
  internal fun `Claims Metallurgist for combined metal production and Trader for three resource types`() {
    newGame(Utopia)
    p1.manual(
        "16 MC, PROD[2 Steel, 4 Titanium], $SearchForLife, Science<$SearchForLife>, " +
            "$Predators, Animal<$Predators>, $RegolithEaters, Microbe<$RegolithEaters>"
    )
    engine.phase("Action")

    p1.stdAction("ClaimMilestone") { doTask("Metallurgist") }.expect("-8 MC, Milestone")
    p1.stdAction("ClaimMilestone") { doTask("Trader") }.expect("-8 MC, Milestone")

    p1.assertCounts(2 to "Milestone")
  }

  @Test
  internal fun `Fundraiser requires printed mc production of twelve`() {
    newGame(Cimmeria)
    p1.manual("PROD[11 MC]")

    shouldThrow<RequirementException> { p1.manual("Fundraiser") }

    p1.manual("PROD[1 MC], Fundraiser")
    p1.count("Fundraiser") shouldBe 1
  }
}
