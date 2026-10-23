package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Verifies which Catalog Classes are selected and inhabited by each game premise. */
internal class ClassTableSelectionTest {
  // Selection and inhabitance

  @Test
  internal fun `known selected and inhabited are different questions`() {
    val base = gameView("").classTable
    val emptyAwardDomain = gameView("Award", "Player1")

    // Known concrete Class omitted by this premise.
    val venusStep = requireNotNull(base.findClass(cn("VenusStep")))
    venusStep shouldBe Canon.classTable.getClass(cn("VenusStep"))
    base.allClassNames.shouldNotContain(cn("VenusStep"))
    base.isInhabited(cn("VenusStep")) shouldBe false
    shouldThrow<IllegalArgumentException> { base.effects(venusStep) }

    // Included abstract Class with no included concrete narrowing.
    emptyAwardDomain.classNames.contains(cn("Award")) shouldBe true
    emptyAwardDomain.classTable.isInhabited(cn("Award")) shouldBe false
    emptyAwardDomain.classTable
        .allSubclasses(emptyAwardDomain.classTable.getClass(cn("Award")))
        .filterNot { it.abstract }
        .shouldBeEmpty()

    // The same abstract Class becomes inhabited when its concrete children are selected.
    base.isInhabited(cn("Award")) shouldBe true
    base.allSubclasses(base.getClass(cn("Award"))).filterNot { it.abstract }.isEmpty() shouldBe
        false
  }

  @Test
  internal fun `a directly selected card resource remains uninhabited without its card`() {
    assertValidView("Camp") { view ->
      view.classNames.shouldContain(cn("Camp"))
      view.classTable.isInhabited(cn("Camp")) shouldBe false
    }
  }

  // Players and game variants

  @Test
  internal fun `solo rules and player seats follow the number of players`() {
    val soloGame = gameView("", "Player1")
    val twoPlayerGame = gameView("")

    assertSelected(soloGame, setOf(cn("SoloOpponent")))
    assertOmitted(soloGame, setOf(cn("ClaimMilestone")))
    // Player seats are premise-local declarations, not classes in the Canon catalog.
    soloGame.classNames.shouldNotContain(cn("Player2"))
    assertOmitted(twoPlayerGame, setOf(cn("SoloOpponent")))
    assertSelected(twoPlayerGame, setOf(cn("ClaimMilestone")))
    twoPlayerGame.classNames.shouldContain(cn("Player2"))
  }

  @Test
  internal fun `the TR 63 solo objective replaces the standard objective`() {
    val standardSolo = gameView("", "Player1")
    val tr63Solo = gameView("Tr63SoloObjective", "Player1")

    assertSelected(standardSolo, setOf(cn("StandardSoloObjective")))
    assertOmitted(standardSolo, setOf(cn("Tr63SoloObjective")))
    assertOmitted(tr63Solo, setOf(cn("StandardSoloObjective")))
    assertSelected(tr63Solo, setOf(cn("Tr63SoloObjective")))
  }

  @Test
  internal fun `Quick Start replaces Corporate Era rules when Corporate Era is excluded`() {
    val corporateEraGame = gameView("")
    val quickStartGame = gameView("-CorporateEraExpansion")

    assertSelected(corporateEraGame, setOf(cn("CopyProductionBox")))
    assertOmitted(corporateEraGame, setOf(cn("QuickStartVariant")))
    assertOmitted(quickStartGame, setOf(cn("CopyProductionBox")))
    assertSelected(quickStartGame, setOf(cn("QuickStartVariant")))
  }

  @Test
  internal fun `the beginner variant includes beginner corporations and their card back`() {
    val standardGame = gameView("")
    val beginnerGame = gameView("BeginnerVariant")

    assertOmitted(standardGame, setOf(cn("BeginnerCorporation1")))
    assertOmitted(standardGame, setOf(cn("BeginnerCard")))
    assertOmitted(standardGame, setOf(cn("BeginnerOption")))
    assertSelected(standardGame, setOf(cn("StandardOption")))
    assertSelected(beginnerGame, setOf(cn("BeginnerCorporation1")))
    assertSelected(beginnerGame, setOf(cn("BeginnerCard")))
    assertSelected(beginnerGame, setOf(cn("BeginnerOption")))
  }

