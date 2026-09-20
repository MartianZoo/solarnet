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

/** Verifies which Catalog Classes are selected and inhabited by each game premise. */
internal class ClassTableSelectionTest {
  // Deliberate expansion-specific omissions

  @Test
  internal fun `Colonies classes stay unselected without Colonies`() {
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
  internal fun `Corporate Era classes stay unselected without Corporate Era`() {
    assertNotSelected("CopyProductionBox", withoutCorporateEra)
  }

  @Test
  internal fun `Prelude classes stay unselected without Prelude`() {
    // Promo has Prelude cards, but its ordinary selection filters them without a Prelude card pack.
    matchingClasses("prelude", promosUtopiaWithoutCorporateEra).shouldBeEmpty()
  }

  @Test
  internal fun `Venus classes stay unselected without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertNotSelected("VenusStep", promosCimmeriaWithoutCorporateEra)
    assertNotSelected("VenusTag", promosCimmeriaWithoutCorporateEra)
  }

  // Deliberate mode-specific omissions

  @Test
  internal fun `player classes follow the selected seats`() {
    assertNotSelected("Player2", baseSolo)
    assertNotSelected("Player3", baseMultiplayer)
  }

  @Test
  internal fun `multiplayer standard actions stay unselected in solo`() {
    assertNotSelected("ClaimMilestoneAction", baseSolo)
    assertNotSelected("FundAwardAction", baseSolo)
  }

  @Test
  internal fun `concrete award classes stay uninhabited in solo`() {
    val award = baseSolo.classTable.getClass(cn("Award"))

    baseSolo.classTable.isInhabited(award) shouldBe false
    baseSolo.classTable.allSubclasses(award).shouldBeEmpty()
  }

  // Game-mode and player-count divisions

  @Test
  internal fun `solo classes stay unselected in multiplayer`() {
    matchingClasses("solo", preludeVenusMultiplayer).shouldBeEmpty()
  }

  @Test
  internal fun `award domain and scoring machinery stay uninhabited in solo`() {
    matchingClasses("award", baseSolo).shouldBeEmpty()
    baseSolo.classNames.shouldNotContain(cn("FirstPlace"))
    baseSolo.classNames.shouldNotContain(cn("SecondPlace"))
  }

  @Test
  internal fun `Vitor does not include the unreachable award domain in solo`() {
    val gameView = preludeSolo

    gameView.classTable.isInhabited(cn("Vitor")) shouldBe true
    matchingClasses("award", gameView).shouldBeEmpty()
    gameView.classNames.shouldNotContain(cn("FirstPlace"))
    gameView.classNames.shouldNotContain(cn("SecondPlace"))
  }

  private fun assertNotSelected(className: String, gameView: GameView) {
    gameView.classNames.shouldNotContain(cn(className))
  }

  private fun matchingClasses(pattern: String, gameView: GameView) =
      gameView.classTable
          .allClasses()
          .map { it.className }
          .filter { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(it.toString()) }

  private class GameView(private val config: GameConfig) {
    val classTable by lazy { Engine.newGame(Canon.gamePremise(config)).classTable }
    val classNames by lazy { classTable.allClassNames }
  }

  // Compiled game views belong to this test instance, not the test worker's lifetime.
  private val baseMultiplayer = gameView("", "Player1", "Player2")
  private val baseSolo = gameView("", "Me")
  private val preludeSolo = gameView("PreludeExpansion", "Me")
  private val withoutCorporateEra = gameView("-CorporateEraExpansion", "Player1", "Player2")
  private val promosUtopiaWithoutCorporateEra =
      gameView(
          "PromoCardPack, UtopiaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )
  private val promosCimmeriaWithoutCorporateEra =
      gameView(
          "PromoCardPack, CimmeriaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )
  private val preludeVenusMultiplayer =
      gameView("PreludeExpansion, VenusNextExpansion", "Player1", "Player2")

  private fun gameView(config: String, vararg playerNames: String): GameView =
      GameView(GameConfig(config, *playerNames))
}
