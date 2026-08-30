package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameEvent.ChangeEvent

/** Whether this change belongs in the ordinary player-facing event log. */
public fun ChangeEvent.isVisibleInLog(game: GameReader): Boolean {
  val changedTypes = listOfNotNull(change.gaining, change.removing).map(game::resolve)
  val hidden = game.resolve(HIDDEN.expression)
  val phase = game.resolve(cn("Phase").expression)
  return changedTypes.any { !it.isSubtypeOf(hidden) } || changedTypes.any { it.isSubtypeOf(phase) }
}

/** The same event selection shown by the REPL's filtered `log` command. */
public fun World.visibleLogEvents(): List<ChangeEvent> =
    events.changesSinceSetup().filter { it.isVisibleInLog(reader) }
