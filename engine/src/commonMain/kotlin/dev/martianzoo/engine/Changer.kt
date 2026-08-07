package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.types.Type

internal class Changer(
    private val reader: GameReader,
    private val components: ComponentGraph,
    private val events: EventLog,
    private val defaultActor: Actor,
) {

  internal fun change(
      count: Int,
      gaining: Component?,
      removing: Component?,
      cause: Cause?,
      orRemoveOneDependent: Boolean,
      actor: Actor = defaultActor,
  ): Pair<ChangeEvent, Boolean> {
    return try {
      val change = StateChange(count, gaining?.expressionFull, removing?.expressionFull)
      val event = ChangeEvent(events.nextOrdinal, actor, change, cause)
      events.record(event) { components.applyChange(count, gaining, removing) } to true
    } catch (e: ExistingDependentsException) {
      if (!orRemoveOneDependent) throw e
      removeAll(e.dependents.first(), cause, actor) to false
    }
  }

  private fun removeAll(dependent: Type, cause: Cause?, actor: Actor): ChangeEvent =
      change(
              count = reader.countComponent(dependent),
              gaining = null,
              removing = dependent.toComponent(reader),
              cause = cause,
              orRemoveOneDependent = true,
              actor = actor,
          )
          .first
}
