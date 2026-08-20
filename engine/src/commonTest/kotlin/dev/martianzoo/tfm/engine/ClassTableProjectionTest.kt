package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContainExactly
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
            "CorporateEraExpansion" to 146 + 72, // 73 minus Land Claim
            "VenusNextExpansion" to 218 + 54,
            "PreludeExpansion" to 272 + 47,
            "ColoniesExpansion" to 319 + 52, // 54 minus Aridor and Stormcraft
            "TurmoilCardPack" to 371 + 4,
            "PromoCardPack" to 375 + 87,
        )
    )
  }

  @Test
  fun `Colonies classes stay unloaded without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    matchingClasses("colon(y|ie)", promosUtopiaWithoutCorporateEra).shouldBeEmpty()
    matchingClasses("trade", promosUtopiaWithoutCorporateEra).shouldBeEmpty()
  }

  @Test
  fun `Corporate Era classes stay unloaded without Corporate Era`() {
    assertNotLoaded("CopyProductionBox", withoutCorporateEra)
  }

  @Test
  fun `Prelude classes stay unloaded without Prelude`() {
    // Promo has Prelude-gated cards, including one whose instruction names PreludeCard.
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
    baseSolo.classTable
        .getClass(cn("Award"))
        .allSubclasses()
        .map { it.className }
        .shouldContainExactly(cn("Award"))
  }

  @Test
  fun `MultiplayerVictoryCheck stays unloaded in solo`() =
      assertNotLoaded("MultiplayerVictoryCheck", baseSolo)

  // Mode and player-count boundaries

  @Test
  fun `GenerationSetup stays unloaded in multiplayer`() =
      assertNotLoaded("GenerationSetup", baseMultiplayer)

  @Test
  fun `solo classes stay unloaded in multiplayer`() =
      matchingClasses("solo", preludeVenusMultiplayer).shouldBeEmpty()

  @Test
  fun `abstract Award and its scoring machinery are incorrectly loaded in solo`() {
    matchingClasses("award", baseSolo)
        .shouldContainExactlyInAnyOrder(
            cn("Award"),
            cn("MeasureAward"),
            cn("AwardTally"),
            cn("AssignAwardPlaces"),
        )
    baseSolo.classNames.shouldContain(cn("FirstPlace"))
    baseSolo.classNames.shouldContain(cn("SecondPlace"))
  }

  @Test
  fun `Vitor does not load concrete award classes in solo`() {
    val projection = preludeSolo
    matchingClasses("award", projection)
        .shouldContainExactlyInAnyOrder(
            cn("MeasureAward"),
            cn("AwardTally"),
            cn("Award"),
            cn("AssignAwardPlaces"),
        )
    projection.classTable
        .getClass(cn("Award"))
        .allSubclasses()
        .map { it.className }
        .shouldContainExactly(cn("Award"))
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
            "PromoCardPack, UtopiaPlanitiaMapOption, -CorporateEraExpansion",
            "Player1",
            "Player2",
        )
    val promosCimmeriaWithoutCorporateEra =
        projection(
            "PromoCardPack, TerraCimmeriaMapOption, -CorporateEraExpansion",
            "Player1",
            "Player2",
        )
    val preludeVenusMultiplayer =
        projection("PreludeExpansion, VenusNextExpansion", "Player1", "Player2")

    fun projection(config: String, vararg playerNames: String): Projection =
        Projection(GameConfig(config, *playerNames))
  }
}
