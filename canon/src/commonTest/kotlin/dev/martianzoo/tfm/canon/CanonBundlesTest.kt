package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.api.TfmRuleset
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun hellasAndElysiumAreSeparateOptionsFromOneBundle() {
    val bundles = setOf(cn("TerraformingMars"), cn("HellasElysiumExpansion"))

    val hellas = Canon.resolve(bundles, setupReader(cn("HellasMapOption")))
    hellas.marsMapDefinitions.single().className shouldBe cn("Hellas")
    hellas.milestoneDefinitions.any { it.shortName == cn("HM1") } shouldBe true
    hellas.milestoneDefinitions.any { it.shortName == cn("EM2") } shouldBe false
    (cn("Diversifier") in hellas.allClassNames) shouldBe true
    (cn("Specialist") in hellas.allClassNames) shouldBe false
    (cn("Cultivator") in hellas.allClassNames) shouldBe true
    (cn("Celebrity") in hellas.allClassNames) shouldBe false

    val elysium = Canon.resolve(bundles, setupReader(cn("ElysiumMapOption")))
    elysium.marsMapDefinitions.single().className shouldBe cn("Elysium")
    elysium.milestoneDefinitions.any { it.shortName == cn("EM2") } shouldBe true
    elysium.milestoneDefinitions.any { it.shortName == cn("HM1") } shouldBe false
    (cn("Specialist") in elysium.allClassNames) shouldBe true
    (cn("Diversifier") in elysium.allClassNames) shouldBe false
    (cn("Celebrity") in elysium.allClassNames) shouldBe true
    (cn("Cultivator") in elysium.allClassNames) shouldBe false
  }

  @Test
  fun utopiaAndCimmeriaAreSeparateOptionsFromOneBundle() {
    val bundles = setOf(cn("TerraformingMars"), cn("UtopiaCimmeriaExpansion"))

    val utopia = Canon.resolve(bundles, setupReader(cn("UtopiaPlanitiaMapOption")))
    utopia.marsMapDefinitions.single().className shouldBe cn("UtopiaPlanitia")
    utopia.milestoneDefinitions.any { it.shortName == cn("UM1") } shouldBe true
    utopia.milestoneDefinitions.any { it.shortName == cn("UM2") } shouldBe false
    utopia.milestoneDefinitions.any { it.shortName == cn("IM2") } shouldBe false

    val cimmeria = Canon.resolve(bundles, setupReader(cn("TerraCimmeriaMapOption")))
    cimmeria.marsMapDefinitions.single().className shouldBe cn("TerraCimmeria")
    cimmeria.milestoneDefinitions.any { it.shortName == cn("IM2") } shouldBe true
    cimmeria.milestoneDefinitions.any { it.shortName == cn("UM1") } shouldBe false
  }

  @Test
  fun pioneerRequiresColonies() {
    val utopia = setOf(cn("TerraformingMars"), cn("UtopiaCimmeriaExpansion"))

    Canon.resolve(utopia, setupReader(cn("UtopiaPlanitiaMapOption"))).milestoneDefinitions.any {
      it.className == cn("Pioneer")
    } shouldBe false
    Canon.resolve(
            utopia + cn("ColoniesExpansion"),
            setupReader(cn("UtopiaPlanitiaMapOption"), cn("ColoniesExpansion")),
        )
        .milestoneDefinitions
        .map { it.className } shouldContain cn("Pioneer")
  }

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

    Canon.resolve(base).milestoneDefinitions.any { it.className == cn("Hoverlord") } shouldBe false
    Canon.resolve(base + cn("VenusNextExpansion")).milestoneDefinitions.map {
      it.className
    } shouldContain cn("Hoverlord")
  }

  @Test
  fun planetologistRequiresVenusNext() {
    val terraCimmeria = setOf(cn("TerraformingMars"), cn("UtopiaCimmeriaExpansion"))

    Canon.resolve(terraCimmeria, setupReader(cn("TerraCimmeriaMapOption")))
        .milestoneDefinitions
        .any {
          it.className == cn("Planetologist")
        } shouldBe false
    Canon.resolve(
            terraCimmeria + cn("VenusNextExpansion"),
            setupReader(cn("TerraCimmeriaMapOption"), cn("VenusNextExpansion")),
        )
        .milestoneDefinitions
        .map {
          it.className
        } shouldContain cn("Planetologist")
  }

  @Test
  fun awardsComeFromTheSelectedMapAndExpansions() {
    val base = setOf(cn("TerraformingMars"), cn("TharsisMap"))

    Canon.resolve(base).awardDefinitions.map { it.className }.toSet() shouldBe
        setOf(cn("Landlord"), cn("Banker"), cn("Scientist"), cn("Thermalist"), cn("Miner"))
    Canon.resolve(base + cn("VenusNextExpansion")).awardDefinitions.map {
      it.className
    } shouldContain cn("Venuphile")
  }

  @Test
  fun hellasAndElysiumAwardsFollowTheSelectedMapOption() {
    val bundles = setOf(cn("TerraformingMars"), cn("HellasElysiumExpansion"))

    Canon.resolve(bundles, setupReader(cn("HellasMapOption")))
        .awardDefinitions
        .map {
          it.className
        }
        .toSet() shouldBe
        setOf(
            cn("Cultivator"),
            cn("Magnate"),
            cn("SpaceBaron"),
            cn("Excentric"),
            cn("Contractor"),
        )
    Canon.resolve(bundles, setupReader(cn("ElysiumMapOption")))
        .awardDefinitions
        .map {
          it.className
        }
        .toSet() shouldBe
        setOf(
            cn("Celebrity"),
            cn("Industrialist"),
            cn("DesertSettler"),
            cn("EstateDealer"),
            cn("Benefactor"),
        )
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
    val promos = setOf(cn("TerraformingMars"), cn("PromoCardsExpansion"))

    Canon.resolve(promos, setupReader()).cardDefinitions.any {
      it.className == cn("DoubleDown")
    } shouldBe false
    Canon.resolve(promos + cn("PreludeExpansion"), setupReader(cn("PreludeExpansion")))
        .cardDefinitions
        .map {
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
        Canon.resolve(
                setOf(cn("TerraformingMars"), cn("TharsisMap")),
                setupReader(cn("TharsisMapOption")),
            )
            .cardDefinitions
            .mapTo(mutableSetOf()) {
              it.className
            }
    val withPromos =
        Canon.resolve(
                setOf(cn("TerraformingMars"), cn("TharsisMap"), cn("PromoCardsExpansion")),
                setupReader(cn("TharsisMapOption"), cn("PromoCardPack")),
            )
            .cardDefinitions
            .mapTo(mutableSetOf()) { it.className }

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
            name = "MapProvider",
            resourceDirectory = "bundles/TharsisMap",
            resourceFilenames = setOf(StandardFormBundle.MAPS_FILENAME),
        )

    (cn("MapProvider") in bundle.allClassNames) shouldBe false
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

  private fun setupReader(vararg enabledOptions: ClassName): TypeInfo =
      ExactOptionsState(enabledOptions.toSet())

  private class ExactOptionsState(private val enabledOptions: Set<ClassName>) : TypeInfo {
    override fun has(requirement: Requirement): Boolean = requirement.isMetBy(::count)

    private fun count(metric: Metric): Int {
      require(metric is Count && metric.expression.simple) { "unsupported test metric: $metric" }
      return if (metric.expression.className in enabledOptions) 1 else 0
    }

    override fun isAbstract(e: Expression): Boolean = unused()

    override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = unused()

    private fun unused(): Nothing = error("not needed by setup-requirement tests")
  }
}
