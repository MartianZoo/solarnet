package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GenerateMapPetsTest {
  @Test
  internal fun rendersEveryCanonicalMapAndAreaAsPets() {
    val sources = canonicalMapPetsFiles(alignedAreas = true).values
    val generated = sources.flatMap(::parseClasses)
    assertEquals(
        canonicalMapDeclarations().map { it.className }.sorted(),
        generated.map { it.className }.sorted(),
    )
    assertTrue(
        sources.all { source ->
          val sections = source.split("$GENERATED_AREAS_COMMENT\n", limit = 2)
          sections.size == 2 &&
              parseClasses(sections[0]).all { it.className.toString().endsWith("Map") } &&
              parseClasses(sections[1]).none { it.className.toString().endsWith("Map") }
        }
    )
  }
}
