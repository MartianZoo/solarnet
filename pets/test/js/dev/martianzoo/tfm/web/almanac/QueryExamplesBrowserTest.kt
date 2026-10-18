package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.search.CanonQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class QueryExamplesBrowserTest {
  private val queries = CanonQuery.Form.entries.associateWith { CanonQuery(it) }

  @Test
  fun everyExampleRunsInItsSelectedTreeAndReturnsSomething() {
    for (example in queryExamples) {
      val matches = queries.getValue(example.form).evaluate(example.xpath)
      assertTrue(matches.isNotEmpty(), example.description)
      assertTrue(matches.all { it.xml == null }, example.description)
    }
  }

  @Test
  fun productionSearchReachesConditionalChoicesWithoutConfusingPlantsWithProduction() {
    val production = names("Which cards have a play effect that gains 2 or more plant production")
    assertTrue("NitrogenRichAsteroid" in production)
    assertTrue("NitrophilicMoss" in production)
    assertFalse("AdaptedLichen" in production)
    assertFalse("TowingAComet" in production)
    val plants = names("Which cards have a play effect that gains plants without")
    assertTrue("TowingAComet" in plants)
    assertFalse("NitrogenRichAsteroid" in plants)
    val evidence = example("Show the actual 2-or-more plant-production instructions")
    val matches = queries.getValue(evidence.form).evaluate(evidence.xpath)
    assertEquals(production, matches.mapNotNull { it.owner }.toSet())
    assertTrue(
        matches.filter { it.owner == "NitrogenRichAsteroid" }.all { "4" in it.pets.orEmpty() }
    )
  }

  @Test
  fun examplesDistinguishCostsTriggersScoringAndDeclaredContents() {
    assertTrue("Ants" in names("Which cards have an action that spends microbes"))
    assertFalse("Decomposers" in names("Which cards have an action that spends microbes"))
    assertTrue("Decomposers" in names("Which cards react to any biological tag"))
    assertTrue("Birds" in names("Which cards score at game end according to their animals"))
    assertFalse("Birds" in names("Which cards give at least 2 fixed VP"))
    assertTrue("Research" in names("Which cards repeat the same tag"))
    assertTrue("PowerPlant" in names("Which classes declare a positive exact number"))
    assertTrue("MiningArea" in names("Which cards have a play effect that places a tile"))
    assertTrue("EarthCatapult" in names("Which cards reduce what is owed"))
  }

  private fun example(start: String): QueryExample = queryExamples.single {
    it.description.startsWith(start)
  }

  private fun names(start: String): Set<String> {
    val example = example(start)
    return queries.getValue(example.form).evaluate(example.xpath).mapNotNull { it.name }.toSet()
  }
}