  @Test
  internal fun `choosing a colony tile enables its solo setup`() {
    val soloWithoutTileChoice = gameView("", "Player1")
    val soloWithTileChoice = gameView("ColoniesExpansion, Callisto", "Player1")
    val multiplayerWithTileChoice = gameView("ColoniesExpansion, Callisto")

    assertOmitted(soloWithoutTileChoice, setOf(cn("SelectedColonyTile")))
    assertOmitted(soloWithoutTileChoice, setOf(cn("SoloColoniesSetup")))
    assertSelected(soloWithTileChoice, setOf(cn("SelectedColonyTile")))
    assertSelected(soloWithTileChoice, setOf(cn("SoloColoniesSetup")))
    assertOmitted(multiplayerWithTileChoice, setOf(cn("SoloColoniesSetup")))
  }

  @Test
  internal fun `two-player games omit SecondPlace`() {
    val view = gameView("", "Green", "Yellow")

    assertSelected(view, setOf(cn("FirstPlace")))
    assertOmitted(view, setOf(cn("SecondPlace")))
  }

  @Test
  internal fun `three-player games include SecondPlace`() {
    val view = gameView("", "Green", "Yellow", "Blue")

    assertSelected(view, setOf(cn("FirstPlace"), cn("SecondPlace")))
  }

  @Test
  internal fun `solo classes stay unselected in multiplayer`() {
    matchingClasses("solo", gameView("PreludeExpansion, VenusNextExpansion")).shouldBeEmpty()
  }

  @Test
  internal fun `award domain and scoring machinery stay uninhabited in solo`() {
    val solo = gameView("", "Player1")
    matchingClasses("award", solo).shouldBeEmpty()
    assertOmitted(solo, setOf(cn("FirstPlace"), cn("SecondPlace")))
  }

  // Maps, goals, and replacements

  @Test
  internal fun `maps are Content whose supporting areas follow the selected map`() {
    val content = Canon.classTable.getClass(cn("Content"))
    val allAreas =
        Canon.marsMapDefinitions.flatMapTo(linkedSetOf()) { map ->
          map.areas.map { it.className }
        }
    assertSoftly {
      Canon.marsMapDefinitions.forEach { map ->
        withClue("map ${map.className}") {
          Canon.classTable.getClass(map.className).isSubtypeOf(content) shouldBe true
          val areas = map.areas.mapTo(linkedSetOf()) { it.className }
          areas.forEach { area ->
            withClue("supporting area $area") {
              Canon.classTable.getClass(area).isSubtypeOf(content) shouldBe false
            }
          }
          assertValidView(map.className.toString()) { view ->
            assertSelected(view, areas)
            assertOmitted(view, allAreas - areas)
          }
        }
      }
    }
  }

  @Test
  internal fun `a map defines its areas and an area can be selected on its own`() {
    val standardGame = gameView("")
    val hellasGame = gameView("HellasMap")
    val gameWithOneHellasArea = gameView("Hellas_1_1")

    assertOmitted(standardGame, setOf(cn("Hellas_1_1")))
    assertSelected(hellasGame, setOf(cn("Hellas_1_1")))
    assertSelected(gameWithOneHellasArea, setOf(cn("Hellas_1_1")))
  }

  @Test
  internal fun `Amazonis changes the rules for the Mars and Venus tracks`() {
    val standardGame = gameView("")
    val venusGame = gameView("VenusNextExpansion")
    val venusOnAmazonis = gameView("VenusNextExpansion, AmazonisMap")

    assertSelected(standardGame, setOf(cn("StandardGpTrackRules")))
    assertOmitted(standardGame, setOf(cn("ExtendedGlobalParametersRule")))
    assertSelected(venusGame, setOf(cn("StandardVenusTrackRules")))
    assertOmitted(venusGame, setOf(cn("ExtendedVenusTrackRules")))
    assertOmitted(venusOnAmazonis, setOf(cn("StandardGpTrackRules")))
    assertSelected(venusOnAmazonis, setOf(cn("ExtendedGlobalParametersRule")))
    assertOmitted(venusOnAmazonis, setOf(cn("StandardVenusTrackRules")))
    assertSelected(venusOnAmazonis, setOf(cn("ExtendedVenusTrackRules")))
  }

