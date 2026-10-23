package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskResult

/**
 * Derives transaction positions and keeps the retained future of a live World. The [atomic] helper
 * restores recorded game changes after a failed block; it does not define or complete an engine
 * transaction. History navigation is refused while the completion callback runs.
 */
internal class TimelineImpl(
    private val gameWorld: GameWorld,
    private val changer: Changer,
    private val transactions: Transactions,
) : Timeline {

  override fun checkpoint(): Checkpoint {
    check(!transactions.isOpen) { "Cannot expose a checkpoint during a transaction" }
    return Checkpoint((gameWorld.events.lastTransaction?.serial ?: -1) + 1)
  }

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

  private val future = mutableListOf<GameEvent>()
  private val atomicStarts = mutableListOf<Int>()

  override val positions: List<Checkpoint>
    get() {
      check(!transactions.isOpen) { "Cannot expose checkpoints during a transaction" }
      val last = future.lastOrNull()?.transaction ?: gameWorld.events.lastTransaction
      return (0..((last?.serial ?: -1) + 1)).map(::Checkpoint)
    }

  internal val atEnd: Boolean
    get() = future.isEmpty()

  private fun eventOrdinal(position: Checkpoint): Int {
    require(position in positions) { "No transaction position $position" }
    if (position.serial <= checkpoint().serial) return gameWorld.events.eventOrdinal(position)
    return future.firstOrNull { it.transaction?.serial == position.serial }?.ordinal
        ?: (gameWorld.nextOrdinal + future.size)
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
    val ordinal = eventOrdinal(checkpoint)
    check(atomicStarts.lastOrNull()?.let { ordinal >= it } != false) {
      "Cannot seek before the current atomic call began"
    }
    require(checkpoint.serial >= floor.serial) {
      "Cannot seek to $ordinal; committed through ${floor.serial}"
    }
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
    val previous =
        positions.lastOrNull {
          it.serial < checkpoint().serial &&
              it.serial >= floor.serial &&
              eventOrdinal(it) >= (atomicStarts.lastOrNull() ?: 0)
        } ?: return false
    seek(previous)
    return true
  }

  override fun stepForward(): Boolean {
    val next = positions.firstOrNull { it.serial > checkpoint().serial } ?: return false
    seek(next)
    return true
  }

  override fun truncate() {
    requireNotReportingCompletion()
    checkpoint() // Reject navigation during an open transaction.
    future.clear()
  }

  /** Returns a speculative World to its source point without reversing its local event trail. */
  internal fun resetOverlay() {
    check(atomicStarts.isEmpty()) { "cannot reset during an atomic block" }
    changer.resetOverlay {
      floor = checkpoint()
      future.clear()
    }
  }

  internal fun rollBackStateTo(ordinal: Int) {
    changer.rollBackTo(ordinal)
    future.clear()
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
