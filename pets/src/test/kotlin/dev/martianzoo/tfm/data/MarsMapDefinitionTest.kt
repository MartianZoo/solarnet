package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test

class MarsMapDefinitionTest {

  val demoMapJson = """{
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
    assertThat(map.bundle).isEqualTo("D")
    assertThat(map.name).isEqualTo(cn("Demo"))
    assertThat(map.asClassDeclaration.superclassNames).containsExactly(cn("MarsMap"))
    assertThat(map.areas).hasSize(7)
  }

  @Test
  fun testTharsis() {
    val thar: Grid<AreaDefinition> = Canon.marsMap(cn("Tharsis")).areas

    assertThat(thar[5, 3]!!.type).isEqualTo(cn("NoctisArea"))
    assertThat(thar[5, 3]!!.bonusText).isEqualTo("2 Plant")

    assertThat(thar[3, 2]!!.type).isEqualTo(cn("LandArea"))
    assertThat(thar[3, 2]!!.bonusText).isNull()

    assertThat(thar[1, 4]!!.type).isEqualTo(cn("WaterArea"))
    assertThat(thar[1, 4]!!.bonusText).isEqualTo("ProjectCard")
  }

  @Test
  fun testHellas() {
    val hell: Grid<AreaDefinition> = Canon.marsMap(cn("Hellas")).areas

    assertThat(hell[5, 7]!!.type).isEqualTo(cn("WaterArea"))
    assertThat(hell[5, 7]!!.bonusText).isEqualTo("3 Heat")

    assertThat(hell[9, 7]!!.type).isEqualTo(cn("LandArea"))
    assertThat(hell[9, 7]!!.bonusText).isEqualTo("OceanTile, -6")
  }

  @Test
  fun testElysium() {
    val elys: Grid<AreaDefinition> = Canon.marsMap(cn("Elysium")).areas

    assertThat(elys[1, 1]!!.type).isEqualTo(cn("WaterArea"))
    assertThat(elys[1, 1]!!.bonusText).isNull()

    assertThat(elys[3, 7]!!.type).isEqualTo(cn("VolcanicArea"))
    assertThat(elys[3, 7]!!.bonusText).isEqualTo("3 ProjectCard")

    assertThat(elys[5, 9]!!.type).isEqualTo(cn("VolcanicArea"))
    assertThat(elys[5, 9]!!.bonusText).isEqualTo("Plant, Titanium")
  }
}
