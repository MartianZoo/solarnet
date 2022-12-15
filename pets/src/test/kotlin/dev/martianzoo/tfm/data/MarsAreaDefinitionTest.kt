package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.TypeExpression
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test

internal class MarsAreaDefinitionTest {

  @Test
  fun testTharsis() {
    val thar: Grid<MarsAreaDefinition> = Canon.mapAreaDefinitions["Tharsis"]!!
    checkWaterAreaCount(thar)

    assertThat(thar[5, 3]!!.typeText).isEqualTo("NoctisArea")
    assertThat(thar[5, 3]!!.bonusText).isEqualTo("2 Plant")

    assertThat(thar[3, 2]!!.typeText).isEqualTo("LandArea")
    assertThat(thar[3, 2]!!.bonusText).isNull()

    assertThat(thar[1, 4]!!.typeText).isEqualTo("WaterArea")
    assertThat(thar[1, 4]!!.bonusText).isEqualTo("ProjectCard")
  }

  @Test
  fun testHellas() {
    val hell: Grid<MarsAreaDefinition> = Canon.mapAreaDefinitions["Hellas"]!!
    checkWaterAreaCount(hell)

    assertThat(hell[5, 7]!!.typeText).isEqualTo("WaterArea")
    assertThat(hell[5, 7]!!.bonusText).isEqualTo("3 Heat")

    assertThat(hell[9, 7]!!.typeText).isEqualTo("LandArea")
    assertThat(hell[9, 7]!!.bonusText).isEqualTo("OceanTile, -6")
  }

  @Test
  fun testElysium() {
    val elys: Grid<MarsAreaDefinition> = Canon.mapAreaDefinitions["Elysium"]!!
    checkWaterAreaCount(elys)

    assertThat(elys[1, 1]!!.typeText).isEqualTo("WaterArea")
    assertThat(elys[1, 1]!!.bonusText).isNull()

    assertThat(elys[3, 7]!!.typeText).isEqualTo("VolcanicArea")
    assertThat(elys[3, 7]!!.bonusText).isEqualTo("3 ProjectCard")
  }

  private fun checkWaterAreaCount(map: Grid<MarsAreaDefinition>) {
    assertThat(map.count { it.type == TypeExpression("WaterArea") }).isEqualTo(12)
  }

  @Test
  fun parseAllInstructions() {
    val uniqueAreas = Canon.mapAreaDefinitions.values
        .asSequence()
        .flatMap { it }
        .mapNotNull { it.bonus }
        .distinct()
        .map { it.toString() }
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
