package dev.martianzoo.state

import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.state.GameEvent.ChangeEvent

/** Returns the changes that belong in an ordinary player-facing event log. */
public fun Iterable<ChangeEvent>.visibleLogEvents(game: GameReader): List<ChangeEvent> {
  val hidden = game.resolve(HIDDEN.expression)
  return filter { event ->
    val changedTypes = listOfNotNull(event.change.gaining, event.change.removing).map { it.type }
    changedTypes.any { !it.isSubtypeOf(hidden) }
  }
}
