package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertSame

/** Verifies which classes are active in game-specific class-table projections. */
internal class ClassTableProjectionTest {
  // Deliberate expansion-specific omissions

  @Test
  internal fun `Colonies classes stay unloaded without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    val bundle = Canon.bundles.single { it.bundleName == cn("ColoniesExpansion") }
    fun contributedNames(catalog: TfmCatalog): Set<ClassName> = buildSet {
      catalog.explicitClassDeclarations.mapTo(this) { it.className }
      catalog.marsMapDefinitions.forEach { map ->
        add(map.className)
        map.areas.mapTo(this) { area -> area.className }
      }
    }
    val namesUniqueToColonies =
        contributedNames(bundle) -
            Canon.bundles.filterNot { it == bundle }.flatMapTo(linkedSetOf(), ::contributedNames)
    (promosUtopiaWithoutCorporateEra.classNames intersect namesUniqueToColonies).shouldBeEmpty()
  }

  @Test
  internal fun `Corporate Era classes stay unloaded without Corporate Era`() {
    assertNotLoaded("CopyProductionBox", withoutCorporateEra)
  }

  @Test
  internal fun `Prelude classes stay unloaded without Prelude`() {
    // Promo has Prelude cards, but its ordinary selection filters them without a Prelude card pack.
    matchingClasses("prelude", promosUtopiaWithoutCorporateEra).shouldBeEmpty()
  }

  @Test
  internal fun `Venus classes stay unloaded without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertNotLoaded("VenusStep", promosCimmeriaWithoutCorporateEra)
    assertNotLoaded("VenusTag", promosCimmeriaWithoutCorporateEra)
  }

  // Deliberate mode-specific omissions

  @Test
  internal fun `player classes follow the selected seats`() {
    assertNotLoaded("Player2", baseSolo)
    assertNotLoaded("Player3", baseMultiplayer)
  }

  @Test
  internal fun `multiplayer standard actions stay unloaded in solo`() {
    assertNotLoaded("ClaimMilestone", baseSolo)
    assertNotLoaded("FundAward", baseSolo)
  }

  @Test
  internal fun `concrete award classes stay unloaded in solo`() {
    val award = baseSolo.classTable.getClass(cn("Award"))

    assertSame(Canon.classTable.getClass(cn("Award")), award)
    baseSolo.classTable.isActive(award) shouldBe false
    baseSolo.classTable.allSubclasses(award).shouldBeEmpty()
  }

  @Test
  internal fun `AssignMultiplayerVictory stays unloaded in solo`() =
      assertNotLoaded("AssignMultiplayerVictory", baseSolo)

  // Game-mode and player-count divisions

  @Test
  internal fun `SoloGenerationSetup stays unloaded in multiplayer`() =
      assertNotLoaded("SoloGenerationSetup", baseMultiplayer)

  @Test
  internal fun `solo classes stay unloaded in multiplayer`() {
    matchingClasses("solo", preludeVenusMultiplayer).shouldBeEmpty()
  }

  @Test
  internal fun `award domain and scoring machinery stay uninhabited in solo`() {
    matchingClasses("award", baseSolo).shouldBeEmpty()
    baseSolo.classNames.shouldNotContain(cn("FirstPlace"))
    baseSolo.classNames.shouldNotContain(cn("SecondPlace"))
  }

  @Test
  internal fun `Vitor does not activate the unreachable award domain in solo`() {
    val projection = preludeSolo

    projection.classTable.isActive(cn("Vitor")) shouldBe true
    matchingClasses("award", projection).shouldBeEmpty()
    projection.classNames.shouldNotContain(cn("FirstPlace"))
    projection.classNames.shouldNotContain(cn("SecondPlace"))
  }

  private fun assertNotLoaded(className: String, projection: Projection) {
    projection.classNames.shouldNotContain(cn(className))
  }

  private fun matchingClasses(pattern: String, projection: Projection) =
      projection.classTable
          .allClasses()
          .map { it.className }
          .filter { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(it.toString()) }

  private class Projection(private val config: GameConfig) {
    val classTable by lazy { Engine.newGame(Canon.gamePremise(config)).classTable }
    val classNames by lazy { classTable.allClassNames }
  }

  private companion object {
    val baseMultiplayer = projection("", "Player1", "Player2")
    val baseSolo = projection("", "Me")
    val preludeSolo = projection("PreludeExpansion", "Me")
    val withoutCorporateEra = projection("-CorporateEraExpansion", "Player1", "Player2")
    val promosUtopiaWithoutCorporateEra =
        projection(
            "PromoCardPack, UtopiaMap, -CorporateEraExpansion",
            "Player1",
            "Player2",
        )
    val promosCimmeriaWithoutCorporateEra =
        projection(
            "PromoCardPack, CimmeriaMap, -CorporateEraExpansion",
            "Player1",
            "Player2",
        )
    val preludeVenusMultiplayer =
        projection("PreludeExpansion, VenusNextExpansion", "Player1", "Player2")

    fun projection(config: String, vararg playerNames: String): Projection =
        Projection(GameConfig(config, *playerNames))
  }
}
