package dev.martianzoo.tfm.canon

import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.types.ClassTable
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun oneAuthorityKnowsEveryMapWhileOneModuleSelectsOneMap() {
    Canon.marsMapDefinitions.map { it.className }.toSet() shouldBe
        setOf(cn("Tharsis"), cn("Hellas"), cn("Elysium"), cn("Utopia"), cn("Cimmeria"))

    val hellas = table(cn("HellasMapOption"))

    hellas.isActive(cn("Hellas")) shouldBe true
    hellas.isActive(cn("Elysium")) shouldBe false
    hellas.isActive(cn("MilestoneHM0")) shouldBe true
    hellas.isActive(cn("MilestoneHM5")) shouldBe false
    hellas.isActive(cn("AwardHA0")) shouldBe true
    hellas.isActive(cn("AwardHA5")) shouldBe false
  }

  @Test
  fun modulesInOneBundleRemainIndependent() {
    val utopia = table(cn("UtopiaPlanitiaMapOption"))
    val cimmeria = table(cn("TerraCimmeriaMapOption"))

    utopia.isActive(cn("Utopia")) shouldBe true
    utopia.isActive(cn("Cimmeria")) shouldBe false
    cimmeria.isActive(cn("Cimmeria")) shouldBe true
    cimmeria.isActive(cn("Utopia")) shouldBe false
  }

  @Test
  fun definitionConditionsUseTheCompleteModuleSelection() {
    val withoutColonies = table(cn("UtopiaPlanitiaMapOption"))
    val withColonies = table(cn("UtopiaPlanitiaMapOption"), cn("ColoniesExpansion"))

    withoutColonies.isActive(cn("MilestoneUM1")) shouldBe false
    withColonies.isActive(cn("MilestoneUM1")) shouldBe true
  }

  @Test
  fun expansionModuleCanAddDefinitionsToAnotherSelectedMap() {
    val base = table(cn("TharsisMapOption"))
    val venus = table(cn("TharsisMapOption"), cn("VenusNextExpansion"))

    base.isActive(cn("MilestoneVM1")) shouldBe false
    venus.isActive(cn("MilestoneVM1")) shouldBe true
  }

  @Test
  fun milestonesAwardsModulePrivatelySelectsItsBundleContent() {
    val base = table(cn("TharsisMapOption"))
    val expanded = table(cn("TharsisMapOption"), cn("MilestonesAwardsExpansion"))

    base.isActive(cn("MilestoneMM02")) shouldBe false
    base.isActive(cn("MilestoneBM4")) shouldBe true
    expanded.isActive(cn("MilestoneMM02")) shouldBe true
    expanded.isActive(cn("MilestoneBM4")) shouldBe false
    expanded.isActive(cn("AwardMA01")) shouldBe true
    expanded.isActive(cn("AwardMA21")) shouldBe true
  }

  @Test
  fun promoModuleReplacesCardsWithoutRemovingEitherFromTheAuthority() {
    val relevant =
        setOf(
            cn("Card039"),
            cn("Card136"),
            cn("Card165"),
            cn("CardX31"),
            cn("CardX32"),
            cn("CardX33"),
        )
    val withoutPromos = table(cn("TharsisMapOption"))
    val withPromos = table(cn("TharsisMapOption"), cn("PromoCardPack"))

    relevant.filterTo(linkedSetOf(), withoutPromos::isActive) shouldBe
        setOf(cn("Card039"), cn("Card136"), cn("Card165"))
    relevant.filterTo(linkedSetOf(), withPromos::isActive) shouldBe
        setOf(cn("CardX31"), cn("CardX32"), cn("CardX33"))
    Canon.allClassNames.containsAll(relevant) shouldBe true
  }

  @Test
  fun authorityRetainsEveryCustomImplementation() {
    Canon.customClasses.map { it.className.toString() } shouldContain "LowestProduction"
  }

  @Test
  fun entireAuthorityCatalogLoadsTogether() {
    Canon.classTable.allClassNames shouldBe Canon.allClassNames
    (Canon.classTable === Canon.classTable) shouldBe true
  }

  @Test
  fun playableTablesProjectTheAuthorityMasterTable() {
    val tharsis = table(cn("TharsisMapOption"))
    val hellas = table(cn("HellasMapOption"))

    (tharsis === hellas) shouldBe false
    assertProjectionOfCanon(tharsis)
    assertProjectionOfCanon(hellas)
  }

  private fun assertProjectionOfCanon(projection: ClassTable) {
    val master = Canon.classTable
    (projection.allClassNames - master.allClassNames) shouldBe emptySet()
    master.allClassNames.forEach { name ->
      val projectedClass = projection.getClass(name)
      val masterClass = master.getClass(name)
      projectedClass.className shouldBe masterClass.className
      projectedClass.abstract shouldBe masterClass.abstract
      projectedClass.directSuperclasses.map { it.className } shouldBe
          masterClass.directSuperclasses.map { it.className }
    }
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
  fun standardFormBundleLoadsLanguageFiles() {
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
