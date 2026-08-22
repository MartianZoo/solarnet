package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Verifies which classes are active in game-specific class-table projections. */
internal class ClassTableProjectionTest {
  // Deliberate expansion-specific omissions

  // Stop deleting this test when cards are added; update the expected totals instead.
  @Test
  fun contentChangeDetectorTestThatIAmNotGoingToDeleteSoPleaseStopSuggestingIt() {
    val corporateEra = cn("CorporateEraExpansion")
    val colonies = cn("ColoniesExpansion")
    val selected = linkedSetOf<ClassName>()
    val totals = linkedMapOf<String, Int>()
    for ((label, option) in
        listOf(
            "Base" to null,
            "CorporateEraExpansion" to corporateEra,
            "VenusNextExpansion" to cn("VenusNextExpansion"),
            "PreludeExpansion" to cn("PreludeExpansion"),
            "ColoniesExpansion" to colonies,
            "TurmoilCardPack" to cn("TurmoilCardPack"),
            "PromoCardPack" to cn("PromoCardPack"),
        )) {
      option?.let(selected::add)
      val colonyTiles = if (colonies in selected) TestHelpers.testColonyTiles(2) else emptySet()
      val config =
          GameConfig.create(
              included = selected + colonyTiles,
              excluded = if (corporateEra in selected) emptySet() else setOf(corporateEra),
              playerNames = listOf(cn("Player1"), cn("Player2")),
          )
      totals[label] =
          Engine.newGame(Canon.gamePremise(config)).gameplay(ENGINE).count("Class<CardFront>")
    }

    totals.shouldContainExactly(
        mapOf(
            "Base" to 146, // 148 minus BegCorp and Helion
            "CorporateEraExpansion" to 146 + 73,
            "VenusNextExpansion" to 219 + 54,
            "PreludeExpansion" to 273 + 47,
            "ColoniesExpansion" to 320 + 52, // 54 minus Aridor and Stormcraft
            "TurmoilCardPack" to 372 + 4,
            "PromoCardPack" to 376 + 88,
        )
    )
  }

  @Test
  fun `Colonies classes stay unloaded without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    val bundle = Canon.bundles.single { it.bundleName == cn("ColoniesExpansion") }
    fun contributedNames(authority: TfmAuthority): Set<ClassName> = buildSet {
      authority.explicitClassDeclarations.mapTo(this) { it.className }
      authority.allDefinitions.mapTo(this) { it.className }
      authority.cardDefinitions.flatMapTo(this) { card -> card.extraClasses.map { it.className } }
    }
    val namesUniqueToColonies =
        contributedNames(bundle) -
            Canon.bundles.filterNot { it == bundle }.flatMapTo(linkedSetOf(), ::contributedNames)
    (promosUtopiaWithoutCorporateEra.classNames intersect namesUniqueToColonies).shouldBeEmpty()
  }

  @Test
  fun `Corporate Era classes stay unloaded without Corporate Era`() {
    assertNotLoaded("CopyProductionBox", withoutCorporateEra)
  }

  @Test
  fun `Prelude classes stay unloaded without Prelude`() {
    // Promo has Prelude cards, but its ordinary selection filters them without a Prelude deck.
    matchingClasses("prelude", promosUtopiaWithoutCorporateEra).shouldBeEmpty()
  }

  @Test
  fun `Venus classes stay unloaded without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertNotLoaded("VenusStep", promosCimmeriaWithoutCorporateEra)
    assertNotLoaded("VenusTag", promosCimmeriaWithoutCorporateEra)
  }

  // Deliberate mode-specific omissions

  @Test
  fun `player classes follow the selected seats`() {
    assertNotLoaded("Player2", baseSolo)
    assertNotLoaded("Player3", baseMultiplayer)
  }

  @Test
  fun `multiplayer standard actions stay unloaded in solo`() {
    assertNotLoaded("ClaimMilestoneSA", baseSolo)
    assertNotLoaded("FundAwardSA", baseSolo)
  }

  @Test
  fun `concrete award definitions stay unloaded in solo`() {
    val award = baseSolo.classTable.getClass(cn("Award"))

    award.phantom shouldBe true
    award.allSubclasses().shouldBeEmpty()
  }

  @Test
  fun `MultiplayerVictoryCheck stays unloaded in solo`() =
      assertNotLoaded("MultiplayerVictoryCheck", baseSolo)

  // Mode and player-count boundaries

  @Test
  fun `GenerationSetup stays unloaded in multiplayer`() =
      assertNotLoaded("GenerationSetup", baseMultiplayer)

  @Test
  fun `solo classes stay unloaded in multiplayer`() {
    matchingClasses("solo", preludeVenusMultiplayer).shouldBeEmpty()
  }

  @Test
  fun `award domain and scoring machinery stay uninhabited in solo`() {
    matchingClasses("award", baseSolo).shouldBeEmpty()
    baseSolo.classNames.shouldNotContain(cn("FirstPlace"))
    baseSolo.classNames.shouldNotContain(cn("SecondPlace"))
  }

  @Test
  fun `Vitor does not activate the unreachable award domain in solo`() {
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
    val classTable by lazy {
      Engine.newGame(Canon.gamePremise(config)).classTable
    }
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
