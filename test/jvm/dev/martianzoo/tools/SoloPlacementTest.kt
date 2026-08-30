package dev.martianzoo.tools

import dev.martianzoo.tfm.canon.cardCost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class SoloPlacementTest {
  @Test
  internal fun calculatesCompatibilityModePlacementsFromCanonicalCardsAndMap() {
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
        listOf("Tharsis_4_6", "Tharsis_4_7", "Tharsis_9_7", "Tharsis_8_6"),
        placements.map { it.area.className.toString() },
    )
    assertEquals(
        listOf("MiningColony", "ResearchOutpost", "Potatoes", "JupiterFloatingStation"),
        placements.map { it.card.className.toString() },
    )
    val output = formatPlacements(placements)
    assertTrue(output.contains("Mining Colony"))
    assertTrue(output.contains("Jupiter Floating Station"))
    assertTrue(!output.contains("MiningColony"))
    assertEquals(listOf(20, 18, 2, 9), placements.map { cardCost(it.card) })
    assertEquals(4, placements.map { it.area }.toSet().size)
  }

  @Test
  internal fun standardModeAcceptsZeroAndCountsOneAsSecond() {
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
  internal fun compatibilityModeRejectsZeroAndCountsOneAsFirst() {
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
