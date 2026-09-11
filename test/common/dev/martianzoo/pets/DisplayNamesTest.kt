package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.types.testCatalog
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DisplayNamesTest {
  @Test
  internal fun englishDefaultsSeparateClassNameWords() {
    defaultEnglishDisplayName(cn("ColonizerTrainingCamp")) shouldBe "Colonizer Training Camp"
    defaultEnglishDisplayName(cn("BeamFromAThoriumAsteroid")) shouldBe "Beam From AThorium Asteroid"
    defaultEnglishDisplayName(cn("Builder8")) shouldBe "Builder 8"
    defaultEnglishDisplayName(cn("NaturalPreserve_SpecialTile")) shouldBe
        "Natural Preserve Special Tile"
  }

  @Test
  internal fun requestedLocaleFallsBackPerEntry() {
    val base = testCatalog("CLASS First\nCLASS Second")
    val catalog =
        object : Catalog by base {
          override val displayNamesByLanguage =
              mapOf(
                  "en" to mapOf(cn("First") to "First card", cn("Second") to "Second card"),
                  "fr" to mapOf(cn("First") to "Première carte"),
              )
        }

    displayName(catalog, cn("First"), "FR_ca") shouldBe "Première carte"
    displayName(catalog, cn("Second"), "fr-CA") shouldBe "Second card"
  }
}
