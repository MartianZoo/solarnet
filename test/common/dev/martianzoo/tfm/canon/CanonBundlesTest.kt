package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  internal fun beginnerCorporationsRequireTheBeginnerVariant() {
    val standard = table()
    val beginner = table(cn("BeginnerVariant"))
    val beginnerCorporations = (1..5).map { cn("BeginnerCorporation$it") }

    listOf(cn("BeginnerVariant"), cn("BeginnerCorporationCard")).forEach { className ->
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
  internal fun coloniesDependentCardsCannotBeSelectedWithoutColonies() {
    // These cover an observed standard action, a count, a direct fleet gain, and an optional trade.
    listOf("CryoSleep", "EcologyResearch", "SkyDocks", "TitanFloatingLaunchPad").forEach { cardName
      ->
      shouldThrow<InvalidGameConfigException> { table(cn(cardName)) }
    }
  }

  @Test
  internal fun otherCardPacksDeriveColoniesCompatibilityFromTheirInstructions() {
    val promosWithoutColonies = table(cn("PromoCardPack"), cn("PreludeExpansion"))
    val promosWithColonies =
        table(cn("PromoCardPack"), cn("PreludeExpansion"), cn("ColoniesExpansion"))
    promosWithoutColonies.isInhabited(cn("StrategicBasePlanning")) shouldBe false
    (cn("StrategicBasePlanning") in promosWithoutColonies.allClassNames) shouldBe false
    promosWithColonies.isInhabited(cn("StrategicBasePlanning")) shouldBe true

    val prelude2VenusWithoutColonies =
        table(cn("PreludeExpansion"), cn("Prelude2CardPack"), cn("VenusNextExpansion"))
    val prelude2VenusWithColonies =
        table(
            cn("PreludeExpansion"),
            cn("Prelude2CardPack"),
            cn("VenusNextExpansion"),
            cn("ColoniesExpansion"),
        )
    prelude2VenusWithoutColonies.isInhabited(cn("VenusTradeHub")) shouldBe false
    (cn("VenusTradeHub") in prelude2VenusWithoutColonies.allClassNames) shouldBe false
    prelude2VenusWithColonies.isInhabited(cn("VenusTradeHub")) shouldBe true
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
    val withColonies = table(cn("UtopiaMap"), cn("ColoniesExpansion"))

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
  internal fun contentOnlyProductsDoNotRequireSelectableExpansionModules() {
    Canon.allClassNames.contains(cn("MilestonesAwardsExpansion")) shouldBe false
    Canon.allClassNames.contains(cn("Prelude2Expansion")) shouldBe false
    Canon.allClassNames.contains(cn("Prelude2CardPack")) shouldBe true
    Canon.allClassNames.contains(cn("Landscaper")) shouldBe true

    val landscaperWithTharsis =
        table(cn("TharsisMap"), cn("Landscaper"), cn("Administrator"), cn("Biologist"))
    landscaperWithTharsis.isInhabited(cn("Landscaper")) shouldBe true
    landscaperWithTharsis.isInhabited(cn("TileInLargestGroup")) shouldBe true
  }

  @Test
  internal fun promoModuleReplacesCardsWithoutRemovingEitherFromTheCatalog() {
    val relevant =
        setOf(
            cn("DeimosDown"),
            cn("GreatDam"),
            cn("MagneticFieldGenerators"),
            cn("DeimosDownPromo"),
            cn("GreatDamPromo"),
            cn("MagneticFieldGeneratorsPromo"),
        )
    val withoutPromos = table(cn("TharsisMap"))
    val withPromos = table(cn("TharsisMap"), cn("PromoCardPack"))

    relevant.filterTo(linkedSetOf(), withoutPromos::isInhabited) shouldBe
        setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    relevant.filterTo(linkedSetOf(), withoutPromos.allClassNames::contains) shouldBe
        setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    relevant.filterTo(linkedSetOf(), withPromos::isInhabited) shouldBe
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    relevant.filterTo(linkedSetOf(), withPromos.allClassNames::contains) shouldBe
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    Canon.allClassNames.containsAll(relevant) shouldBe true
  }

  @Test
  internal fun standardFormBundleCombinesEveryPetsSource() {
    val bundle =
        StandardFormBundle(
            name = "SplitBundle",
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
            name = "MapProvider",
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

    (cn("MapProvider") in bundle.allClassNames) shouldBe false
    bundle.marsMapDefinitions.single().className shouldBe cn("DemoMap")
  }

  @Test
  internal fun standardFormBundleLoadsLanguageFiles() {
    val bundle =
        StandardFormBundle(
            name = "LocalizedBundle",
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

  private fun table(vararg selectedModules: ClassName): ClassTable {
    val included =
        linkedSetOf(
            *selectedModules,
            cn("TerraformingMars"),
        )
    return Canon.gamePremise(
            GameConfig.create(included, playerNames = listOf(cn("Player1"), cn("Player2")))
        )
        .classTable
  }
}
