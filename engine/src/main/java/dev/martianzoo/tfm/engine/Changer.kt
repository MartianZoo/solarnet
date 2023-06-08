package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Engine.ChangeLogger
import dev.martianzoo.tfm.engine.Engine.PlayerScoped
import dev.martianzoo.tfm.engine.Engine.Updater
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
    listOfNotNull(gaining, removing).forEach { require(it.mtype.root.custom == null) }
    try {
      return doSingleChange(count, gaining, removing, cause) to true
    } catch (e: ExistingDependentsException) {
      if (orRemoveOneDependent) return removeAll(e.dependents.first(), cause) to false
      throw e
    }
  }

  private fun removeAll(dependent: Type, cause: Cause?): ChangeEvent {
    val component = dependent.toComponent(reader)
    val count = reader.countComponent(component.mtype)
    return change(count, null, component, cause, true).first
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
