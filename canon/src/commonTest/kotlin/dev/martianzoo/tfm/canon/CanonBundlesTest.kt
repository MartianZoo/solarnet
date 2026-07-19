package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun systemDeclarationsBelongToPetsRatherThanACanonBundle() {
    Canon.classDeclarationBundles.getValue(COMPONENT).shouldBeEmpty()
  }

  @Test
  fun resolvedRulesetIncludesSelectedBundlesCustomImplementations() {
    val coreCustomClasses =
        Canon.resolve(setOf(cn("TerraformingMars"))).customClasses.map { it.className.toString() }

    coreCustomClasses.contains("GainLowestProduction") shouldBe false
    Canon.resolve(setOf(cn("TerraformingMars"), cn("PreludeExpansion"))).customClasses.map {
      it.className.toString()
    } shouldContain "GainLowestProduction"
  }

  @Test
  fun venusAddsHoverlordToTheMapsFiveMilestones() {
    val base = setOf(cn("TerraformingMars"), cn("TharsisMap"))

    Canon.resolve(base).milestoneDefinitions.map { it.className }.contains(cn("Hoverlord")) shouldBe
        false
    Canon.resolve(base + cn("VenusNextExpansion")).milestoneDefinitions.map {
      it.className
    } shouldContain cn("Hoverlord")
  }

  @Test
  fun doubleDownRequiresBothPromosAndPrelude() {
    val promos = setOf(cn("TerraformingMars"), cn("PromoCardsBundle"))

    Canon.resolve(promos).cardDefinitions.map { it.className }.contains(cn("DoubleDown")) shouldBe
        false
    Canon.resolve(promos + cn("PreludeExpansion")).cardDefinitions.map {
      it.className
    } shouldContain cn("DoubleDown")
  }

  @Test
  fun coloniesRulesetOwnsItsVocabularyAndDefinitions() {
    val colonies = Canon.bundles.single { it.bundleName == cn("ColoniesExpansion") }

    Canon.classDeclaration(cn("ColonyTile")) shouldBe colonies.classDeclaration(cn("ColonyTile"))
  }

  @Test
  fun jsonBundleWithoutPetsSynthesizesItsBundleDeclaration() {
    val bundle =
        JsonBundle(
            name = "TharsisMap",
            legacyCode = "M",
            resourceDirectory = "bundles/TharsisMap",
            resourceFilenames = setOf(JsonBundle.MAPS_FILENAME),
        )

    bundle.classDeclaration(cn("TharsisMap")).className shouldBe cn("TharsisMap")
    bundle.marsMapDefinitions.single().className shouldBe cn("Tharsis")
  }
}
