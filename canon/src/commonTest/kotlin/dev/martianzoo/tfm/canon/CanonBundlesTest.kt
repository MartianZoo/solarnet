package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.bundles.ColoniesExpansion.ColoniesExpansion
import dev.martianzoo.tfm.canon.bundles.System.System
import dev.martianzoo.tfm.canon.bundles.TerraformingMars.TerraformingMars
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun canonComposesEveryCanonicalBundle() {
    Canon.bundleRulesets.map { it.bundleName.toString() } shouldContainExactly
        listOf(
            "System",
            "TerraformingMars",
            "CorporateEraExpansion",
            "TharsisMap",
            "HellasMap",
            "ElysiumMap",
            "VenusNextExpansion",
            "PreludeExpansion",
            "ColoniesExpansion",
            "SoloMode",
            "PromosExpansion",
            "TurmoilExpansion",
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
  fun systemRulesetOwnsTheSystemDeclarations() {
    Canon.rulesets.shouldContain(System)
    Canon.classDeclaration(COMPONENT) shouldBe System.classDeclaration(COMPONENT)
  }

  @Test
  fun terraformingMarsRulesetOwnsTheCoreGameDeclarations() {
    Canon.rulesets.shouldContain(TerraformingMars)
    Canon.classDeclaration(cn("TerraformingMars")) shouldBe
        TerraformingMars.classDeclaration(cn("TerraformingMars"))
  }

  @Test
  fun coloniesRulesetOwnsItsVocabularyAndDefinitions() {
    Canon.rulesets.shouldContain(ColoniesExpansion)
    Canon.classDeclaration(cn("ColonyTile")) shouldBe
        ColoniesExpansion.classDeclaration(cn("ColonyTile"))
    Canon.colonyTileDefinitions shouldBe ColoniesExpansion.colonyTileDefinitions
  }
}
