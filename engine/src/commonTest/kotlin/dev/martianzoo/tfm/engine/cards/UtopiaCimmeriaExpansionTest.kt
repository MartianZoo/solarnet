package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtopiaCimmeriaExpansionTest : CardTest() {
  @Test
  fun `MSL Curiosity bonus is inert without Colonies`() {
    newGame(TerraCimmeriaMapOption)
    p1.manual("10")

    p1.manual("CityTile<TerraCimmeria_3_3>")

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

    p1.manual("CityTile<TerraCimmeria_3_3>") {
      doTask("Colony<Luna>")
    }

    p1.count("Megacredit") shouldBe 5
    p1.count("Colony<Luna>") shouldBe 1
  }

  @Test
  fun `Incorporator counts inexpensive active and automated projects only`() {
    newGame(UtopiaPlanitiaMapOption, PreludeExpansion)
    p1.manual("Ecoline, Donation, SearchForLife, Mine, PlayedEvent<Class<InventionContest>>")

    p1.count("CardFront") shouldBe 4
    p1.count("PlayedEvent") shouldBe 1
    p1.count("ActiveCard(HAS MAX 10 CardCost) OR AutomatedCard(HAS MAX 10 CardCost)") shouldBe 2
  }

  @Test
  fun `Suburbian counts tiles not fully surrounded by neighboring Mars areas`() {
    newGame(UtopiaPlanitiaMapOption)
    p1.manual("CityTile<UtopiaPlanitia_5_5>")
    val otherPlayer = requireP2()
    otherPlayer.manual("CityTile<UtopiaPlanitia_1_1>")
    p1.manual("Suburbian, TallyAward<Suburbian>")
    otherPlayer.manual("TallyAward<Suburbian>")

    p1.count("AwardTally<Player1, Suburbian>") shouldBe 0
    otherPlayer.count("AwardTally<Player2, Suburbian>") shouldBe 1
  }

  @Test
  fun `Founder counts an owned tile once when it neighbors multiple special tiles`() {
    newGame(TerraCimmeriaMapOption)
    p1.manual(
        "CityTile<TerraCimmeria_3_3>, MiningRightsTile<TerraCimmeria_3_2>, " +
            "NpTile<TerraCimmeria_3_4>"
    )

    p1.count("OwnedTile<MarsArea(HAS Neighbor<SpecialTile>)>") shouldBe 1
  }

  @Test
  fun `Utopia milestone metrics count combined metal production and card resource types`() {
    newGame(UtopiaPlanitiaMapOption)
    val otherPlayer = requireP2()
    p1.manual(
        "PROD[2 Steel, 4 Titanium], SearchForLife, Science<SearchForLife>, Predators, " +
            "Animal<Predators>, RegolithEaters, Microbe<RegolithEaters>"
    )
    otherPlayer.manual("PROD[10 Steel]")

    p1.count("PROD[Steel OR Titanium]") shouldBe 6
    otherPlayer.count("PROD[Steel OR Titanium]") shouldBe 10
    p1.count("Class<CardResource>(HAS CardResource<Player1>)") shouldBe 3
    p1.manual("Metallurgist")
    p1.count("Metallurgist") shouldBe 1
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
