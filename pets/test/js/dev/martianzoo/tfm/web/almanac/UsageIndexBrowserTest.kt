package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UsageIndexBrowserTest {
  private companion object {
    val catalog =
        object : TfmCatalog(Canon) {
          override val explicitClassDeclarations =
              super.explicitClassDeclarations +
                  parseClasses(
                      """
                      CLASS PositiveContents : AutomatedCard {
                        cost = 0
                        HAS =1 BuildingTag<This>
                      }
                      CLASS MinimumContents : AutomatedCard {
                        cost = 0
                        HAS 1 BuildingTag<This>
                      }
                      CLASS LimitedContents : AutomatedCard {
                        cost = 0
                        HAS MAX 1 BuildingTag<This>
                      }
                      CLASS AbsentContents : AutomatedCard {
                        cost = 0
                        HAS =0 BuildingTag<This>
                      }
                      CLASS GlobalCount {
                        HAS =1 BuildingTag
                      }
                      CLASS Roles {
                        This: Microbe<Ants>, MC / BuildingTag
                        Plant IF OxygenStep: (TemperatureStep: Steel)
                      }
                      CLASS RefinedGain {
                        This: CardFront(HAS BuildingTag)
                      }
                      CLASS ConditionalCard : AutomatedCard {
                        cost = 0
                        requirement = HAS "BuildingTag"
                      }
                      ABSTRACT CLASS PopularParent {}
                      ABSTRACT CLASS NarrowParent {}
                      CLASS OtherChild : PopularParent {}
                      CLASS MultipleParents : NarrowParent, PopularParent {}
                      """
                          .trimIndent()
                  )
        }
    val index by lazy { buildUsageIndex(catalog.classTable) }
  }

  @Test
  fun declaredContentsAreGainsButLimitsAndUnattachedCountsAreNot() {
    assertEquals(setOf("Gains"), roles("BuildingTag", "PositiveContents"))
    assertEquals(setOf("Gains"), roles("BuildingTag", "MinimumContents"))
    for (name in listOf("LimitedContents", "AbsentContents", "GlobalCount")) {
      assertEquals(setOf("Invariants"), roles("BuildingTag", name), name)
    }
    assertTrue("Gains" in roles("BuildingTag", "PowerPlant"))
    assertFalse("Requirements" in roles("BuildingTag", "PowerPlant"))
  }

  @Test
  fun targetsArgumentsCountsAndConditionsKeepTheirOwnRoles() {
    assertEquals(setOf("Gains"), roles("Microbe", "Roles"))
    assertEquals(setOf("Other references"), roles("Ants", "Roles"))
    assertEquals(setOf("Other Metrics"), roles("BuildingTag", "Roles"))
    assertEquals(setOf("Triggers"), roles("Plant", "Roles"))
    assertEquals(setOf("Gates"), roles("OxygenStep", "Roles"))
    assertEquals(setOf("Gates"), roles("TemperatureStep", "Roles"))
    assertEquals(setOf("Gains"), roles("Steel", "Roles"))
    assertEquals(setOf("Gains"), roles("CardFront", "RefinedGain"))
    assertEquals(setOf("Gates"), roles("BuildingTag", "RefinedGain"))
    assertEquals(setOf("Requirements"), roles("BuildingTag", "ConditionalCard"))
  }

  @Test
  fun existingLoweringDistinguishesProductionAndIncludesActionCosts() {
    assertTrue("Gains" in roles("Production", "AdaptedLichen"))
    assertFalse("Gains" in roles("Plant", "AdaptedLichen"))
    assertTrue("Other references" in roles("Plant", "AdaptedLichen"))
    assertTrue("Gains" in roles("Microbe", "Ants"))
    assertTrue("Removes" in roles("Microbe", "Ants"))
  }

  @Test
  fun principalSuperclassUsesOnlyDirectParentsAndTheirSubclassCounts() {
    val klass = catalog.classTable.getClass(cn("MultipleParents"))
    assertEquals(cn("PopularParent"), principalSuperclass(klass)?.className)
    assertEquals(null, principalSuperclass(catalog.classTable.getClass(cn("Component"))))
  }

  private fun roles(target: String, user: String): Set<String> =
      index[cn(target)].orEmpty().filterValues { cn(user) in it }.keys
}
