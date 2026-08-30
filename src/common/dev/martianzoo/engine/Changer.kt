package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.pets.types.Type

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
      val change = StateChange(count, gaining?.expression, removing?.expression)
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
