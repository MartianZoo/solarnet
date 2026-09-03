package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.canon.MarsMapReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RegenerateMapAreasTest {
  @Test
  internal fun replacesAreaBlockFromDiagramAndRoundTripsThroughPets() {
    val source =
        """
        CLASS DemoMap : MarsMap

        // The map areas below are code-generated based on the following comment
        //
        //            VS    L
        //  WPP    LC    L
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
        listOf(1 to 2, 1 to 3, 2 to 1, 2 to 2, 2 to 3),
        map.areas.map { it.row to it.column },
    )
    assertEquals(
        """
        //            VS    L
        //
        //  WPP    LC    L
        """
            .trimIndent(),
        renderDiagram(map),
    )
  }

  @Test
  internal fun padsEveryCoordinateWhenAnyRowOrColumnHasTwoDigits() {
    val wideSource =
        """
        CLASS DemoMap : MarsMap

        // The map areas below are code-generated based on the following comment
        // L L L L L L L L L L

        CLASS Demo_1_1 : LandArea { row = 1; column = 1 }
        """
            .trimIndent()
    val tallSource =
        (listOf(
                "CLASS DemoMap : MarsMap",
                "",
                "// The map areas below are code-generated based on the following comment",
            ) +
                (0..10).map { row -> "// ${"   ".repeat(row)}L" } +
                listOf("", "CLASS Demo_1_1 : LandArea { row = 1; column = 1 }"))
            .joinToString("\n")

    val wide = regenerateMapAreas(wideSource)
    val tall = regenerateMapAreas(tallSource)

    assertTrue("CLASS Demo_01_01" in wide)
    assertTrue("CLASS Demo_01_10" in wide)
    assertTrue("CLASS Demo_1_1" !in wide)
    assertTrue("CLASS Demo_01_01" in tall)
    assertTrue("CLASS Demo_11_11" in tall)
    assertTrue("CLASS Demo_1_1" !in tall)
  }
}
