package dev.martianzoo.tfm.canon

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
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
    grid.size shouldBe 61

    // We have an empty row first because we want coordinates to be 1-referenced
    grid.rowCount shouldBe 10
    grid.rows().first().toSet() shouldBe setOf(null)
    grid.rows().drop(1).all(::hasAtLeast5) shouldBe true

    grid.columnCount shouldBe 10
    grid.columns().first().toSet() shouldBe setOf(null)
    grid.columns().drop(1).all(::hasAtLeast5) shouldBe true

    // The way Grid handles diagonals is a little weird, to be sure
    grid.diagonals().size shouldBe 19
    grid.diagonals().subList(0, 5).flatten().toSet() shouldBe setOf(null)
    grid.diagonals().subList(5, 14).all(::hasAtLeast5) shouldBe true
    grid.diagonals().subList(14, 19).flatten().toSet() shouldBe setOf(null)

    grid.count { "${it.kind}" == "WaterArea" } shouldBe 12
    grid.count { "${it.kind}" == "VolcanicArea" } shouldBeIn listOf(0, 4)
  }

  @Test
  fun testAllDistinctMapBonuses() {
    val bonuses =
        Canon.marsMapDefinitions.flatMap { it.areas }.mapNotNull { it.bonus }.distinct().toStrings()

    // This is brittle as we don't care which order the "a, b" bonuses are in
    bonuses.toSet() shouldBe
        setOf(
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
