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
    try {
      return doSingleChange(count, gaining, removing, cause) to true
    } catch (e: ExistingDependentsException) {
      if (orRemoveOneDependent) return removeAll(e.dependents.first(), cause) to false
      throw e
    }
  }

  private fun removeAll(dependent: Type, cause: Cause?): ChangeEvent {
    val component = dependent.toComponent(reader)
    val count = reader.countComponent(component)
    return change(
        count = count,
        gaining = null,
        removing = component,
        cause = cause,
        orRemoveOneDependent = true
    ).first
  }

  private fun doSingleChange(
      count: Int,
      gaining: Component?,
      removing: Component?,
      cause: Cause?
  ): ChangeEvent {
    val change = updater.update(count, gaining, removing)
    return changeLog.addChangeEvent(change, player, cause)
  }
}