  @Test
  internal fun `the promo pack selects the new Deimos Down instead of the original`() {
    val standardGame = gameView("")
    val promoGame = gameView("promos:cards")

    assertSelected(standardGame, setOf(cn("DeimosDown")))
    assertOmitted(standardGame, setOf(cn("DeimosDownPromo")))
    assertOmitted(promoGame, setOf(cn("DeimosDown")))
    assertSelected(promoGame, setOf(cn("DeimosDownPromo")))
  }

  @Test
  internal fun `a map milestone needing Turmoil waits for both options`() {
    val amazonisGame = gameView("AmazonisMap")
    val turmoilGame = gameView("TurmoilExpansion")
    val amazonisWithTurmoil = gameView("AmazonisMap, TurmoilExpansion")

    assertOmitted(amazonisGame, setOf(cn("Lobbyist")))
    assertOmitted(turmoilGame, setOf(cn("Lobbyist")))
    assertSelected(amazonisWithTurmoil, setOf(cn("Lobbyist")))
  }

  @Test
  internal fun `an extra milestone is absent until chosen`() {
    val standardGame = gameView("")
    val gameWithBriber = gameView("Briber")

    assertOmitted(standardGame, setOf(cn("Briber")))
    assertSelected(gameWithBriber, setOf(cn("Briber")))
  }

  @Test
  internal fun `Quick Start replaces Elysium's Generalist with Generalist2`() {
    assertValidView("ElysiumMap, QuickStartVariant") { view ->
      assertSelected(view, setOf(cn("Generalist2")))
      assertOmitted(view, setOf(cn("Generalist")))
    }
  }

  @Test
  internal fun `Without Quick Start Elysium selects Generalist without Generalist2`() {
    assertValidView("ElysiumMap") { view ->
      assertSelected(view, setOf(cn("Generalist")))
      assertOmitted(view, setOf(cn("Generalist2")))
    }
  }

  @Test
  internal fun `Unselected milestones omit their support`() {
    assertValidView("Builder, Legend, Merchant") { view ->
      assertOmitted(
          view,
          setOf(
              cn("Hydrologist"),
              cn("HydrologistCredit"),
              cn("HydrologistWatcher"),
              cn("Diversifier"),
              cn("DiversifierWatcher"),
          ),
      )
    }
  }

  @Test
  internal fun `Hellas selects Diversifier and its allocation watcher together`() {
    assertValidView("HellasMap") { view ->
      assertSelected(view, setOf(cn("Diversifier"), cn("DiversifierWatcher")))
    }
  }

  // Eligibility across expansions

  @Test
  internal fun `Prelude 2 cards needing Venus wait for Venus Next`() {
    val standardGame = gameView("")
    val prelude2Game = gameView("prelude2:cards")
    val prelude2WithVenus = gameView("prelude2:cards, VenusNextExpansion")

    assertOmitted(standardGame, setOf(cn("Ecotec")))
    assertSelected(prelude2Game, setOf(cn("Ecotec")))
    assertOmitted(prelude2Game, setOf(cn("AtmosphericEnhancers")))
    assertSelected(prelude2WithVenus, setOf(cn("AtmosphericEnhancers")))
  }

  @Test
  internal fun `Promo global events are absent without the promo pack`() {
    assertValidView("TurmoilExpansion, VenusNextExpansion, ColoniesExpansion, Callisto") { view ->
      assertOmitted(
          view,
          setOf(cn("MartianProtectionism"), cn("JovianColonyBoom"), cn("TitanVenusAlliance")),
      )
    }
  }

  @Test
  internal fun `Venus global events require Venus even with the promo pack`() {
    assertValidView("TurmoilExpansion, promos:cards, ColoniesExpansion, Callisto") { view ->
      assertSelected(view, setOf(cn("JovianColonyBoom")))
      assertOmitted(view, setOf(cn("MartianProtectionism"), cn("TitanVenusAlliance")))
    }
  }

