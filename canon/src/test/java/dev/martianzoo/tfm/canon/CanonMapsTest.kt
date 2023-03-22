package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonMapsTest {
  @Test fun testTharsis() = checkMap(Canon.marsMap(cn("Tharsis")))

  @Test fun testHellas() = checkMap(Canon.marsMap(cn("Hellas")))

  @Test fun testElysium() = checkMap(Canon.marsMap(cn("Elysium")))

  private fun checkMap(map: MarsMapDefinition) {
    fun hasAtLeast5(it: Iterable<*>) = it.count { it != null } >= 5

    val grid: Grid<AreaDefinition> = map.areas
    assertThat(grid.size).isEqualTo(61)

    // We have an empty row first because we want coordinates to be 1-referenced
    assertThat(grid.rowCount).isEqualTo(10)
    assertThat(grid.rows().first().toSet()).containsExactly(null)
    assertThat(grid.rows().drop(1).all(::hasAtLeast5)).isTrue()

    assertThat(grid.columnCount).isEqualTo(10)
    assertThat(grid.columns().first().toSet()).containsExactly(null)
    assertThat(grid.columns().drop(1).all(::hasAtLeast5)).isTrue()

    // The way Grid handles diagonals is a little weird, to be sure
    assertThat(grid.diagonals().size).isEqualTo(19)
    assertThat(grid.diagonals().subList(0, 5).flatten().toSet()).containsExactly(null)
    assertThat(grid.diagonals().subList(5, 14).all(::hasAtLeast5)).isTrue()
    assertThat(grid.diagonals().subList(14, 19).flatten().toSet()).containsExactly(null)

    assertThat(grid.count { "${it.kind}" == "WaterArea" }).isEqualTo(12)
    assertThat(grid.count { "${it.kind}" == "VolcanicArea" }).isAnyOf(0, 4)
  }

  @Test
  fun testAllDistinctMapBonuses() {
    val bonuses =
        Canon.marsMapDefinitions
            .flatMap { it.areas }
            .mapNotNull { it.bonus?.unprocessed }
            .distinct()
            .toStrings()

    // This is brittle as we don't care which order the "a, b" bonuses are in
    assertThat(bonuses)
        .containsExactly(
            "ProjectCard",
            "Plant",
            "Steel",
            "Titanium",
            "2 ProjectCard",
            "2 Heat",
            "2 Plant",
            "2 Steel",
            "2 Titanium",
            "Plant, ProjectCard",
            "Plant, Steel",
            "Plant, Titanium",
            "3 ProjectCard",
            "3 Heat",
            "3 Plant",
            "OceanTile, -6",
        )
  }
}
