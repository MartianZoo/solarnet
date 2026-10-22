package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskResult

/**
 * Keeps recorded positions and the retained future of a live World. The [atomic] helper restores
 * recorded game changes after a failed block; it does not define or complete an engine transaction.
 * History navigation is refused while the completion callback runs.
 */
internal class TimelineImpl(
    private val gameWorld: GameWorld,
    private val changer: Changer,
) : Timeline {

  override fun checkpoint() = Checkpoint(gameWorld.nextOrdinal)

  /** The current event count, for internal scans over the events a block produced. */
  internal val nextEventOrdinal: Int
    get() = gameWorld.nextOrdinal

  /** The changes and surviving new tasks recorded since event [ordinal]. */
  internal fun activitySince(ordinal: Int): TaskResult = gameWorld.activitySince(ordinal)

  private var inCompletionCallback = false

  /**
   * Runs [block] as the completion callback, during which history navigation is refused. Nested
   * gameplay calls are still permitted; they may not seek or truncate either.
   */
  internal fun <T> duringCompletionCallback(block: () -> T): T {
    check(!inCompletionCallback) { "already reporting completion" }
    inCompletionCallback = true
    return try {
      block()
    } finally {
      inCompletionCallback = false
    }
  }

  private fun requireNotReportingCompletion() {
    check(!inCompletionCallback) { "Cannot navigate history while reporting completion" }
  }

  override var floor = Checkpoint(0)
    private set

  private val recordedPositions = mutableListOf<Checkpoint>()
  private val future = mutableListOf<GameEvent>()
  private val atomicStarts = mutableListOf<Int>()

  override val positions: List<Checkpoint>
    get() = recordedPositions.toList()

  internal val atEnd: Boolean
    get() = future.isEmpty()

  override fun recordPosition() {
    val current = checkpoint()
    if (current in recordedPositions) return
    requireAtEnd()
    recordedPositions += current
  }

  internal fun restorePositions(positions: List<Checkpoint>) {
    check(recordedPositions.isEmpty())
    check(positions.lastOrNull() == checkpoint())
    recordedPositions.addAll(positions)
  }

  internal fun requireAtEnd() {
    check(atEnd) { "Truncate the future before changing this World" }
  }

  override fun commit() {
    requireAtEnd()
    // TODO(ENGINE_CONTRACT): restrict committing inside a restoration block or restore this field
    // on failure. Currently a failed block can leave the floor beyond the restored game position.
    floor = checkpoint()
  }

  override fun seek(checkpoint: Checkpoint) {
    requireNotReportingCompletion()
    check(atomicStarts.lastOrNull()?.let { checkpoint.ordinal >= it } != false) {
      "Cannot seek before the current atomic call began"
    }
    check(checkpoint() in recordedPositions) { "Cannot seek from an unrecorded position" }
    val ordinal = checkpoint.ordinal
    require(ordinal >= floor.ordinal) {
      "Cannot seek to $ordinal; committed through ${floor.ordinal}"
    }
    require(checkpoint in recordedPositions) { "Position $ordinal is not recorded" }
    val current = gameWorld.nextOrdinal
    when {
      ordinal < current -> {
        val removed = gameWorld.events.entriesSince(ordinal)
        changer.rollBackTo(ordinal)
        future.addAll(0, removed)
      }
      ordinal > current -> {
        val count = ordinal - current
        future.take(count).forEach(changer::replay)
        future.subList(0, count).clear()
      }
    }
  }

  override fun stepBack(): Boolean {
    val minimum = maxOf(floor.ordinal, atomicStarts.lastOrNull() ?: floor.ordinal)
    val previous =
        recordedPositions.lastOrNull {
          it.ordinal < gameWorld.nextOrdinal && it.ordinal >= minimum
        } ?: return false
    seek(previous)
    return true
  }

  override fun stepForward(): Boolean {
    val next = recordedPositions.firstOrNull { it.ordinal > gameWorld.nextOrdinal } ?: return false
    seek(next)
    return true
  }

  override fun truncate() {
    requireNotReportingCompletion()
    check(checkpoint() in recordedPositions) { "Cannot truncate at an unrecorded position" }
    future.clear()
    recordedPositions.removeAll { it.ordinal > gameWorld.nextOrdinal }
  }

  /** Returns a speculative World to its source point without reversing its local event trail. */
  internal fun resetOverlay() {
    check(atomicStarts.isEmpty()) { "cannot reset during an atomic block" }
    changer.resetOverlay {
      floor = checkpoint()
      recordedPositions.removeAll { it.ordinal > floor.ordinal }
      future.clear()
    }
  }

  private fun rollBackStateTo(ordinal: Int) {
    // TODO(ENGINE_CONTRACT): review restoring position metadata as part of failure-atomicity.
    // A position newly recorded at ordinal survives this removal of only later positions.
    changer.rollBackTo(ordinal)
    future.clear()
    recordedPositions.removeAll { it.ordinal > ordinal }
  }

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  override fun atomic(block: () -> Unit): TaskResult {
    requireAtEnd()
    val start = nextEventOrdinal
    // TODO(ENGINE_CONTRACT): review the scope of failure-atomicity. Only the event position is
    // captured; existing event commentary and arbitrary caller settings are not restored.
    atomicStarts += start
    try {
      block()
    } catch (_: AbortTransactionException) {
      rollBackStateTo(start)
    } catch (e: Exception) {
      // TODO(ENGINE_CONTRACT): decide which failure types require restoration. Errors and other
      // non-Exception Throwables currently escape without restoring the block's game changes.
      rollBackStateTo(start)
      throw e
    } finally {
      atomicStarts.removeLast()
    }
    return gameWorld.activitySince(start)
  }

  internal fun initializationFinished() = gameWorld.markSetupStart()
}
