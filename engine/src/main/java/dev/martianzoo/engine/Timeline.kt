package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Engine.Updater
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.TaskResult
import javax.inject.Inject

@GameScoped
public class Timeline @Inject constructor(val reader: GameReader) {
  // These classes aren't public, but Timeline is, so they can't be constructor properties.
  @Inject internal lateinit var updater: Updater
  @Inject internal lateinit var events: WritableEventLog
  @Inject internal lateinit var tasks: WritableTaskQueue

  public data class Checkpoint(internal val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
    override fun toString() = "$ordinal"
  }

  fun checkpoint() = Checkpoint(events.size)

  fun rollBack(checkpoint: Checkpoint) {

    val ordinal = checkpoint.ordinal
    require(ordinal <= events.size)
    if (ordinal == events.size) return

    val subList = events.events.subList(ordinal, events.size) // TODO improve
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskEvent -> tasks.reverse(entry)
        is ChangeEvent ->
            with(entry.change) {
              updater.update(
                  count = count,
                  gaining = removing?.toComponent(reader),
                  removing = gaining?.toComponent(reader),
              )
            }
      }
    }
    subList.clear()
  }

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   * Within the block you can throw [AbortOperationException] to roll everything back but have this
   * method complete normally.
   */
  fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (e: Exception) {
      rollBack(checkpoint)
      if (e !is AbortOperationException) throw e
    }
    return events.activitySince(checkpoint)
  }

  public class AbortOperationException : Exception()

  internal fun setupFinished() = events.setStartPoint()
}
