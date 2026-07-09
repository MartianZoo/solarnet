package dev.martianzoo.tfm.data

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test

private class MarsMapDefinitionTest {

  val demoMapJson =
      """
        {
          "legend": {
            "L": "LandArea", "W": "WaterArea", "V": "VolcanicArea",
            "P": "Plant", "S": "Steel", "C": "ProjectCard",
          },
          "maps": [{
            "name": "Demo",
            "bundle": "D",
            "rows": [
              [ "VS", "L" ],
              [ "V2P", "WPP", "WPC" ],
              [ "", "LSS", "LC" ],
            ]
          }]
        }"""

  @Test
  fun testDemoMapFromJson() {
    val map: MarsMapDefinition = JsonReader.readMaps(demoMapJson).single()
    map.bundle shouldBe "D"
    map.className shouldBe cn("Demo")
    map.asClassDeclaration.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("MarsMap"))
    map.areas.shouldHaveSize(7)
  }

  @Test
  fun testTharsis() {
    val thar: Grid<AreaDefinition> = Canon.marsMap(cn("Tharsis")).areas

    thar[5, 3]!!.kind shouldBe cn("NoctisArea")
    thar[5, 3]!!.bonusText shouldBe "2 Plant"

    thar[3, 2]!!.kind shouldBe cn("LandArea")
    thar[3, 2]!!.bonusText shouldBe null

    thar[1, 4]!!.kind shouldBe cn("WaterArea")
    thar[1, 4]!!.bonusText shouldBe "ProjectCard"
  }

  @Test
  fun testHellas() {
    val hell: Grid<AreaDefinition> = Canon.marsMap(cn("Hellas")).areas

    hell[5, 7]!!.kind shouldBe cn("WaterArea")
    hell[5, 7]!!.bonusText shouldBe "3 Heat"

    hell[9, 7]!!.kind shouldBe cn("LandArea")
    hell[9, 7]!!.bonusText shouldBe "OceanTile, -6"
  }

  @Test
  fun testElysium() {
    val elys: Grid<AreaDefinition> = Canon.marsMap(cn("Elysium")).areas

    elys[1, 1]!!.kind shouldBe cn("WaterArea")
    elys[1, 1]!!.bonusText shouldBe null

    elys[3, 7]!!.kind shouldBe cn("VolcanicArea")
    elys[3, 7]!!.bonusText shouldBe "3 ProjectCard"

    elys[5, 9]!!.kind shouldBe cn("VolcanicArea")
    elys[5, 9]!!.bonusText shouldBe "Plant, Titanium"
  }
}
