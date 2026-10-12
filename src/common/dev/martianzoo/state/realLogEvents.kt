package dev.martianzoo.state

import dev.martianzoo.pets.api.SystemClasses.REAL
import dev.martianzoo.state.GameEvent.ChangeEvent

/**
 * Keeps changes with a Real type on at least one side. Callers choose the history's starting point.
 */
public fun Iterable<ChangeEvent>.realLogEvents(game: GameReader): List<ChangeEvent> {
  val real = game.resolve(REAL.expression)
  return filter { event ->
    val changedTypes = listOfNotNull(event.change.gaining, event.change.removing).map { it.type }
    changedTypes.any { it.isSubtypeOf(real) }
  }
}
