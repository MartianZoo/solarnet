package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.SpecialClassNames.ACTION_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.CARD_FRONT
import dev.martianzoo.tfm.data.SpecialClassNames.TILE
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.util.Grid
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonTest {
  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val table = PClassLoader(Canon).loadEverything()
    val ot = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(ot.intersectionType).isTrue()
    assertThat(table.getClass(OWNED).intersect(table.getClass(TILE))).isEqualTo(ot)
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val table = PClassLoader(Canon).loadEverything()
    val ac = table.getClass(ACTION_CARD)

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(ac.intersectionType).isTrue()
    assertThat(table.getClass(CARD_FRONT).intersect(table.getClass(cn("HasActions")))).isEqualTo(ac)
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

    assertThat(grid.count { "${it.kind}" == "WaterArea" }).isEqualTo(12)
    assertThat(grid.count { "${it.kind}" == "VolcanicArea" }).isAnyOf(0, 4)
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
    val expected =
        (Canon.allClassNames - unusedCards)
            .filterNot { it.matches(regex) }
            .filterNot { it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle) }

    assertThat(game.loader.allClasses.map { it.name }).containsExactlyElementsIn(expected)
  }

  val regex = Regex("(Hellas|Elysium|Player5|Camp|Row|Venus|Area2|Floater|Dirigible|AirScrap).*")

  @Test
  fun testThatSingletonComponentsWereCreated() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3))
    val startingComponents: Multiset<Component> = game.getAll(game.resolve(COMPONENT.type))
    assertThat(startingComponents.elements).hasSize(startingComponents.size)

    val isArea: (Component) -> Boolean = { it.asTypeExpr.toString().startsWith("Tharsis_") }
    val isBorder: (Component) -> Boolean = { it.asTypeExpr.toString().startsWith("Border<") }
    val isClass: (Component) -> Boolean = { it.asTypeExpr.toString().startsWith("Class<") }

    assertThat(startingComponents.count(isArea)).isEqualTo(61)
    assertThat(startingComponents.count(isBorder)).isEqualTo(312)
    assertThat(startingComponents.count(isClass)).isGreaterThan(400)

    assertThat(startingComponents.filterNot { isArea(it) || isBorder(it) || isClass(it) }
        .toStrings())
        .containsExactly(
            "[Player1]", "[Player2]", "[Player3]",
            "[Generation]",
            "[Tharsis]",
            "[PlayCardFromHand]", "[UseStandardProject]", "[ClaimMilestone]",
            "[UseActionFromCard]", "[ConvertHeat]", "[ConvertPlants]",
            "[SellPatents]",
            "[PowerPlantSP]", "[AsteroidSP]", "[AquiferSP]", "[GreenerySP]", "[CitySP]",
            "[MetalHandler]",
        )
  }

  @Test
  fun classCounts() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3))

    fun checkCount(count: Int, type: String) {
      assertThat(game.count(typeExpr(type))).isEqualTo(count)
    }

    checkCount(1, "Class<Class>")
    checkCount(2, "Class<CityTile>") // Huh? aha, Capital
    checkCount(3, "Class<GlobalParameter>")
    checkCount(4, "Class<CardResource>") // Plants Microbes Animals.. and Fighters
    checkCount(5, "Class<Milestone>")
    checkCount(6, "Class<StandardResource>")
    checkCount(7, "Class<StandardAction>")
    checkCount(8, "Class<SpecialTile>") // oops missing some
    checkCount(10, "Class<Tag>")
    checkCount(11, "Class<OwnedTile>") // so here too
    checkCount(12, "Class<WaterArea>")
    checkCount(63, "Class<Area>")

    assertThat(game.count(typeExpr("Class<CardFront>"))).isGreaterThan(200)
    assertThat(game.count(typeExpr("Class<Component>"))).isGreaterThan(300)
    assertThat(game.count(typeExpr("Class"))).isGreaterThan(300)
  }
}
