package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameReader

/** Whether this change belongs in the ordinary player-facing event log. */
public fun ChangeEvent.isVisibleInLog(game: GameReader): Boolean {
  val changedTypes = listOfNotNull(change.gaining, change.removing).map { it.type }
  val hidden = game.resolve(HIDDEN.expression)
  return changedTypes.any { !it.isSubtypeOf(hidden) }
}

/** The same event selection shown by the REPL's filtered `log` command. */
public fun World.visibleLogEvents(): List<ChangeEvent> =
    events.changesSinceSetup().filter { it.isVisibleInLog(reader) }
