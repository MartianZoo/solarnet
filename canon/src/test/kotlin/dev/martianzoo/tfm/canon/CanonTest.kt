package dev.martianzoo.tfm.canon

import com.google.common.collect.Multiset
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.SpecialClassNames.CARD_FRONT
import dev.martianzoo.tfm.data.SpecialClassNames.TILE
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonTest {
  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val table = PetClassLoader(Canon).loadEverything()
    val ot = table["OwnedTile"]

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(ot.intersectionType).isTrue()
    assertThat(table[OWNED].intersect(table[TILE])).isEqualTo(ot)
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val table = PetClassLoader(Canon).loadEverything()
    val ac = table["ActionCard"]

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(ac.intersectionType).isTrue()
    assertThat(table[CARD_FRONT].intersect(table[cn("HasActions")])).isEqualTo(ac)
  }

  @Test
  fun testTharsis() {
    checkMap(Canon.marsMap(cn("Tharsis")))
  }

  @Test
  fun testHellas() {
    checkMap(Canon.marsMap(cn("Hellas")))
  }

  @Test
  fun testElysium() {
    checkMap(Canon.marsMap(cn("Elysium")))
  }

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

    // So, the way Grid handles diagonals is a little weird ... TODO
    assertThat(grid.diagonals().size).isEqualTo(19)
    assertThat(grid.diagonals().subList(0, 5).flatten().toSet()).containsExactly(null)
    assertThat(grid.diagonals().subList(5, 14).all(::hasAtLeast5)).isTrue()
    assertThat(grid.diagonals().subList(14, 19).flatten().toSet()).containsExactly(null)

    assertThat(grid.count { "${it.type}" == "WaterArea" }).isEqualTo(12)
    assertThat(grid.count { "${it.type}" == "VolcanicArea" }).isAnyOf(0, 4)
  }

  @Test
  fun testAllDistinctMapBonuses() {
    val bonuses =
        Canon.marsMapDefinitions.flatMap { it.areas }.mapNotNull { it.bonus }.distinct().toStrings()

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

  @Test
  fun loadsExpectedClasses() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 4))
    val unusedCards =
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.map { it.name }.toSet()

    val milestoneNames = Canon.milestoneDefinitions.map { it.name }
    val expected = (Canon.allClassNames - unusedCards)
        .filterNot { it.matches(regex) }
        .filterNot { it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle) }

    assertThat(game.classTable.loadedClassNames()).containsExactlyElementsIn(expected)
  }

  val regex = Regex("(Hellas|Elysium|Player5|Camp|Row|Venus|Area2|Floater|Dirigible|AirScrap).*")

  @Test
  fun createdSingletons() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3))
    val all: Multiset<Component> = game.components.getAll(game.classTable.resolve("Component"))

    val isArea: (Component) -> Boolean = { it.asTypeExpression.toString().startsWith("Tharsis_") }
    val isBorder: (Component) -> Boolean = {
      it.asTypeExpression.asGeneric().root.toString() == "Border"
    }

    assertThat(all.elementSet().count(isArea)).isEqualTo(61)
    assertThat(all.elementSet().count(isBorder)).isEqualTo(312)

    assertThat(all.filterNot { isArea(it) || isBorder(it) }.map { it.asTypeExpression.toString() })
        .containsExactly(
            "Player1", "Player2", "Player3",
            "Generation",
            "Tharsis",
            "ClaimMilestone", "ConvertHeat", "ConvertPlants",
            "PlayCardFromHand", "UseActionFromCard", "UseStandardProject",
            "SellPatents",
            "PowerPlantSP", "AsteroidSP", "AquiferSP", "GreenerySP", "CitySP",
        )
  }
}