  @Test
  internal fun `Colony global events require Colonies even with Venus and the promo pack`() {
    assertValidView("TurmoilExpansion, promos:cards, VenusNextExpansion") { view ->
      assertSelected(view, setOf(cn("MartianProtectionism")))
      assertOmitted(view, setOf(cn("JovianColonyBoom"), cn("TitanVenusAlliance")))
    }
  }

  @Test
  internal fun `Both expansions make the combined Venus colony events available`() {
    assertValidView(
        "TurmoilExpansion, promos:cards, VenusNextExpansion, ColoniesExpansion, Callisto"
    ) { view ->
      assertSelected(view, setOf(cn("TitanVenusAlliance"), cn("ImmigrationToVenus")))
    }
  }

  @Test
  internal fun `cross-bundle Colonies classes stay unselected without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    val bundle = Canon.bundles.single { it.bundleName == "colonies" }
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
  internal fun `cross-bundle prelude Content selects its card back without Prelude rules`() {
    assertSelected(promosUtopiaWithoutCorporateEra, setOf(cn("PreludeCard")))
    assertOmitted(promosUtopiaWithoutCorporateEra, setOf(cn("PreludePhase")))
  }

  @Test
  internal fun `cross-bundle Venus classes stay unselected without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertOmitted(promosCimmeriaWithoutCorporateEra, setOf(cn("VenusStep"), cn("VenusTag")))
  }

  // Supporting declarations

  @Test
  internal fun `a Prelude card brings its card back without the Prelude expansion`() {
    val gameWithPrelude = gameView("AcquiredSpaceAgency")
    val gameWithCorporation = gameView("CheungShingMars")

    assertSelected(gameWithPrelude, setOf(cn("PreludeCard")))
    assertOmitted(gameWithCorporation, setOf(cn("PreludeCard")))
  }

  @Test
  internal fun `shared promo card resource follows either card without the pack`() {
    listOf("PharmacyUnion", "Hospitals").forEach { card ->
      assertValidView(card) { view ->
        assertSelected(view, setOf(cn(card), cn("Disease")))
      }
    }
  }

  @Test
  internal fun `card local instruction follows its card without the pack`() {
    assertValidView("IcyImpactors") { view ->
      assertSelected(view, setOf(cn("IcyImpactors"), cn("ChooseOceanArea")))
    }
  }

  @Test
  internal fun `Mars Nomads marker follows its card without the pack`() {
    assertValidView("MarsNomads") { view ->
      assertSelected(view, setOf(cn("MarsNomads"), cn("NomadsMarker")))
    }
    assertValidView("promos:cards, -MarsNomads") { view ->
      assertOmitted(view, setOf(cn("MarsNomads"), cn("NomadsMarker")))
    }
  }

  // Modules, packs, and explicit choices

  @Test
  internal fun `modules normally select their intrinsic rules and associated Content`() {
    assertSoftly {
      examples.forEach { example ->
        assertValidView(example.moduleConfig) { view ->
          assertSelected(
              view,
              setOf(example.module) +
                  example.otherModuleSelections +
                  example.privateSupport +
                  example.standaloneContent +
                  example.moduleDependentContent,
          )
        }
      }
    }
  }

  @Test
  internal fun `Prelude cards can be selected without Prelude rules`() {
    assertValidView("prelude:cards") { view ->
      assertSelected(
          view,
          preludeCards + cn("PreludeCard"),
      )
      assertOmitted(view, setOf(cn("PreludeExpansion"), cn("PreludePhase")))
    }
  }

  @Test
  internal fun `Prelude rules can exclude their card pack while explicit cards override that exclusion`() {
    assertValidView("PreludeExpansion, -prelude:cards") { view ->
      assertSelected(view, setOf(cn("PreludeExpansion"), cn("PreludePhase"), cn("PreludeCard")))
      assertOmitted(
          view,
          preludeCards,
      )
    }
    preludeCards.forEach { card ->
      assertValidView("PreludeExpansion, -prelude:cards, $card") { view ->
        assertSelected(view, setOf(card))
      }
    }
  }

  @Test
  internal fun `explicit inclusion agrees with an already selected module or pack`() {
    listOf(
            "PreludeExpansion, Vitor" to "Vitor",
            "prelude:cards, Vitor" to "Vitor",
            "VenusNextExpansion, Manutech" to "Manutech",
        )
        .forEach { (config, card) ->
          assertValidView(config) { view -> assertSelected(view, setOf(cn(card))) }
        }
  }

  @Test
  internal fun `individual card exclusions also apply to a standalone pack`() {
    assertValidView("prelude:cards, -Vitor") { view ->
      assertOmitted(view, setOf(cn("Vitor"), cn("NonNegativeIconsOf")))
    }
  }

  @Test
  internal fun `explicitly excluded Content and its private support stay unselected`() {
    assertSoftly {
      examples.forEach { example ->
        assertValidView("${example.moduleConfig}, -${example.excludedContent}") { view ->
          assertOmitted(view, setOf(example.excludedContent) + example.privateSupport)
        }
      }
    }
  }

  @Test
  internal fun `explicitly included compatible Content needs no module or bundle selection`() {
    assertSoftly {
      examples.forEach { example ->
        example.standaloneContent.forEach { content ->
          assertValidView(content.toString()) { view -> assertSelected(view, setOf(content)) }
        }
      }
    }
  }

  @Test
  internal fun `explicitly included dependent Content requires its module`() {
    assertSoftly {
      examples.forEach { example ->
        example.moduleDependentContent.forEach { content ->
          assertInvalidView(content.toString())
          assertValidView("${example.moduleConfig}, $content") { view ->
            assertSelected(view, setOf(content))
          }
        }
      }
    }
  }

  @Test
  internal fun `Turmoil global events are individual Content with a hard Module dependency`() {
    assertInvalidView("DemocraticReform")
    assertValidView("TurmoilExpansion, -DemocraticReform") { view ->
      assertSelected(view, setOf(cn("TurmoilExpansion"), cn("GlobalEvent")))
      assertOmitted(view, setOf(cn("DemocraticReform"), cn("AquiferReleasedByPublicCouncil")))
    }
    assertValidView("TurmoilExpansion, -MinimalImpactPolicy") { view ->
      assertOmitted(
          view,
          setOf(
              cn("MinimalImpactPolicy"),
              cn("DryDeserts"),
          ),
      )
    }
  }

  @Test
  internal fun `Turmoil rules and events remain when its cards are individually excluded`() {
    val bundle = "turmoil"
    val declaredNames =
        Canon.bundles
            .single { it.bundleName == bundle }
            .explicitClassDeclarations
            .mapTo(hashSetOf()) { it.className }
    val cards = Canon.cards.map { it.className }.filterTo(linkedSetOf(), declaredNames::contains)
    cards.containsAll(setOf(cn("AerialLenses"), cn("LakefrontResorts"))) shouldBe true

    // A narrower pool choice is resolved into individual exclusions before premise creation.
    val config = (listOf("TurmoilExpansion") + cards.map { "-$it" }).joinToString(", ")
    assertValidView(config) { view ->
      assertSelected(
          view,
          setOf(cn("TurmoilExpansion"), cn("Party"), cn("GlobalEvent"), cn("DemocraticReform")),
      )
      assertOmitted(view, cards)
    }
  }

  @Test
  internal fun `compatible Turmoil corporations can be selected together without its rules`() {
    val corporations = examples.single { it.module == cn("TurmoilExpansion") }.standaloneContent
    assertValidView(corporations.joinToString(", ")) { view ->
      assertSelected(view, corporations)
      assertOmitted(
          view,
          setOf(
              cn("TurmoilExpansion"),
              cn("Party"),
              cn("GlobalEvent"),
              cn("AerialLenses"),
              cn("DemocraticReform"),
          ),
      )
    }
  }

  private fun assertValidView(config: String, assertions: (GameView) -> Unit) {
    val result = runCatching { gameView(config).also { it.classTable } }
    withClue("configuration [$config] should produce a game view") {
      result.exceptionOrNull() shouldBe null
    }
    result.getOrNull()?.let(assertions)
  }

  private fun assertInvalidView(config: String) {
    withClue("configuration [$config] should reject a hard Module dependency") {
      shouldThrow<InvalidGameConfigException> {
        gameView(config).classTable
      }
    }
  }

  private fun assertSelected(view: GameView, classNames: Set<ClassName>) {
    classNames.forEach { className ->
      withClue("$className should be known, selected, and inhabited in [${view.config}]") {
        Canon.allClassNames.contains(className) shouldBe true
        view.classNames.contains(className) shouldBe true
        view.classTable.isInhabited(className) shouldBe true
      }
    }
  }

  private fun assertOmitted(view: GameView, classNames: Set<ClassName>) {
    classNames.forEach { className ->
      withClue("$className should be known but unselected and uninhabited in [${view.config}]") {
        Canon.allClassNames.contains(className) shouldBe true
        view.classNames.contains(className) shouldBe false
        view.classTable.isInhabited(className) shouldBe false
      }
    }
  }

  private fun matchingClasses(pattern: String, gameView: GameView): List<ClassName> =
      gameView.classTable
          .allClasses()
          .map { it.className }
          .filter { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(it.toString()) }

  private class GameView(val config: GameConfig) {
    val classTable: ClassTable by lazy { Canon.gamePremise(config).classTable }
    val classNames: Set<ClassName> by lazy { classTable.allClassNames }
  }

  private class ModuleContentExample(
      val module: ClassName,
      val moduleConfig: String = module.toString(),
      val otherModuleSelections: Set<ClassName>,
      val standaloneContent: Set<ClassName> = emptySet(),
      val moduleDependentContent: Set<ClassName> = emptySet(),
      val excludedContent: ClassName,
      val privateSupport: Set<ClassName> = emptySet(),
  )

  private val examples =
      listOf(
          ModuleContentExample(
              module = cn("PreludeExpansion"),
              otherModuleSelections = setOf(cn("PreludePhase"), cn("PreludeCard")),
              standaloneContent =
                  setOf(
                      cn("ValleyTrust"),
                      cn("Vitor"),
                      cn("AcquiredSpaceAgency"),
                      cn("HousePrinting"),
                  ),
              excludedContent = cn("Vitor"),
              privateSupport = setOf(cn("NonNegativeIconsOf")),
          ),
          ModuleContentExample(
              module = cn("VenusNextExpansion"),
              otherModuleSelections = setOf(cn("VenusStep"), cn("AirScrappingProject")),
              standaloneContent = setOf(cn("Manutech")),
              excludedContent = cn("Manutech"),
          ),
          ModuleContentExample(
              module = cn("ColoniesExpansion"),
              moduleConfig = "ColoniesExpansion, Callisto",
              otherModuleSelections =
                  setOf(cn("TradeAction"), cn("BuildColonyProject"), cn("Callisto")),
              standaloneContent = setOf(cn("Arklight"), cn("RefugeeCamps")),
              moduleDependentContent = setOf(cn("CryoSleep")),
              excludedContent = cn("RefugeeCamps"),
              privateSupport = setOf(cn("Camp")),
          ),
          ModuleContentExample(
              module = cn("TurmoilExpansion"),
              otherModuleSelections = setOf(cn("GlobalEvent"), cn("Party"), cn("DemocraticReform")),
              standaloneContent =
                  setOf(
                      cn("LakefrontResorts"),
                      cn("Pristar"),
                      cn("TerraLabsResearch"),
                      cn("UtopiaInvest"),
                  ),
              moduleDependentContent = setOf(cn("AerialLenses")),
              excludedContent = cn("AerialLenses"),
          ),
      )

  private val preludeCards: Set<ClassName>
    get() = examples.single { it.module == cn("PreludeExpansion") }.standaloneContent

  // Compiled game views belong to this test instance, not the test worker's lifetime.
  private val promosUtopiaWithoutCorporateEra =
      gameView(
          "promos:cards, UtopiaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )
  private val promosCimmeriaWithoutCorporateEra =
      gameView(
          "promos:cards, CimmeriaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )

  private fun gameView(config: String, vararg playerNames: String): GameView =
      GameView(
          GameConfig(
              config,
              *(playerNames.takeIf { it.isNotEmpty() } ?: arrayOf("Player1", "Player2")),
          )
      )
}
