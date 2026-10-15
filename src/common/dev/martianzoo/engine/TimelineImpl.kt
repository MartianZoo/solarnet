package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskResult

/**
 * Keeps completed positions and the retained future of a live World. Atomic failures still discard
 * the events made by the failed interaction.
 */
internal class TimelineImpl(
    private val gameWorld: GameWorld,
    private val changer: Changer,
) : Timeline {

  override fun checkpoint() = Checkpoint(gameWorld.nextOrdinal)

  override var floor = Checkpoint(0)
    private set

  private val completedPositions = mutableListOf<Checkpoint>()
  private val future = mutableListOf<GameEvent>()
  private val atomicStarts = mutableListOf<Checkpoint>()

  override val positions: List<Checkpoint>
    get() = completedPositions.toList()

  internal val atEnd: Boolean
    get() = future.isEmpty()

  internal fun recordCompletedPosition() {
    val current = checkpoint()
    if (current in completedPositions) return
    requireAtEnd()
    completedPositions += current
  }

  override fun recordPosition() = recordCompletedPosition()

  internal fun restoreCompletedPositions(positions: List<Checkpoint>) {
    check(completedPositions.isEmpty())
    check(positions.lastOrNull() == checkpoint())
    completedPositions.addAll(positions)
  }

  internal fun requireAtEnd() {
    check(atEnd) { "Truncate the future before changing this World" }
  }

  override fun commit() {
    requireAtEnd()
    floor = checkpoint()
  }

  override fun seek(checkpoint: Checkpoint) {
    check(atomicStarts.lastOrNull()?.let { checkpoint.ordinal >= it.ordinal } != false) {
      "Cannot seek before the current atomic call began"
    }
    check(checkpoint() in completedPositions) { "Cannot seek from an incomplete position" }
    val ordinal = checkpoint.ordinal
    require(ordinal >= floor.ordinal) {
      "Cannot seek to $ordinal; committed through ${floor.ordinal}"
    }
    require(checkpoint in completedPositions) { "Position $ordinal is not completed" }
    val current = gameWorld.nextOrdinal
    when {
      ordinal < current -> {
        val removed = gameWorld.events.entriesSince(checkpoint)
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
    val minimum = maxOf(floor.ordinal, atomicStarts.lastOrNull()?.ordinal ?: floor.ordinal)
    val previous =
        completedPositions.lastOrNull {
          it.ordinal < gameWorld.nextOrdinal && it.ordinal >= minimum
        } ?: return false
    seek(previous)
    return true
  }

  override fun stepForward(): Boolean {
    val next = completedPositions.firstOrNull { it.ordinal > gameWorld.nextOrdinal } ?: return false
    seek(next)
    return true
  }

  override fun truncate() {
    check(checkpoint() in completedPositions) { "Cannot truncate at an incomplete position" }
    future.clear()
    completedPositions.removeAll { it.ordinal > gameWorld.nextOrdinal }
  }

  private fun rollBackStateTo(ordinal: Int) {
    changer.rollBackTo(ordinal)
    future.clear()
    completedPositions.removeAll { it.ordinal > ordinal }
  }

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  override fun atomic(block: () -> Unit): TaskResult {
    requireAtEnd()
    val checkpoint = checkpoint()
    atomicStarts += checkpoint
    try {
      block()
    } catch (_: AbortTransactionException) {
      rollBackStateTo(checkpoint.ordinal)
    } catch (e: Exception) {
      rollBackStateTo(checkpoint.ordinal)
      throw e
    } finally {
      atomicStarts.removeLast()
    }
    return gameWorld.activitySince(checkpoint)
  }

  internal fun initializationFinished() = gameWorld.markSetupStart()
}
