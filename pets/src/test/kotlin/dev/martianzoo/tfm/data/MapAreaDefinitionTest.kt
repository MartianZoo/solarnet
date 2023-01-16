package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test

private class MapAreaDefinitionTest {

  @Test
  fun testTharsis() {
    val thar: Grid<AreaDefinition> = Canon.marsMap(cn("Tharsis")).areas
    checkWaterAreaCount(thar)

    assertThat(thar[5, 3]!!.type.string).isEqualTo("NoctisArea")
    assertThat(thar[5, 3]!!.bonusText).isEqualTo("2 Plant")

    assertThat(thar[3, 2]!!.type.string).isEqualTo("LandArea")
    assertThat(thar[3, 2]!!.bonusText).isNull()

    assertThat(thar[1, 4]!!.type.string).isEqualTo("WaterArea")
    assertThat(thar[1, 4]!!.bonusText).isEqualTo("ProjectCard")
  }

  @Test
  fun testHellas() {
    val hell: Grid<AreaDefinition> = Canon.marsMap(cn("Hellas")).areas
    checkWaterAreaCount(hell)

    assertThat(hell[5, 7]!!.type.string).isEqualTo("WaterArea")
    assertThat(hell[5, 7]!!.bonusText).isEqualTo("3 Heat")

    assertThat(hell[9, 7]!!.type.string).isEqualTo("LandArea")
    assertThat(hell[9, 7]!!.bonusText).isEqualTo("OceanTile, -6")
  }

  @Test
  fun testElysium() {
    val elys: Grid<AreaDefinition> = Canon.marsMap(cn("Elysium")).areas
    checkWaterAreaCount(elys)

    assertThat(elys[1, 1]!!.type.string).isEqualTo("WaterArea")
    assertThat(elys[1, 1]!!.bonusText).isNull()

    assertThat(elys[3, 7]!!.type.string).isEqualTo("VolcanicArea")
    assertThat(elys[3, 7]!!.bonusText).isEqualTo("3 ProjectCard")

    assertThat(elys[5, 9]!!.type.string).isEqualTo("VolcanicArea")
    assertThat(elys[5, 9]!!.bonusText).isEqualTo("Plant, Titanium")
  }

  private fun checkWaterAreaCount(map: Grid<AreaDefinition>) {
    assertThat(map.count { it.type == cn("WaterArea") }).isEqualTo(12)
  }

  @Test
  fun parseAllInstructions() {
    val uniqueAreas = Canon.marsMapDefinitions
        .asSequence()
        .flatMap { it.areas }
        .mapNotNull { it.bonus }
        .distinct()
        .map(Any::toString)
        .toSet()

    assertThat(uniqueAreas).containsExactly(
        "ProjectCard", "Plant", "Steel", "Titanium",
        "2 ProjectCard", "2 Heat", "2 Plant", "2 Steel", "2 Titanium",
        "Plant, ProjectCard", "Plant, Steel", "Plant, Titanium",
        "3 ProjectCard", "3 Heat", "3 Plant",
        "OceanTile, -6",
    )
  }
}
