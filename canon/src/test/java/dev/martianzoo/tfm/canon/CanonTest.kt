package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.util.Grid
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toSetStrict
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonTest {
  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val table = PClassLoader(Canon).loadEverything()

    val owned = table.getClass(cn("Owned"))
    val tile = table.getClass(cn("Tile"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(owned.intersect(tile)).isEqualTo(ownedTile)
    assertThat(ownedTile.intersectionType).isTrue()
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val table = PClassLoader(Canon).loadEverything()

    val cardFront = table.getClass(cn("CardFront"))
    val hasActions = table.getClass(cn("HasActions"))
    val actionCard = table.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(cardFront.intersect(hasActions)).isEqualTo(actionCard)
    assertThat(actionCard.intersectionType).isTrue()
  }

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
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.classNames().toSetStrict()

    val milestoneNames = Canon.milestoneDefinitions.classNames().toSetStrict()
    val expected =
        (Canon.allClassNames - unusedCards)
            .filterNot { it.matches(regex) }
            .filterNot { it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle) }

    assertThat(game.loader.allClasses.classNames()).containsExactlyElementsIn(expected)
  }

  val regex = Regex("(Hellas|Elysium|Player5|Camp|Row|Venus|Area2|Floater|Dirigible|AirScrap).*")

  @Test
  fun testThatSingletonComponentsWereCreated() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3))
    val startingComponents: Multiset<Component> =
        game.getComponents(game.resolveType(COMPONENT.type))
    assertThat(startingComponents.elements).hasSize(startingComponents.size)

    val isArea: (Component) -> Boolean = { it.toString().startsWith("[Tharsis_") }
    val isBorder: (Component) -> Boolean = { it.toString().startsWith("[Border<") }
    val isClass: (Component) -> Boolean = { it.toString().startsWith("[Class<") }

    assertThat(startingComponents.count(isArea)).isEqualTo(61)
    assertThat(startingComponents.count(isBorder)).isEqualTo(312)
    assertThat(startingComponents.count(isClass)).isGreaterThan(400)

    assertThat(
            startingComponents.filterNot { isArea(it) || isBorder(it) || isClass(it) }.toStrings())
        .containsExactly(
            "[Game]",
            "[Tharsis]",
            "[Player1]",
            "[Player2]",
            "[Player3]",
            "[PlayCardFromHand]",
            "[UseStandardProject]",
            "[ClaimMilestone]",
            "[UseActionFromCard]",
            "[ConvertHeat]",
            "[ConvertPlants]",
            "[SellPatents]",
            "[PowerPlantSP]",
            "[AsteroidSP]",
            "[AquiferSP]",
            "[GreenerySP]",
            "[CitySP]",
        )
  }

  @Test
  fun classCounts() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3))

    fun checkCount(count: Int, type: String) {
      assertThat(game.countComponents(te(type))).isEqualTo(count)
    }

    checkCount(1, "Class<Class>")
    checkCount(2, "Class<CityTile>") // Huh? aha, Capital
    checkCount(3, "Class<GlobalParameter>")
    checkCount(4, "Class<CardResource>") // Plants Microbes Animals.. and Fighters
    checkCount(5, "Class<Milestone>")
    checkCount(6, "Class<StandardResource>")
    checkCount(7, "Class<StandardAction>")
    checkCount(9, "Class<SpecialTile>") // oops missing some
    checkCount(10, "Class<Tag>")
    checkCount(11, "Class<OwnedTile>") // so here too
    checkCount(12, "Class<WaterArea>")
    checkCount(63, "Class<Area>")

    assertThat(game.countComponents(te("Class<CardFront>"))).isGreaterThan(200)
    assertThat(game.countComponents(te("Class<Component>"))).isGreaterThan(300)
    assertThat(game.countComponents(te("Class"))).isGreaterThan(300)
  }

  @Test
  fun concreteExtendingConcrete() {
    val loader = PClassLoader(Canon).loadEverything()
    val map = mutableListOf<Pair<ClassName, ClassName>>()
    loader.allClasses
        .filterNot { it.abstract }
        .forEach { sup ->
          (sup.allSubclasses - setOf(sup)).forEach { map += sup.className to it.className }
        }
    assertThat(map).containsExactly(cn("CityTile") to cn("Tile008"))
  }

  @Test
  fun onlyCardboundHasDepToDepLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val haveLinkages = declarations.filter { it.depToDepLinkages.any() }
    assertThat(haveLinkages.classNames()).containsExactly(
        cn("Cardbound"),
        cn("Adjacency") // TODO fix this!!
    )
    assertThat(Canon.classDeclaration(cn("Cardbound")).depToDepLinkages).containsExactly(ANYONE)
  }

  @Test
  fun depToEffectLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val haveLinkages = declarations.filter { it.depToEffectLinkages.any() }
    assertThat(haveLinkages.classNames().toStrings()).containsExactly(
        "Border",
        "Neighbor",
        "Production",
        "Owed",
        "Pay",

        // TODO these should be harmless, but they're wrong; how to get them out?
        // OPEN CLASS might help
        "Astrodrill",
        "PharmacyUnion",
        "AerialMappers",
        "Extremophiles",
        "FloatingHabs",
        "AtmoCollectors",
        "JovianLanterns",
        "CometAiming",
        "DirectedImpactors",
        "AsteroidRights",
    )
  }

  private fun te(s: String) = typeExpr(s)
}
