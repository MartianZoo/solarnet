package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun canonComposesEveryCanonicalBundle() {
    Canon.bundles.map { it.bundleName.toString() } shouldContainExactly
        listOf(
            "TerraformingMars",
            "SoloMode",
            "CorporateEraExpansion",
            "TharsisMap",
            "HellasMap",
            "ElysiumMap",
            "VenusNextExpansion",
            "PreludeExpansion",
            "ColoniesExpansion",
            "TurmoilExpansion",
            "PromosExpansion",
        )
  }

  @Test
  fun splittingTheDefinitionCatalogsPreservesAllSupportedContent() {
    Canon.cardDefinitions.size shouldBe 401
    Canon.standardActionDefinitions.size shouldBe 16
    Canon.marsMapDefinitions.size shouldBe 3
    Canon.milestoneDefinitions.size shouldBe 13
    Canon.colonyTileDefinitions.size shouldBe 11
  }

  @Test
  fun systemDeclarationsBelongToPetsRatherThanACanonBundle() {
    Canon.classDeclaration(COMPONENT).className shouldBe COMPONENT
    Canon.classDeclarationBundles.getValue(COMPONENT).shouldBeEmpty()
  }

  @Test
  fun terraformingMarsRulesetOwnsTheCoreGameDeclarations() {
    Canon.rulesets.shouldContain(TerraformingMars)
    Canon.classDeclaration(cn("TerraformingMars")) shouldBe
        TerraformingMars.classDeclaration(cn("TerraformingMars"))
  }

  @Test
  fun resolvedRulesetContainsOnlySelectedBundlesCustomImplementations() {
    val coreCustomClasses =
        Canon.resolve(setOf(cn("TerraformingMars"))).customClasses.map { it.className.toString() }

    coreCustomClasses shouldContainExactly
        listOf(
            "CreateAdjacencies",
            "CheckCardDeck",
            "CheckCardRequirement",
            "HandleCardCost",
            "GetEventVps",
            "PassLeft",
        )
    Canon.resolve(setOf(cn("TerraformingMars"), cn("PreludeExpansion"))).customClasses.map {
      it.className.toString()
    } shouldContain "GainLowestProduction"
  }

  @Test
  fun venusAddsHoverlordToTheMapsFiveMilestones() {
    val base = setOf(cn("TerraformingMars"), cn("TharsisMap"))

    Canon.resolve(base).milestoneDefinitions.shouldHaveSize(5)
    Canon.resolve(base + cn("VenusNextExpansion")).milestoneDefinitions.map {
      it.className
    } shouldContain cn("Hoverlord")
    Canon.resolve(base + cn("VenusNextExpansion")).milestoneDefinitions.shouldHaveSize(6)
  }

  @Test
  fun doubleDownRequiresBothPromosAndPrelude() {
    val promos = setOf(cn("TerraformingMars"), cn("PromosExpansion"))

    Canon.resolve(promos).cardDefinitions.map { it.className }.contains(cn("DoubleDown")) shouldBe
        false
    Canon.resolve(promos + cn("PreludeExpansion")).cardDefinitions.map {
      it.className
    } shouldContain cn("DoubleDown")
  }

  @Test
  fun coloniesRulesetOwnsItsVocabularyAndDefinitions() {
    Canon.rulesets.shouldContain(ColoniesExpansion)
    Canon.classDeclaration(cn("ColonyTile")) shouldBe
        ColoniesExpansion.classDeclaration(cn("ColonyTile"))
    Canon.colonyTileDefinitions shouldBe ColoniesExpansion.colonyTileDefinitions
  }
}
