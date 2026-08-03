package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.ChangeLogger
import dev.martianzoo.engine.Engine.Updater
import dev.martianzoo.types.Type

internal class Changer(
    private val reader: GameReader,
    private val updater: Updater,
    private val changeLog: ChangeLogger,
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
      val change = updater.update(count, gaining, removing)
      changeLog.addChangeEvent(change, actor, cause) to true
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
