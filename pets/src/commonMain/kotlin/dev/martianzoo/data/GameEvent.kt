package dev.martianzoo.data

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.util.pre

public sealed class GameEvent {
  public abstract val ordinal: Int

  public sealed class TaskEvent : GameEvent() {
    public abstract val task: Task

    /** The assignment recorded by this task lifecycle event. */
    internal val assignee: Actor
      get() = task.assignee

    internal fun taskToString() = buildString {
      append("$ordinal: +Task${task.id} { ${task.instruction}")
      task.then?.let { append(" THEN $it") }
      append(" }")
      task.whyPending?.let { append(" ($it)") }
    }
  }

  public data class TaskAddedEvent(override val ordinal: Int, override val task: Task) :
      TaskEvent() {
    init {
      require(task.id.ordinal == ordinal)
    }

    override fun toString(): String = taskToString()
  }

  public data class TaskRemovedEvent(override val ordinal: Int, override val task: Task) :
      TaskEvent() {
    override fun toString(): String = "$ordinal: -Task${task.id}"
  }

  public data class TaskEditedEvent(
      override val ordinal: Int,
      val oldTask: Task,
      override val task: Task,
  ) : TaskEvent() {
    init {
      require(task.id == oldTask.id)
    }

    override fun toString(): String = taskToString() + " FROM Task${task.id}"
  }

  /** All interesting information about a state change that happened in a game. */
  public data class ChangeEvent(
      override val ordinal: Int,
      /** The Actor recorded as having performed [change]. */
      val actor: Actor,
      val change: StateChange,
      val cause: Cause?,
  ) : GameEvent() {
    init {
      require(ordinal >= 0)
      require((cause?.triggerEvent ?: -1) < ordinal)
    }

    override fun toString(): String = buildString {
      append("$ordinal: $change BY $actor")
      append(" ${cause ?: "(manual)"}")
    }

    /** The part of a `ChangeEvent` that describes only what actually changed. */
    public data class StateChange(
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
    public data class Cause(
        /** The type of the existing component the activated effect belonged to. */
        val context: Expression,

        /** The ordinal of the previous event which this event was triggered in response to. */
        val triggerEvent: Int,
    ) {
      init {
        require(triggerEvent >= 0)
      }

      override fun toString(): String = "VIA $context BECAUSE $triggerEvent"
    }
  }
}
