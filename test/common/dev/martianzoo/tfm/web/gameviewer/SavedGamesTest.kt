package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.state.Checkpoint
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SavedGamesTest {
  @Test
  internal fun indexUsesSortedDistinctTestFilenames() {
    assertEquals(
        listOf(SavedGame("AlphaTest"), SavedGame("ZuluTest")),
        SavedGames.fromIndex("ZuluTest\n\nAlphaTest\nZuluTest\n"),
    )
  }

  @Test
  internal fun playerColorNamesAreUsedAndOtherNamesReceiveUnusedColors() {
    assertEquals(
        listOf("green", "red", "yellow", "blue"),
        assignPlayerColors(listOf("Green", "Alex", "Yellow", "Blue")),
    )
    assertEquals(
        listOf("green", "red", "yellow"),
        assignPlayerColors(listOf("Green", "Green", "Alex")),
    )
    assertEquals(
        listOf("red", "yellow", "green", "blue", "purple", "red"),
        assignPlayerColors(List(6) { "Player$it" }),
    )
  }

  @Test
  internal fun selectablePositionsCollapseRunsWithoutVisibleLogEvents() {
    val positions = listOf(0, 4, 7, 10, 13).map(::Checkpoint)

    assertEquals(listOf(0, 1, 3, 4), selectablePositionIndices(positions, listOf(1, 8)))
  }
}
