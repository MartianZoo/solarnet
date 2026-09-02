package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  internal fun modulesInOneBundleRemainIndependent() {
    val utopia = table(cn("UtopiaMap"))
    val cimmeria = table(cn("CimmeriaMap"))

    utopia.isActive(cn("UtopiaMap")) shouldBe true
    utopia.isActive(cn("CimmeriaMap")) shouldBe false
    cimmeria.isActive(cn("CimmeriaMap")) shouldBe true
    cimmeria.isActive(cn("UtopiaMap")) shouldBe false
    utopia.isActive(cn("CimmeriaColonyBonus")) shouldBe false
    cimmeria.isActive(cn("CimmeriaColonyBonus")) shouldBe true
  }

  @Test
  internal fun independentColoniesCardCanBeSelectedWithoutColonies() {
    table(cn("Arklight")).isActive(cn("Arklight")) shouldBe true
  }

  @Test
  internal fun coloniesDependentCardsCannotBeSelectedWithoutColonies() {
    // These cover an observed standard action, a count, a direct fleet gain, and an optional trade.
    listOf("CryoSleep", "EcologyResearch", "SkyDocks", "TitanFloatingLaunchPad").forEach { cardName
      ->
      shouldThrow<IllegalArgumentException> { table(cn(cardName)) }
    }
  }

  @Test
  internal fun otherCardPacksDeriveColoniesCompatibilityFromTheirInstructions() {
    val promosWithoutColonies = table(cn("PromoCardPack"), cn("PreludeExpansion"))
    val promosWithColonies =
        table(cn("PromoCardPack"), cn("PreludeExpansion"), cn("ColoniesExpansion"))
    promosWithoutColonies.isActive(cn("StrategicBasePlanning")) shouldBe false
    promosWithColonies.isActive(cn("StrategicBasePlanning")) shouldBe true

    val prelude2VenusWithoutColonies = table(cn("Prelude2Deck"), cn("VenusNextExpansion"))
    val prelude2VenusWithColonies =
        table(cn("Prelude2Deck"), cn("VenusNextExpansion"), cn("ColoniesExpansion"))
    prelude2VenusWithoutColonies.isActive(cn("VenusTradeHub")) shouldBe false
    prelude2VenusWithColonies.isActive(cn("VenusTradeHub")) shouldBe true
  }

  @Test
  internal fun secondaryModuleDoesNotEnableItsOwningExpansionBundle() {
    val worldGovernmentOnly = table(cn("WorldGovernmentOption"))

    worldGovernmentOnly.isActive(cn("WorldGovernmentOption")) shouldBe true
    worldGovernmentOnly.isActive(cn("VenusTag")) shouldBe false
    worldGovernmentOnly.isActive(cn("VenusStep")) shouldBe false
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
    val solo = ClassTable.forPremise(premise)

    solo.isActive(cn("Vitor")) shouldBe true
    solo.isActive(cn("MultiplayerMode")) shouldBe false
  }

  @Test
  internal fun definitionConditionsUseTheCompleteModuleSelection() {
    val withoutColonies = table(cn("UtopiaMap"))
    val withColonies = table(cn("UtopiaMap"), cn("ColoniesExpansion"))

    withoutColonies.isActive(cn("Pioneer3")) shouldBe false
    withColonies.isActive(cn("Pioneer3")) shouldBe true
  }

  @Test
  internal fun expansionModuleCanAddDefinitionsToAnotherSelectedMap() {
    val base = table(cn("TharsisMap"))
    val venus = table(cn("TharsisMap"), cn("VenusNextExpansion"))

    base.isActive(cn("Hoverlord")) shouldBe false
    venus.isActive(cn("Hoverlord")) shouldBe true
  }

  @Test
  internal fun goalCatalogDoesNotRequireASelectableBundleModule() {
    Canon.allClassNames.contains(cn("MilestonesAwardsExpansion")) shouldBe false
    Canon.allClassNames.contains(cn("Landscaper")) shouldBe true
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

    relevant.filterTo(linkedSetOf(), withoutPromos::isActive) shouldBe
        setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    relevant.filterTo(linkedSetOf(), withPromos::isActive) shouldBe
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    Canon.allClassNames.containsAll(relevant) shouldBe true
  }

  @Test
  internal fun standardFormBundleLoadsMapDefinitionFromPetsComment() {
    val bundle =
        StandardFormBundle(
            name = "MapProvider",
            resourceDirectory = "maps",
            resourceFilenames = setOf("classes.pets"),
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
            resourceFilenames = setOf("classes.pets", "language/en.json5"),
            resourceReader = { filename ->
              when (filename) {
                "localized/classes.pets" -> "CLASS Example"
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
    return ClassTable.forPremise(
        Canon.gamePremise(
            GameConfig.create(included, playerNames = listOf(cn("Player1"), cn("Player2")))
        )
    )
  }
}
