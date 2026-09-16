package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.Component.Companion.toComponent
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameWorld

internal class Changer(
    private val reader: GameReader,
    private val gameWorld: GameWorld,
    private val effector: Effector,
) {

  internal fun change(
      count: Int,
      gaining: Component?,
      removing: Component?,
      cause: Cause?,
      orRemoveOneDependent: Boolean,
      actor: Actor,
  ): Pair<ChangeEvent, Boolean> {
    return try {
      val change =
          when {
            gaining == null -> ComponentChange.Remove(count, checkNotNull(removing))
            removing == null &&
                gaining.type.rootClass.isSubtypeOf(reader.classTable.getClass(SIGNAL)) ->
                ComponentChange.Transmute(count, gaining = gaining, removing = gaining)
            removing == null -> ComponentChange.Gain(count, gaining)
            else -> ComponentChange.Transmute(count, gaining, removing)
          }
      val event = ChangeEvent(gameWorld.nextOrdinal, actor, change, cause)
      applyEvent(event) to true
    } catch (e: ExistingDependentsException) {
      if (!orRemoveOneDependent) throw e
      removeAll(e.dependents.first(), cause, actor) to false
    }
  }

  /** Reapplies one recorded component event without calculating its consequences. */
  internal fun replay(event: ChangeEvent) {
    applyEvent(event)
  }

  /** Reverses authoritative state and then synchronizes the engine's derived effect index. */
  internal fun rollBackTo(ordinal: Int) {
    gameWorld.rollBackTo(ordinal).forEach(effector::applied)
  }

  private fun applyEvent(event: ChangeEvent): ChangeEvent {
    effector.prepare(event.change)
    gameWorld.apply(event)
    effector.applied(event.change)
    return event
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
