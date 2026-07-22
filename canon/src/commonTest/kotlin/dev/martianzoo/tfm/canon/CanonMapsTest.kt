package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.util.Grid
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Tests for the Canon data set. */
internal class CanonMapsTest {
  @Test fun testTharsis() = checkMap(Canon.marsMap(cn("Tharsis")))

  @Test fun testHellas() = checkMap(Canon.marsMap(cn("Hellas")))

  @Test fun testElysium() = checkMap(Canon.marsMap(cn("Elysium")))

  @Test
  fun testTerraCimmeria() {
    val map = Canon.fromOptionCodes("BI", 2).map
    map.className shouldBe cn("TerraCimmeria")
    checkMap(map)
  }

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
}
