package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.api.Expression
import org.junit.jupiter.api.Test

internal class ParseMapTest {

  @Test
  fun testTharsis() {
    val thar: MarsMap = Canon.mapData["Tharsis"]!!
    assertThat(thar.name).isEqualTo("Tharsis")

    assertThat(thar.grid[5, 3]!!.typePetaform).isEqualTo("NoctisArea")
    assertThat(thar.grid[5, 3]!!.bonusPetaform).isEqualTo("2 Plant")

    assertThat(thar.grid[3, 2]!!.typePetaform).isEqualTo("LandArea")
    assertThat(thar.grid[3, 2]!!.bonusPetaform).isNull()

    assertThat(thar.grid[1, 4]!!.typePetaform).isEqualTo("WaterArea")
    assertThat(thar.grid[1, 4]!!.bonusPetaform).isEqualTo("Card")

    assertThat(thar.grid.count { it.type == Expression("WaterArea") }).isEqualTo(12)
  }

  @Test
  fun testHellas() {
    val hell: MarsMap = Canon.mapData["Hellas"]!!
    assertThat(hell.name).isEqualTo("Hellas")

    assertThat(hell.grid[5, 7]!!.typePetaform).isEqualTo("WaterArea")
    assertThat(hell.grid[5, 7]!!.bonusPetaform).isEqualTo("3 Heat")

    assertThat(hell.grid[9, 7]!!.typePetaform).isEqualTo("LandArea")
    assertThat(hell.grid[9, 7]!!.bonusPetaform).isEqualTo("OceanTile, -6")

    assertThat(hell.grid.count { it.type == Expression("WaterArea") }).isEqualTo(12)
  }

  @Test
  fun testElysium() {
    val elys: MarsMap = Canon.mapData["Elysium"]!!
    assertThat(elys.name).isEqualTo("Elysium")

    assertThat(elys.grid[1, 1]!!.typePetaform).isEqualTo("WaterArea")
    assertThat(elys.grid[1, 1]!!.bonusPetaform).isNull()

    assertThat(elys.grid[3, 7]!!.typePetaform).isEqualTo("VolcanicArea")
    assertThat(elys.grid[3, 7]!!.bonusPetaform).isEqualTo("3 Card")

    assertThat(elys.grid.count { it.type == Expression("WaterArea") }).isEqualTo(12)
  }

  @Test
  fun parseAllInstructions() {
    val uniqueAreas = Canon.mapData.values
        .asSequence()
        .flatMap { it.grid }
        .mapNotNull { it.bonus }
        .distinct()
        .map { it.toString() }
        .toSet()

    assertThat(uniqueAreas).containsExactly(
        "Card", "Plant", "Steel", "Titanium",
        "2 Card", "2 Heat", "2 Plant", "2 Steel", "2 Titanium",
        "Plant, Card", "Plant, Steel", "Plant, Titanium",
        "3 Card", "3 Heat", "3 Plant",
        "OceanTile, -6",
    )
  }
}
