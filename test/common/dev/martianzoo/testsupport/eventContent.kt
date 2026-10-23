package dev.martianzoo.testsupport

import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent

/**
 * These events with their [GameEvent.transaction] dropped, for comparing two histories that should
 * contain the same changes but need not agree on provenance.
 *
 * A reconstruction from recorded inputs replays a Player's choices as submissions even when the
 * original game performed them through that Player's own handling policy, so the two histories
 * record different [dev.martianzoo.state.Motivation] values for identical work. Use this where the
 * change content is the claim; compare events directly where the grouping itself is.
 */
internal fun List<GameEvent>.eventContent(): List<GameEvent> = map { event ->
  when (event) {
    is ChangeEvent -> event.copy(transaction = null)
    is TaskAddedEvent -> event.copy(transaction = null)
    is TaskEditedEvent -> event.copy(transaction = null)
    is TaskRemovedEvent -> event.copy(transaction = null)
  }.also { it.notes = event.notes }
}
