package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.GameEvent.ChangeEvent

/**
 * Viewer copy of the ordinary player-facing event-log rule, guarded by a cross-module parity test.
 */
internal fun visibleLogEvents(
    events: List<ChangeEvent>,
    reader: GameReader,
): List<ChangeEvent> {
  val hidden = reader.resolve(HIDDEN.expression)
  val phase = reader.resolve(cn("Phase").expression)
  return events.filter { event ->
    val changedTypes = listOfNotNull(event.change.gaining, event.change.removing).map { it.type }
    changedTypes.any { !it.isSubtypeOf(hidden) } || changedTypes.any { it.isSubtypeOf(phase) }
  }
}
