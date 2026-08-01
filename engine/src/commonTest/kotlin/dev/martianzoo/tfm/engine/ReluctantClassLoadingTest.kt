package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import kotlin.test.Test

/** Characterizes classes deliberately omitted from playable games that do not need them. */
internal class ReluctantClassLoadingTest {
  // Behaves as intended

  @Test
  fun `Colonies classes stay unloaded without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    matchingClasses("colon(y|ie)", Setup.PROMOS_UTOPIA_PLANITIA)
        .shouldContainExactlyInAnyOrder(cn("GanymedeColony"), cn("InterstellarColonyShip"))
    matchingClasses("trade", Setup.PROMOS_UTOPIA_PLANITIA)
        .shouldContainExactlyInAnyOrder(cn("InterplanetaryTrade"), cn("Trader"))
    assertNotLoaded("ResetProduction", Setup.PROMOS_UTOPIA_PLANITIA)
  }

  @Test
  fun `Corporate Era classes stay unloaded without Corporate Era`() {
    assertNotLoaded("NextCardEffect", Setup.WITHOUT_CORPORATE_ERA)
  }

  @Test
  fun `Prelude classes stay unloaded without Prelude`() {
    // Promo has Prelude-gated cards, including one whose instruction names PreludeCard.
    matchingClasses("prelude", Setup.PROMOS_UTOPIA_PLANITIA).shouldBeEmpty()
  }

  @Test
  fun `Venus classes stay unloaded without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertNotLoaded("VenusStep", Setup.PROMOS_TERRA_CIMMERIA)
    assertNotLoaded("VenusTag", Setup.PROMOS_TERRA_CIMMERIA)
  }

  @Test
  fun `player classes follow the selected seats and mode`() {
    assertNotLoaded("Opponent", Setup.BASE_MULTIPLAYER)
    assertNotLoaded("Player2", Setup.BASE_SOLO)
    assertNotLoaded("Player3", Setup.BASE_MULTIPLAYER)
  }

  @Test fun `ValidateSetup`() = assertNotLoaded("ValidateSetup", Setup.BASE_MULTIPLAYER)

  // Current behavior we would like to change

  @Test
  fun `award classes are incorrectly loaded in solo`() {
    matchingClasses("award", Setup.BASE_SOLO).shouldNotBeEmpty()
    assertLoaded("FirstPlace", Setup.BASE_SOLO)
    assertLoaded("SecondPlace", Setup.BASE_SOLO)
  }

  @Test
  fun `GenerationSetup is incorrectly loaded in multiplayer`() =
      assertLoaded("GenerationSetup", Setup.BASE_MULTIPLAYER)

  @Test
  fun `MultiplayerVictoryCheck is incorrectly loaded in solo`() =
      assertLoaded("MultiplayerVictoryCheck", Setup.BASE_SOLO)

  @Test
  fun `solo classes are incorrectly loaded in multiplayer`() {
    matchingClasses("solo", Setup.PRELUDE_VENUS_MULTIPLAYER).shouldNotBeEmpty()
  }

  private fun assertLoaded(className: String, setup: Setup) {
    setup.classNames.shouldContain(cn(className))
  }

  private fun assertNotLoaded(className: String, setup: Setup) {
    setup.classNames.shouldNotContain(cn(className))
  }

  private fun matchingClasses(pattern: String, setup: Setup) =
      setup.classTable
          .allClasses()
          .map { it.className }
          .filter { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(it.toString()) }

  private enum class Setup(
      private val instruction: String,
      private val players: Int = 2,
  ) {
    BASE_MULTIPLAYER(""),
    BASE_SOLO("SoloMode", 1),
    WITHOUT_CORPORATE_ERA("-CorporateEraExpansion"),
    PROMOS_UTOPIA_PLANITIA("PromoCardPack, UtopiaPlanitiaMapOption FROM TharsisMapOption"),
    PROMOS_TERRA_CIMMERIA("PromoCardPack, TerraCimmeriaMapOption FROM TharsisMapOption"),
    PRELUDE_VENUS_MULTIPLAYER("PreludeExpansion, VenusNextExpansion");

    val classTable by lazy { Engine.newGame(canonicalPremise(instruction, players)).classTable }
    val classNames by lazy { classTable.allClassNamesAndIds }
  }
}
