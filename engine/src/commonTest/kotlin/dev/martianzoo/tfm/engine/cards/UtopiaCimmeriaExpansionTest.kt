package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtopiaCimmeriaExpansionTest : CardTest() {
  @Test
  fun `MSL Curiosity bonus is inert without Colonies`() {
    newGame(TerraCimmeriaMapOption)
    p1.manual("10")

    p1.manual("CityTile<Cimmeria_3_3>")

    p1.count("Megacredit") shouldBe 10
    p1.count("Colony") shouldBe 0
  }

  @Test
  fun `MSL Curiosity bonus can buy a colony with Colonies`() {
    newGame(
        ColoniesExpansion,
        TerraCimmeriaMapOption,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("10")

    p1.manual("CityTile<Cimmeria_3_3>") {
      doTask("Colony<Luna>")
    }

    p1.count("Megacredit") shouldBe 5
    p1.count("Colony<Luna>") shouldBe 1
  }

  @Test
  fun `Incorporator rewards inexpensive active and automated projects, not events or corporations`() {
    newGame(UtopiaPlanitiaMapOption)
    val p2 = requireP2()
    p1.manual("8, $Ecoline, $EarthCatapult, Asteroid")
    p2.manual("$Mine")
    engine.phase("Action")

    p1.stdAction("FundAwardSA") { doTask("Incorporator") }
    engine.phase("End")

    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(25 to "VictoryPoint")
  }

  @Test
  fun `Suburbian rewards a tile on the map edge over an interior tile`() {
    newGame(UtopiaPlanitiaMapOption)
    val p2 = requireP2()
    p1.manual("8, CityTile<Utopia_1_1>")
    p2.manual("CityTile<Utopia_5_5>")
    engine.phase("Action")

    p1.stdAction("FundAwardSA") { doTask("Suburbian") }
    engine.phase("End")

    p1.assertCounts(25 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }

  @Test
  fun `Founder counts a tile once when it neighbors multiple opponents' special tiles`() {
    newGame(TerraCimmeriaMapOption)
    val p2 = requireP2()
    p1.manual("8, CityTile<Cimmeria_3_3>")
    p2.manual("MiningRights_SpecialTile<Cimmeria_3_2>, NaturalPreserve_SpecialTile<Cimmeria_3_4>")
    engine.phase("Action")

    p1.stdAction("FundAwardSA") { doTask("Founder") }
    engine.phase("End")

    p1.assertCounts(
        1 to "AwardTally<Player1, Founder>",
        1 to "FirstPlace<Player1, Founder>",
    )
    p2.assertCounts(0 to "AwardTally<Player2, Founder>")
  }

  @Test
  fun `Claims Metallurgist for combined metal production and Trader for three resource types`() {
    newGame(UtopiaPlanitiaMapOption)
    p1.manual(
        "16, PROD[2 Steel, 4 Titanium], $SearchForLife, Science<$SearchForLife>, " +
            "$Predators, Animal<$Predators>, $RegolithEaters, Microbe<$RegolithEaters>"
    )
    engine.phase("Action")

    p1.stdAction("ClaimMilestoneSA") { doTask("Metallurgist") }.expect("-8, Milestone")
    p1.stdAction("ClaimMilestoneSA") { doTask("Trader") }.expect("-8, Milestone")

    p1.assertCounts(2 to "Milestone")
  }

  @Test
  fun `Fundraiser requires printed megacredit production of twelve`() {
    newGame(TerraCimmeriaMapOption)
    p1.manual("PROD[11 Megacredit]")

    shouldThrow<RequirementException> { p1.manual("Fundraiser") }

    p1.manual("PROD[Megacredit], Fundraiser")
    p1.count("Fundraiser") shouldBe 1
  }
}
