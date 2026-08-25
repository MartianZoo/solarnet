package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.data.MarsMapReader
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RegenerateMapAreasTest {
  @Test
  internal fun replacesAreaBlockFromDiagramAndRoundTripsThroughPets() {
    val source =
        """
        CLASS DemoMap : MarsMap

        // The map areas below are code-generated based on the following comment
        //
        // VS L
        // WPP LC L
        //

        CLASS Demo_1_1 : LandArea { row = 99; column = 99 }
        """
            .trimIndent()

    val regenerated = regenerateMapAreas(source)
    val map = MarsMapReader.readMaps(regenerated).single()

    assertEquals(
        map.areas.map { it.asClassDeclaration }.toSet(),
        parseClasses(regenerated).drop(1).toSet(),
    )
    assertEquals(
        """
        //      VS    L
        //
        //  WPP    LC    L
        """
            .trimIndent(),
        renderDiagram(map),
    )
  }
}
