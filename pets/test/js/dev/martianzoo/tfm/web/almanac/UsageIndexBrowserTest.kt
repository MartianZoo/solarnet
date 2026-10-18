package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.search.CanonQuery
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
    val index by lazy {
      buildUsageIndex(catalog.classTable, CanonQuery(CanonQuery.Form.ELABORATED, catalog))
    }
  }

  @Test
  fun declaredContentsAreGainsButLimitsAndUnattachedCountsAreNot() {
    assertTrue("Gains" in roles("BuildingTag", "PositiveContents"))
    assertTrue("Gains" in roles("BuildingTag", "MinimumContents"))
    for (name in listOf("LimitedContents", "AbsentContents", "GlobalCount")) {
      assertFalse("Gains" in roles("BuildingTag", name), name)
      assertTrue("Requirements" in roles("BuildingTag", name), name)
      assertFalse("Counted in" in roles("BuildingTag", name), name)
    }
    assertTrue("Gains" in roles("BuildingTag", "PowerPlant"))
    assertTrue("Requirements" in roles("BuildingTag", "PowerPlant"))
    assertFalse("Counted in" in roles("BuildingTag", "PowerPlant"))
    assertFalse("Counted in" in roles("CityTag", "BoomTown"))
    assertTrue("Counted in" in roles("CityTag", "Architect"))
  }

  @Test
  fun targetsArgumentsCountsAndConditionsKeepTheirOwnRoles() {
    assertEquals(setOf("Gains"), roles("Microbe", "Roles"))
    assertEquals(setOf("Other references"), roles("Ants", "Roles"))
    assertTrue("Counted in" in roles("BuildingTag", "Roles"))
    assertEquals(setOf("Triggers"), roles("Plant", "Roles"))
    assertTrue("Conditions" in roles("OxygenStep", "Roles"))
    assertTrue("Counted in" in roles("OxygenStep", "Roles"))
    assertTrue("Conditions" in roles("TemperatureStep", "Roles"))
    assertEquals(setOf("Gains"), roles("Steel", "Roles"))
    assertEquals(setOf("Gains"), roles("CardFront", "RefinedGain"))
    assertTrue("Conditions" in roles("BuildingTag", "RefinedGain"))
    assertTrue("Counted in" in roles("BuildingTag", "RefinedGain"))
    assertTrue("Requirements" in roles("BuildingTag", "ConditionalCard"))
    assertTrue("Triggers" in roles("OxygenStep", "OxygenStep"))
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
