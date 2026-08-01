package dev.martianzoo.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class SoloPlacementTest {
  @Test
  fun calculatesCompatibilityModePlacementsFromCanonicalCardsAndMap() {
    val placements =
        calculateSoloPlacements(
            listOf(
                "--compatibility",
                "Tharsis",
                "MiningColony",
                "ResearchOutpost",
                "Potatoes",
                "JupiterFloatingStation",
            )
        )

    assertEquals(
        listOf("Tharsis_4_6", "Tharsis_3_6", "Tharsis_6_6", "Tharsis_6_5"),
        placements.map { it.area.className.toString() },
    )
    assertEquals(
        listOf("MiningColony", "Potatoes", "ResearchOutpost", "JupiterFloatingStation"),
        placements.map { it.card.className.toString() },
    )
    assertEquals(listOf(20, 2, 18, 9), placements.map { it.card.cost })
    assertEquals(4, placements.map { it.area }.toSet().size)
  }

  @Test
  fun countFromZeroModeAcceptsZeroAndCountsOneAsSecond() {
    val zeroPlacements =
        calculateSoloPlacements(
            listOf(
                "Tharsis",
                "AirRaid",
                "ColonizerTrainingCamp",
                "NuclearPower",
                "ArcticAlgae",
            )
        )
    val onePlacements =
        calculateSoloPlacements(
            listOf(
                "Tharsis",
                "MarketManipulation",
                "ColonizerTrainingCamp",
                "NuclearPower",
                "ArcticAlgae",
            )
        )

    assertEquals("Tharsis_1_1", zeroPlacements.first().area.className.toString())
    assertEquals("Tharsis_1_3", onePlacements.first().area.className.toString())
  }

  @Test
  fun compatibilityModeRejectsZeroAndCountsOneAsFirst() {
    val onePlacements =
        calculateSoloPlacements(
            listOf(
                "--compatibility",
                "Tharsis",
                "MarketManipulation",
                "ColonizerTrainingCamp",
                "NuclearPower",
                "ArcticAlgae",
            )
        )
    assertEquals("Tharsis_1_1", onePlacements.first().area.className.toString())

    val exception =
        assertFailsWith<IllegalArgumentException> {
          calculateSoloPlacements(
              listOf(
                  "--compatibility",
                  "Tharsis",
                  "AirRaid",
                  "ColonizerTrainingCamp",
                  "NuclearPower",
                  "ArcticAlgae",
              )
          )
        }

    assertTrue(exception.message!!.contains("compatibility mode rejects"))
  }
}
