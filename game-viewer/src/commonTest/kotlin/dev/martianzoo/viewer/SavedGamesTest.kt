package dev.martianzoo.viewer

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.visibleLogEvents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SavedGamesTest {
  @Test
  internal fun everySavedGameRecordsAndSeeks() {
    SavedGames.all.forEach { savedGame ->
      val recording = savedGame.create().record()
      assertTrue(recording.positions.size > 2, savedGame.name)
      val players = recording.world.actors.filterIsInstance<Player>()
      val finalCards = players.associateWith { playedCards(recording.world, it) }
      val finalCardResources = players.associateWith { player ->
        finalCards.getValue(player).associate { card ->
          card.className to cardResourceCount(recording.world, player, card)
        }
      }
      players.forEach { player ->
        assertTrue(finalCards.getValue(player).isNotEmpty(), "$savedGame ${player.className}")
        assertTrue(
            finalCards.getValue(player).all { cardImageDirectory(it) != null },
            "$savedGame ${player.className}",
        )
      }
      assertTrue(
          players.any { player -> playedEventCards(recording.world, player).isNotEmpty() },
          "$savedGame has a played event card",
      )
      assertTrue(
          finalCardResources.values.any { resources ->
            resources.values.filterNotNull().any { (_, count) -> count > 0 }
          },
          "$savedGame has a populated card-resource counter",
      )
      val visibleLogEvents = recording.world.visibleLogEvents()
      assertTrue(visibleLogEvents.isNotEmpty(), savedGame.name)
      val selectablePositions =
          selectablePositionIndices(recording.positions, visibleLogEvents.map { it.ordinal })
      assertEquals(recording.positions.lastIndex, selectablePositions.last(), savedGame.name)
      assertTrue(
          recording.positions.indices.any { position ->
            recording.seek(position)
            players.any { player ->
              playedCards(recording.world, player).any { card ->
                hasActionUsedMarker(recording.world, player, card)
              }
            }
          },
          "$savedGame exposes used action cards while navigating",
      )
      selectablePositions.zipWithNext().forEach { (before, after) ->
        val visibleOrdinals =
            recording.positions[before].ordinal until recording.positions[after].ordinal
        assertTrue(
            visibleLogEvents.any { it.ordinal in visibleOrdinals },
            "$savedGame positions $before and $after",
        )
      }

      recording.seek(0)
      players.forEach { player ->
        assertTrue(
            playedCards(recording.world, player).size < finalCards.getValue(player).size,
            "$savedGame ${player.className}",
        )
      }
      recording.seek(recording.positions.lastIndex)
      players.forEach { player ->
        assertEquals(
            finalCards.getValue(player),
            playedCards(recording.world, player),
            "$savedGame ${player.className}",
        )
        assertEquals(
            finalCardResources.getValue(player),
            finalCards.getValue(player).associate { card ->
              card.className to cardResourceCount(recording.world, player, card)
            },
            "$savedGame ${player.className} card resources",
        )
      }
    }
  }

  @Test
  internal fun playerNamesChooseTheTemporaryUpstreamColors() {
    assertEquals("green", playerColor("Dad"))
    assertEquals("green", playerColor("Keen"))
    assertEquals("purple", playerColor("Jane"))
    assertEquals("yellow", playerColor("Ellie"))
    assertEquals("blue", playerColor("Mom"))
    assertEquals("blue", playerColor("Chris"))
  }

  @Test
  internal fun selectablePositionsCollapseRunsWithoutVisibleLogEvents() {
    val positions = listOf(0, 4, 7, 10, 13).map(::Checkpoint)

    assertEquals(listOf(0, 1, 3, 4), selectablePositionIndices(positions, listOf(1, 8, 12)))
  }
}
