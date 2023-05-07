package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.pre
import dev.martianzoo.util.wrap

sealed class GameEvent { // TODO move to data? Organize?
  abstract val ordinal: Int
  abstract val owner: Player

  sealed class TaskEvent : GameEvent() {
    abstract val task: Task
  }

  data class TaskAddedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
    override val owner by task::owner
    override fun toString() =
        "$ordinal: +Task${task.id} { ${task.instruction} } ${task.cause}" +
            task.whyPending.wrap(" (", ")")
  }

  data class TaskRemovedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
    override val owner by task::owner
    override fun toString() = "$ordinal: -Task${task.id}"
  }

  data class TaskReplacedEvent(
      override val ordinal: Int,
      val oldTask: Task,
      override val task: Task,
  ) : TaskEvent() {
    init {
      require(task.id == oldTask.id)
    }

    override val owner by task::owner
    override fun toString() =
        "$ordinal: Task${task.id} { ${task.instruction}" +
            " (${task.whyPending}) FROM Task${task.id}"
  }

  /** All interesting information about a state change that happened in a game. */
  data class ChangeEvent(
      override val ordinal: Int,
      override val owner: Player,
      val change: StateChange,
      val cause: Cause?,
  ) : GameEvent() {
    init {
      require(ordinal >= 0)
      require((cause?.triggerEvent ?: -1) < ordinal)
    }

    override fun toString() = "$ordinal: $change FOR $owner ${cause ?: "(manual)"}"

    /** The part of a `ChangeEvent` that describes only what actually changed. */
    data class StateChange(
        /**
         * How many of the component were gained/removed/transmuted. A positive integer. Often 1,
         * since many component types don't admit duplicates.
         */
        val count: Int = 1,

        /** The concrete component that was gained, or `null` if this was a remove. */
        val gaining: Expression? = null,

        /**
         * The concrete component that was removed, or `null` if this was a gain. Can't be the same
         * as `gained` (e.g. both can't be null).
         */
        val removing: Expression? = null,
    ) {
      init {
        require(count > 0)
        require(gaining != removing) { "both gaining and removing $gaining" }
      }

      override fun toString(): String {
        val ct = if (count == 1) "" else "$count "
        return when (gaining) {
          null -> "-$ct$removing"
          else -> "+$ct$gaining${removing.pre(" FROM ")}"
        }
      }
    }

    /** Why a (non-manual) `ChangeEvent` happened. */
    data class Cause(
        /** The type of the existing component the activated effect belonged to. */
        val context: Expression,

        /** The ordinal of the previous event which this event was triggered in response to. */
        val triggerEvent: Int,
    ) {
      init {
        require(triggerEvent >= 0)
      }

      override fun toString() = "BY $context BECAUSE $triggerEvent"
    }
  }
}
