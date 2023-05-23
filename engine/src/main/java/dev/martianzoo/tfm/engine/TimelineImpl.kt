package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.engine.Game.Timeline
import dev.martianzoo.tfm.engine.Game.Timeline.Checkpoint
import javax.inject.Inject

internal class TimelineImpl @Inject constructor(
    private val updater: Updater,
    private val events: WritableEventLog,
    private val tasks: WritableTaskQueue,
    private val reader: SnReader,
) : Timeline {
  init { println(this) }

  override fun checkpoint() = Checkpoint(events.size)

  override fun rollBack(checkpoint: Checkpoint) {

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
   */
  override fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
    return events.activitySince(checkpoint)
  }

  fun setupFinished() = events.setStartPoint()
}
