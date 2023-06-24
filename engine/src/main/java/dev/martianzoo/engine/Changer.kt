package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.ChangeLogger
import dev.martianzoo.engine.Engine.PlayerScoped
import dev.martianzoo.engine.Engine.Updater
import javax.inject.Inject

@PlayerScoped
internal class Changer
@Inject
constructor(
    private val reader: GameReader,
    private val updater: Updater,
    private val changeLog: ChangeLogger,
    private val player: Player,
) {

  fun change(
      count: Int,
      gaining: Component?,
      removing: Component?,
      cause: Cause?,
      orRemoveOneDependent: Boolean,
  ): Pair<ChangeEvent, Boolean> {
    listOfNotNull(gaining, removing).forEach { require(!it.isCustom) }

    return try {
      val change = updater.update(count, gaining, removing)
      changeLog.addChangeEvent(change, player, cause) to true

    } catch (e: ExistingDependentsException) {
      if (!orRemoveOneDependent) throw e
      removeAll(e.dependents.first(), cause) to false
    }
  }

  private fun removeAll(dependent: Type, cause: Cause?): ChangeEvent =
      change(
              count = reader.countComponent(dependent),
              gaining = null,
              removing = dependent.toComponent(reader),
              cause = cause,
              orRemoveOneDependent = true)
          .first
}
