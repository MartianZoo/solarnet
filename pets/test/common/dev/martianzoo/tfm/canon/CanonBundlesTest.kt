package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  internal fun incompatibleCardsAreRejectedWithoutStartingAWorld() {
    listOf(
            "Merger, SagittaFrontierServices",
            "SagittaFrontierServices, Merger",
            "promos:cards, SagittaFrontierServices",
        )
        .forEach {
          val failure =
              shouldThrow<InvalidGameConfigException> {
                Canon.gamePremise(GameConfig("prelude2:cards, $it", "Player1", "Player2"))
                    .classTable
              }
          failure.message.orEmpty() shouldContain "Merger"
          failure.message.orEmpty() shouldContain "SagittaFrontierServices"
        }
  }

  @Test
  internal fun eitherIncompatibleCardCanBeSelectedAloneAndUnsafeAllowsBoth() {
    listOf("Merger", "SagittaFrontierServices").forEach { card ->
      val table =
          Canon.gamePremise(GameConfig("prelude2:cards, $card", "Player1", "Player2")).classTable
      table.isInhabited(cn(card)) shouldBe true
    }
    val unsafe =
        Canon.gamePremise(
                GameConfig(
                    "prelude2:cards, Merger, SagittaFrontierServices, Unsafe",
                    "Player1",
                    "Player2",
                )
            )
            .classTable
    unsafe.isInhabited(cn("Merger")) shouldBe true
    unsafe.isInhabited(cn("SagittaFrontierServices")) shouldBe true
  }

  @Test
  internal fun copiedPremisesAndComposedCatalogsRetainIncompatibilityChecks() {
    val ordinary = Canon.gamePremise(GameConfig("prelude2:cards", "Player1", "Player2"))
    val altered =
        ordinary.copy(
            catalog = Catalog(Canon),
            classSelections =
                ordinary.classSelections
                    .filterNot { it.className == cn("PreservationProgram") }
                    .toSet() + ClassSelection(cn("PreservationProgram")),
        )
    shouldThrow<InvalidGameConfigException> { altered.classTable }.message.orEmpty() shouldContain
        "PreservationProgram"
  }

  @Test
  internal fun manifestListsEveryCanonBundleDirectory() {
    val resourceBundleNames =
        CanonResources.filenames("bundles")
            .filter { '/' in it }
            .mapTo(linkedSetOf()) { it.substringBefore('/') }

    CanonResources.bundleModules.keys shouldBe resourceBundleNames
  }

  @Test
  internal fun eachMapBundleNamesItsMapAsPrimaryModule() {
    val mismatchedBundles =
        Canon.bundles
            .filter { it.marsMapDefinitions.isNotEmpty() }
            .filter { bundle ->
              bundle.primaryModuleName != bundle.marsMapDefinitions.single().className
            }
            .map(Bundle::bundleName)

    mismatchedBundles shouldBe emptyList()
  }

  @Test
  internal fun beginnerCorporationsRequireTheBeginnerVariant() {
    val standard = table()
    val beginner = table(cn("BeginnerVariant"))
    val beginnerCorporations = (1..5).map { cn("BeginnerCorporation$it") }

    listOf(cn("BeginnerVariant"), cn("BeginnerOption"), cn("BeginnerCard")).forEach { className ->
      standard.isInhabited(className) shouldBe false
      beginner.isInhabited(className) shouldBe true
    }
    beginnerCorporations.forEach { corporation ->
      standard.isInhabited(corporation) shouldBe false
      (corporation in standard.allClassNames) shouldBe false
      beginner.isInhabited(corporation) shouldBe true
    }
  }

  @Test
  internal fun modulesInOneBundleRemainIndependent() {
    val utopia = table(cn("UtopiaMap"))
    val cimmeria = table(cn("CimmeriaMap"))

    utopia.isInhabited(cn("UtopiaMap")) shouldBe true
    utopia.isInhabited(cn("CimmeriaMap")) shouldBe false
    (cn("CimmeriaMap") in utopia.allClassNames) shouldBe false
    cimmeria.isInhabited(cn("CimmeriaMap")) shouldBe true
    cimmeria.isInhabited(cn("UtopiaMap")) shouldBe false
    (cn("UtopiaMap") in cimmeria.allClassNames) shouldBe false
    utopia.isInhabited(cn("CimmeriaPlacementBonus")) shouldBe false
    (cn("CimmeriaPlacementBonus") in utopia.allClassNames) shouldBe false
    cimmeria.isInhabited(cn("CimmeriaPlacementBonus")) shouldBe true
  }

  @Test
  internal fun independentColoniesCardCanBeSelectedWithoutColonies() {
    table(cn("Arklight")).isInhabited(cn("Arklight")) shouldBe true
  }

  @Test
  internal fun coloniesCardsCanBeSelectedWithoutColoniesRules() {
    val cards = Canon.gamePremise(GameConfig("colonies:cards", "Player1", "Player2")).classTable

    cards.isInhabited(cn("Arklight")) shouldBe true
    cards.isInhabited(cn("ColoniesExpansion")) shouldBe false
    cards.isInhabited(cn("CryoSleep")) shouldBe false
  }

  @Test
  internal fun coloniesCardPoolCanBeExcludedWhileUsingColoniesRules() {
    val rulesOnly =
        Canon.gamePremise(
                GameConfig("ColoniesExpansion, Callisto, -colonies:cards", "Player1", "Player2")
            )
            .classTable

    rulesOnly.isInhabited(cn("ColoniesExpansion")) shouldBe true
    rulesOnly.isInhabited(cn("Arklight")) shouldBe false
    rulesOnly.isInhabited(cn("CryoSleep")) shouldBe false
  }

  @Test
  internal fun venusAndTurmoilCardsCanBeSelectedWithoutTheirRules() {
    val venus = table(includedCardBundles = setOf("venus"))
    val turmoil = table(includedCardBundles = setOf("turmoil"))

    venus.isInhabited(cn("Manutech")) shouldBe true
    venus.isInhabited(cn("VenusNextExpansion")) shouldBe false
    venus.isInhabited(cn("AerosportTournament")) shouldBe false
    turmoil.isInhabited(cn("Pristar")) shouldBe true
    turmoil.isInhabited(cn("TurmoilExpansion")) shouldBe false
  }

  @Test
  internal fun venusAndTurmoilRulesCanOmitTheirCards() {
    val venus =
        Canon.gamePremise(GameConfig("VenusNextExpansion, -venus:cards", "Player1", "Player2"))
            .classTable
    val turmoil =
        Canon.gamePremise(GameConfig("TurmoilExpansion, -turmoil:cards", "Player1", "Player2"))
            .classTable

    venus.isInhabited(cn("VenusNextExpansion")) shouldBe true
    venus.isInhabited(cn("Manutech")) shouldBe false
    venus.isInhabited(cn("Hoverlord")) shouldBe false
    turmoil.isInhabited(cn("TurmoilExpansion")) shouldBe true
    turmoil.isInhabited(cn("Pristar")) shouldBe false
  }

  @Test
  internal fun explicitlyNamedImpossibleVenusContentStillFails() {
    shouldThrow<InvalidGameConfigException> {
      table(cn("AerosportTournament"))
    }
    shouldThrow<InvalidGameConfigException> {
      Canon.gamePremise(
              GameConfig(
                  "VenusNextExpansion, -venus:cards, Hoverlord",
                  "Player1",
                  "Player2",
              )
          )
          .classTable
    }
  }

  @Test
  internal fun preludeCardPoolsAreIndependentOfPreludeRules() {
    val first = Canon.gamePremise(GameConfig("prelude:cards", "Player1", "Player2")).classTable
    val second = Canon.gamePremise(GameConfig("prelude2:cards", "Player1", "Player2")).classTable
    val rules = Canon.gamePremise(GameConfig("PreludeExpansion", "Player1", "Player2")).classTable
    val rulesWithoutCards =
        Canon.gamePremise(GameConfig("PreludeExpansion, -prelude:cards", "Player1", "Player2"))
            .classTable

    first.isInhabited(cn("Vitor")) shouldBe true
    first.isInhabited(cn("PreludePhase")) shouldBe false
    second.isInhabited(cn("SpaceLanes")) shouldBe true
    second.isInhabited(cn("PreludePhase")) shouldBe false
    rules.isInhabited(cn("Vitor")) shouldBe true
    rulesWithoutCards.isInhabited(cn("PreludePhase")) shouldBe true
    rulesWithoutCards.isInhabited(cn("Vitor")) shouldBe false
  }

  @Test
  internal fun drawingPreludeCardsDoesNotSelectAPreludePool() {
    val valleyTrust = Canon.gamePremise(GameConfig("ValleyTrust", "Player1", "Player2")).classTable
    val wgProject =
        Canon.gamePremise(
                GameConfig("TerraformingMars, TurmoilExpansion, WgProject", "Player1", "Player2")
            )
            .classTable
    val selected =
        Canon.gamePremise(GameConfig("ValleyTrust, prelude2:cards", "Player1", "Player2"))
            .classTable

    valleyTrust.isInhabited(cn("ValleyTrust")) shouldBe true
    valleyTrust.isInhabited(cn("Vitor")) shouldBe false
    wgProject.isInhabited(cn("WgProject")) shouldBe true
    wgProject.isInhabited(cn("Vitor")) shouldBe false
    selected.isInhabited(cn("SpaceLanes")) shouldBe true
    selected.isInhabited(cn("Vitor")) shouldBe false
  }

  @Test
  internal fun coloniesDependentCardsCannotBeSelectedWithoutColonies() {
    // These cover an observed standard action, a fleet gain instruction, and an optional trade.
    listOf("CryoSleep", "SkyDocks", "TitanFloatingLaunchPad").forEach { cardName ->
      shouldThrow<InvalidGameConfigException> { table(cn(cardName)) }
    }
    table(cn("EcologyResearch")).isInhabited(cn("EcologyResearch")) shouldBe true
  }

  @Test
  internal fun otherCardPacksDeriveColoniesCompatibilityFromTheirInstructions() {
    val promosWithoutColonies = table(cn("PreludeExpansion"), includedCardBundles = setOf("promos"))
    val promosWithColonies =
        table(
            cn("PreludeExpansion"),
            cn("ColoniesExpansion"),
            cn("Callisto"),
            includedCardBundles = setOf("promos"),
        )
    promosWithoutColonies.isInhabited(cn("StrategicBasePlanning")) shouldBe false
    (cn("StrategicBasePlanning") in promosWithoutColonies.allClassNames) shouldBe false
    promosWithColonies.isInhabited(cn("StrategicBasePlanning")) shouldBe true

    val prelude2VenusWithoutColonies =
        table(
            cn("PreludeExpansion"),
            cn("VenusNextExpansion"),
            includedCardBundles = setOf("prelude2"),
        )
    val prelude2VenusWithColonies =
        table(
            cn("PreludeExpansion"),
            cn("VenusNextExpansion"),
            cn("ColoniesExpansion"),
            cn("Callisto"),
            includedCardBundles = setOf("prelude2"),
        )
    prelude2VenusWithoutColonies.isInhabited(cn("VenusTradeHub")) shouldBe false
    (cn("VenusTradeHub") in prelude2VenusWithoutColonies.allClassNames) shouldBe false
    prelude2VenusWithColonies.isInhabited(cn("VenusTradeHub")) shouldBe true

    val prelude2TurmoilOnly =
        table(
            cn("PreludeExpansion"),
            cn("TurmoilExpansion"),
            includedCardBundles = setOf("prelude2"),
        )
    val prelude2ColoniesOnly =
        table(
            cn("PreludeExpansion"),
            cn("ColoniesExpansion"),
            cn("Callisto"),
            includedCardBundles = setOf("prelude2"),
        )
    val prelude2ColoniesAndTurmoil =
        table(
            cn("PreludeExpansion"),
            cn("ColoniesExpansion"),
            cn("Callisto"),
            cn("TurmoilExpansion"),
            includedCardBundles = setOf("prelude2"),
        )
    listOf("ColonialEnvoys", "ColonialRepresentation").forEach { cardName ->
      prelude2TurmoilOnly.isInhabited(cn(cardName)) shouldBe false
      (cn(cardName) in prelude2TurmoilOnly.allClassNames) shouldBe false
      prelude2ColoniesOnly.isInhabited(cn(cardName)) shouldBe false
      (cn(cardName) in prelude2ColoniesOnly.allClassNames) shouldBe false
      prelude2ColoniesAndTurmoil.isInhabited(cn(cardName)) shouldBe true
    }
  }

  @Test
  internal fun secondaryModuleDoesNotEnableItsOwningExpansionBundle() {
    val worldGovernmentOnly = table(cn("WorldGovernmentRule"))

    worldGovernmentOnly.isInhabited(cn("WorldGovernmentRule")) shouldBe true
    worldGovernmentOnly.isInhabited(cn("VenusTag")) shouldBe false
    (cn("VenusTag") in worldGovernmentOnly.allClassNames) shouldBe false
    worldGovernmentOnly.isInhabited(cn("VenusStep")) shouldBe false
    (cn("VenusStep") in worldGovernmentOnly.allClassNames) shouldBe false
  }

  @Test
  internal fun modeConditionalCardsRemainAvailableOutsideThatMode() {
    val premise =
        Canon.gamePremise(
            GameConfig.create(
                setOf(cn("TerraformingMars"), cn("SoloMode"), cn("Vitor")),
                playerNames = listOf(cn("Player1")),
            )
        )
    val solo = premise.classTable

    solo.isInhabited(cn("Vitor")) shouldBe true
    solo.isInhabited(cn("MultiplayerMode")) shouldBe false
    (cn("MultiplayerMode") in solo.allClassNames) shouldBe false
  }

  @Test
  internal fun definitionConditionsUseTheCompleteModuleSelection() {
    val withoutColonies = table(cn("UtopiaMap"))
    val withColonies = table(cn("UtopiaMap"), cn("ColoniesExpansion"), cn("Callisto"))

    withoutColonies.isInhabited(cn("Pioneer3")) shouldBe false
    (cn("Pioneer3") in withoutColonies.allClassNames) shouldBe false
    withColonies.isInhabited(cn("Pioneer3")) shouldBe true
  }

  @Test
  internal fun expansionModuleCanAddDefinitionsToAnotherSelectedMap() {
    val base = table(cn("TharsisMap"))
    val venus = table(cn("TharsisMap"), cn("VenusNextExpansion"))

    base.isInhabited(cn("Hoverlord")) shouldBe false
    (cn("Hoverlord") in base.allClassNames) shouldBe false
    venus.isInhabited(cn("Hoverlord")) shouldBe true
  }

  @Test
  internal fun milestonesAndAwardsContentDoesNotRequireASelectableExpansionModule() {
    Canon.allClassNames.contains(cn("MilestonesAwardsExpansion")) shouldBe false
    Canon.allClassNames.contains(cn("Landscaper")) shouldBe true

    val landscaperWithTharsis =
        table(cn("TharsisMap"), cn("Landscaper"), cn("Administrator"), cn("Biologist"))
    landscaperWithTharsis.isInhabited(cn("Landscaper")) shouldBe true
    landscaperWithTharsis.isInhabited(cn("TileInLargestGroup")) shouldBe true
  }

  @Test
  internal fun promoReplacementDefaultsYieldToExplicitOriginalCardSelection() {
    val relevant =
        setOf(
            cn("DeimosDown"),
            cn("GreatDam"),
            cn("MagneticFieldGenerators"),
            cn("DeimosDownPromo"),
            cn("GreatDamPromo"),
            cn("MagneticFieldGeneratorsPromo"),
        )
    val withoutPromos = table(cn("TharsisMap"), cn("CorporateEraExpansion"))
    val withPromos =
        table(cn("TharsisMap"), cn("CorporateEraExpansion"), includedCardBundles = setOf("promos"))
    val withPromosAndGreatDam =
        table(
            cn("TharsisMap"),
            cn("CorporateEraExpansion"),
            cn("GreatDam"),
            includedCardBundles = setOf("promos"),
        )

    relevant.filterTo(linkedSetOf(), withoutPromos::isInhabited) shouldBe
        setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    relevant.filterTo(linkedSetOf(), withoutPromos.allClassNames::contains) shouldBe
        setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    relevant.filterTo(linkedSetOf(), withPromos::isInhabited) shouldBe
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    relevant.filterTo(linkedSetOf(), withPromos.allClassNames::contains) shouldBe
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    val greatDamVersions = setOf(cn("GreatDam"), cn("GreatDamPromo"))
    listOf(
            withoutPromos to setOf(cn("GreatDam")),
            withPromos to setOf(cn("GreatDamPromo")),
            withPromosAndGreatDam to greatDamVersions,
        )
        .forEach { (table, expected) ->
          greatDamVersions.filterTo(linkedSetOf(), table::isInhabited) shouldBe expected
          greatDamVersions.filterTo(linkedSetOf(), table.allClassNames::contains) shouldBe expected
        }
    Canon.allClassNames.containsAll(relevant) shouldBe true
  }

  @Test
  internal fun standardFormBundleCombinesEveryPetsSource() {
    val bundle =
        StandardFormBundle(
            name = "split",
            resourceDirectory = "split",
            resourceFilenames = setOf("game.pets", "scoring.pets"),
            resourceReader = { filename ->
              when (filename) {
                "split/game.pets" -> "CLASS GameClass"
                "split/scoring.pets" -> "CLASS ScoringClass"
                else -> error("Unexpected resource: $filename")
              }
            },
        )

    bundle.explicitClassDeclarations.map { it.className }.toSet() shouldBe
        setOf(cn("GameClass"), cn("ScoringClass"))
  }

  @Test
  internal fun standardFormBundleLoadsMapDefinitionFromPetsComment() {
    val bundle =
        StandardFormBundle(
            name = "maps",
            resourceDirectory = "maps",
            resourceFilenames = setOf("map.pets"),
            resourceReader = {
              """
              CLASS DemoMap : MarsMap
              // The map areas below are code-generated based on the following comment
              //
              // L
              //
              CLASS Demo_1_1 : LandArea { row = 1; column = 1 }
              """
                  .trimIndent()
            },
        )

    (cn("Maps") in bundle.allClassNames) shouldBe false
    bundle.marsMapDefinitions.single().className shouldBe cn("DemoMap")
  }

  @Test
  internal fun standardFormBundleLoadsLanguageFiles() {
    val bundle =
        StandardFormBundle(
            name = "localized",
            resourceDirectory = "localized",
            resourceFilenames = setOf("content.pets", "language/en.json5"),
            resourceReader = { filename ->
              when (filename) {
                "localized/content.pets" -> "CLASS Example"
                "localized/language/en.json5" -> """{ Example: "Example name" }"""
                else -> error("Unexpected resource: $filename")
              }
            },
        )

    bundle.displayNamesByLanguage shouldBe mapOf("en" to mapOf(cn("Example") to "Example name"))
  }

  private fun table(
      vararg selectedModules: ClassName,
      includedCardBundles: Set<String> = emptySet(),
  ): ClassTable {
    val included =
        linkedSetOf(
            *selectedModules,
            cn("TerraformingMars"),
        )
    return Canon.gamePremise(
            GameConfig.create(
                included,
                playerNames = listOf(cn("Player1"), cn("Player2")),
                includedCardBundles = includedCardBundles,
            )
        )
        .classTable
  }
}
