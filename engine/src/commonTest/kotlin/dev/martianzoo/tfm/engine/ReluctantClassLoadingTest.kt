package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Option.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContainExactly
import kotlin.test.Test

/** Verifies reluctant class loading and characterizes known unwanted loading. */
internal class ReluctantClassLoadingTest {
  // Deliberate expansion-specific omissions

  @Test
  fun `card totals characterize progressively selected expansions`() {
    val selected = linkedSetOf(TerraformingMars)
    val totals = linkedMapOf<Canon.Option, Int>()
    for (option in
        listOf(
            TharsisMapOption,
            CorporateEraExpansion,
            VenusNextExpansion,
            PreludeExpansion,
            ColoniesExpansion,
            TurmoilCardPack,
            PromoCardPack,
        )) {
      selected += option
      val colonyTiles =
          if (ColoniesExpansion in selected) TestHelpers.testColonyTiles(2) else emptySet()
      totals[option] =
          Engine.newGame(
                  canonicalPremise(
                      selected,
                      colonyTiles = colonyTiles,
                      excludedOptions =
                          if (CorporateEraExpansion in selected) emptySet()
                          else setOf(CorporateEraExpansion),
                  )
              )
              .gameplay(ENGINE)
              .count("Class<CardFront>")
    }

    totals.shouldContainExactly(
        mapOf(
            TharsisMapOption to 144, // 148 minus BegCorp, Helion, Capital, and Flooding
            CorporateEraExpansion to 144 + 72, // 73 minus Land Claim
            VenusNextExpansion to 216 + 54,
            PreludeExpansion to 270 + 45, // 47 minus Research Network/Coordination
            ColoniesExpansion to 315 + 52, // 54 minus Aridor and Stormcraft
            TurmoilCardPack to 367 + 3, // only 3 corps working haha
            PromoCardPack to 370 + 84 - 3, // 96 minus 12 unsupported
        )
    )
  }

  @Test
  fun `Colonies classes stay unloaded without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    matchingClasses("colon(y|ie)", Setup.PROMOS_UTOPIA_WITHOUT_CORPORATE_ERA)
        .shouldContainExactlyInAnyOrder(cn("GanymedeColony"))
    matchingClasses("trade", Setup.PROMOS_UTOPIA_WITHOUT_CORPORATE_ERA)
        .shouldContainExactlyInAnyOrder(cn("InterplanetaryTrade"), cn("Trader"))
  }

  @Test
  fun `Corporate Era classes stay unloaded without Corporate Era`() {
    assertNotLoaded("CopyProductionBox", Setup.WITHOUT_CORPORATE_ERA)
  }

  @Test
  fun `Prelude classes stay unloaded without Prelude`() {
    // Promo has Prelude-gated cards, including one whose instruction names PreludeCard.
    matchingClasses("prelude", Setup.PROMOS_UTOPIA_WITHOUT_CORPORATE_ERA).shouldBeEmpty()
  }

  @Test
  fun `Venus classes stay unloaded without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertNotLoaded("VenusStep", Setup.PROMOS_CIMMERIA_WITHOUT_CORPORATE_ERA)
    assertNotLoaded("VenusTag", Setup.PROMOS_CIMMERIA_WITHOUT_CORPORATE_ERA)
  }

  // Deliberate setup- and mode-specific omissions

  @Test
  fun `player classes follow the selected seats`() {
    assertNotLoaded("Player2", Setup.BASE_SOLO)
    assertNotLoaded("Player3", Setup.BASE_MULTIPLAYER)
  }

  @Test
  fun `setup-world classes stay unloaded in playable games`() =
      assertNotLoaded("ValidateSetup", Setup.BASE_MULTIPLAYER)

  @Test
  fun `multiplayer standard actions stay unloaded in solo`() {
    assertNotLoaded("ClaimMilestoneSA", Setup.BASE_SOLO)
    assertNotLoaded("FundAwardSA", Setup.BASE_SOLO)
  }

  @Test
  fun `concrete award definitions and placement classes stay unloaded in solo`() {
    Setup.BASE_SOLO.classTable
        .getClass(cn("Award"))
        .allSubclasses()
        .map { it.className }
        .shouldContainExactly(cn("Award"))
    assertNotLoaded("FirstPlace", Setup.BASE_SOLO)
    assertNotLoaded("SecondPlace", Setup.BASE_SOLO)
    assertNotLoaded("AssignAwardPlaces", Setup.BASE_SOLO)
  }

  @Test
  fun `MultiplayerVictoryCheck stays unloaded in solo`() =
      assertNotLoaded("MultiplayerVictoryCheck", Setup.BASE_SOLO)

  // Mode and player-count boundaries

  @Test
  fun `GenerationSetup stays unloaded in multiplayer`() =
      assertNotLoaded("GenerationSetup", Setup.BASE_MULTIPLAYER)

  @Test
  fun `solo classes stay unloaded in multiplayer`() =
      matchingClasses("solo", Setup.PRELUDE_VENUS_MULTIPLAYER).shouldBeEmpty()

  @Test
  fun `abstract Award and its tallying machinery are incorrectly loaded in solo`() {
    matchingClasses("award", Setup.BASE_SOLO)
        .shouldContainExactlyInAnyOrder(
            cn("Award"),
            cn("MeasureAward"),
            cn("TallyAward"),
            cn("AwardTally"),
        )
  }

  @Test
  fun `Vitor does not load concrete award classes in solo`() {
    val setup = Setup.PRELUDE_SOLO
    matchingClasses("award", setup)
        .shouldContainExactlyInAnyOrder(
            cn("MeasureAward"),
            cn("TallyAward"),
            cn("AwardTally"),
            cn("Award"),
        )
    setup.classTable
        .getClass(cn("Award"))
        .allSubclasses()
        .map { it.className }
        .shouldContainExactly(cn("Award"))
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
      private val options: Set<Canon.Option>,
      private val players: Int = 2,
      private val excludedOptions: Set<Canon.Option> = emptySet(),
  ) {
    BASE_MULTIPLAYER(Canon.Option.DEFAULTS),
    BASE_SOLO(Canon.Option.DEFAULTS, 1),
    PRELUDE_SOLO(Canon.Option.DEFAULTS + PreludeExpansion, players = 1),
    WITHOUT_CORPORATE_ERA(
        Canon.Option.DEFAULTS,
        excludedOptions = setOf(CorporateEraExpansion),
    ),
    PROMOS_UTOPIA_WITHOUT_CORPORATE_ERA(
        setOf(TerraformingMars, PromoCardPack, UtopiaPlanitiaMapOption),
        excludedOptions = setOf(CorporateEraExpansion),
    ),
    PROMOS_CIMMERIA_WITHOUT_CORPORATE_ERA(
        setOf(TerraformingMars, PromoCardPack, TerraCimmeriaMapOption),
        excludedOptions = setOf(CorporateEraExpansion),
    ),
    PRELUDE_VENUS_MULTIPLAYER(Canon.Option.DEFAULTS + setOf(PreludeExpansion, VenusNextExpansion));

    val classTable by lazy {
      Engine.newGame(
              canonicalPremise(
                  options,
                  players = players,
                  excludedOptions = excludedOptions,
              )
          )
          .classTable
    }
    val classNames by lazy { classTable.allClassNamesAndIds }
  }
}
