package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtopiaCimmeriaExpansionTest : CardTest() {
  @Test
  fun `MSL Curiosity bonus is inert without Colonies`() {
    newGame("TerraCimmeriaMapOption FROM TharsisMapOption")
    p1.manual("10")

    p1.manual("CityTile<TerraCimmeria_3_3>")

    p1.count("Megacredit") shouldBe 10
    p1.count("Colony") shouldBe 0
  }

  @Test
  fun `MSL Curiosity bonus can buy a colony with Colonies`() {
    newGame(
        "ColoniesExpansion,TerraCimmeriaMapOption FROM TharsisMapOption",
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
  fun `Suburbian counts tiles having at most four neighboring Mars areas`() {
    newGame(UtopiaPlanitiaMapOption)
    val marsAreas = Canon.marsMap(cn("UtopiaPlanitia")).areas.filterNotNull()
    p1.manual(marsAreas.joinToString { "CityTile<${it.className}>" })

    val expectedBorder =
        setOf(
            "UtopiaPlanitia_1_1",
            "UtopiaPlanitia_1_2",
            "UtopiaPlanitia_1_3",
            "UtopiaPlanitia_1_4",
            "UtopiaPlanitia_1_5",
            "UtopiaPlanitia_2_1",
            "UtopiaPlanitia_2_6",
            "UtopiaPlanitia_3_1",
            "UtopiaPlanitia_3_7",
            "UtopiaPlanitia_4_1",
            "UtopiaPlanitia_4_8",
            "UtopiaPlanitia_5_1",
            "UtopiaPlanitia_5_9",
            "UtopiaPlanitia_6_2",
            "UtopiaPlanitia_6_9",
            "UtopiaPlanitia_7_3",
            "UtopiaPlanitia_7_9",
            "UtopiaPlanitia_8_4",
            "UtopiaPlanitia_8_9",
            "UtopiaPlanitia_9_5",
            "UtopiaPlanitia_9_6",
            "UtopiaPlanitia_9_7",
            "UtopiaPlanitia_9_8",
            "UtopiaPlanitia_9_9",
        )
    val actualBorder =
        marsAreas
            .filter {
              p1.count("OwnedTile<${it.className}>(HAS MAX 4 Neighbor<OwnedTile>)") == 1
            }
            .mapTo(linkedSetOf()) { it.className.toString() }

    actualBorder shouldBe expectedBorder
    p1.count("OwnedTile(HAS MAX 4 Neighbor<OwnedTile>)") shouldBe expectedBorder.size
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
}
