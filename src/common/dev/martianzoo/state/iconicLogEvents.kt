package dev.martianzoo.state

import dev.martianzoo.pets.api.SystemClasses.ICONIC
import dev.martianzoo.state.GameEvent.ChangeEvent

/**
 * Keeps changes with an Iconic type on at least one side. Callers choose the history's starting
 * point.
 */
public fun Iterable<ChangeEvent>.iconicLogEvents(game: GameReader): List<ChangeEvent> {
  val iconic = game.resolve(ICONIC.expression)
  return filter { event ->
    val changedTypes = listOfNotNull(event.change.gaining, event.change.removing).map { it.type }
    changedTypes.any { it.isSubtypeOf(iconic) }
  }
}
