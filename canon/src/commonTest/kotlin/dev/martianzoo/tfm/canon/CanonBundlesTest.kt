package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
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
  fun expansionVocabularyComesOnlyFromItsExpansionBundle() {
    val base = setOf(cn("TerraformingMars"), cn("TharsisMap"))
    val expansionVocabulary =
        mapOf(
            cn("VenusNextExpansion") to setOf(cn("VenusStep"), cn("VenusTag")),
            cn("PreludeExpansion") to
                setOf(cn("PreludeCard"), cn("PreludePhase"), cn("PreludeSetup")),
        )

    val baseRuleset = Canon.resolve(base)
    expansionVocabulary.values.flatten().forEach { vocabulary ->
      baseRuleset.allClassNames.contains(vocabulary) shouldBe false
    }

    expansionVocabulary.forEach { (bundle, vocabulary) ->
      val ruleset = Canon.resolve(base + bundle)
      vocabulary.forEach { className ->
        ruleset.classDeclarationBundles.getValue(className) shouldBe setOf(bundle)
      }
    }
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
  fun promosReplaceThreeBaseGameCards() {
    val originals = setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
    val replacements =
        setOf(cn("DeimosDownPromo"), cn("GreatDamPromo"), cn("MagneticFieldGeneratorsPromo"))
    val relevantCards = originals + replacements

    val withoutPromos =
        Canon.fromOptionCodes("BM", 2).ruleset.cardDefinitions.mapTo(mutableSetOf()) {
          it.className
        }
    val withPromos =
        Canon.fromOptionCodes("BMX", 2).ruleset.cardDefinitions.mapTo(mutableSetOf()) {
          it.className
        }

    withoutPromos.intersect(relevantCards) shouldBe originals
    withPromos.intersect(relevantCards) shouldBe replacements
  }

  @Test
  fun coloniesRulesetOwnsItsVocabularyAndDefinitions() {
    val colonies = Canon.bundles.single { it.bundleName == cn("ColoniesExpansion") }

    Canon.classDeclaration(cn("ColonyTile")) shouldBe colonies.classDeclaration(cn("ColonyTile"))
  }

  @Test
  fun standardFormBundleWithoutPetsDoesNotSynthesizeAComponent() {
    val bundle =
        StandardFormBundle(
            name = "TharsisMap",
            areaShortNamePrefix = "M",
            resourceDirectory = "bundles/TharsisMap",
            resourceFilenames = setOf(StandardFormBundle.MAPS_FILENAME),
        )

    (cn("TharsisMap") in bundle.allClassNames) shouldBe false
    bundle.marsMapDefinitions.single().className shouldBe cn("Tharsis")
  }

  @Test
  fun resolvingRulesetDoesNotReadUnselectedBundleResources() {
    var unselectedReads = 0
    val selected =
        StandardFormBundle(
            name = "SelectedBundle",
            resourceDirectory = "selected",
            resourceFilenames = setOf("selected.pets"),
            resourceReader = { "CLASS SelectedOption" },
        )
    val unselected =
        StandardFormBundle(
            name = "UnselectedBundle",
            resourceDirectory = "unselected",
            resourceFilenames = setOf("unselected.pets"),
            resourceReader = {
              unselectedReads++
              "CLASS UnselectedOption"
            },
        )

    TfmRuleset.compose(selected, unselected).resolve(setOf(cn("SelectedBundle"))).allClassNames

    unselectedReads shouldBe 0
  }
}
