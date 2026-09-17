package dev.martianzoo.state

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor

public sealed class GameEvent {
  public abstract val ordinal: Int

  /** Mutable commentary excluded from this event's value equality and all gameplay semantics. */
  public var notes: String? = null

  public sealed class TaskEvent : GameEvent() {
    public abstract val task: Task

    internal fun taskToString() = buildString {
      append("$ordinal: +Task${task.id} { ${task.instruction}")
      task.then?.let { append(" THEN $it") }
      append(" }")
    }
  }

  public data class TaskAddedEvent(
      override val ordinal: Int,
      override val task: Task,
  ) : TaskEvent() {
    init {
      require(task.id.ordinal == ordinal)
    }

    override fun toString(): String = taskToString()
  }

  public data class TaskRemovedEvent(
      override val ordinal: Int,
      override val task: Task,
  ) : TaskEvent() {
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
      val change: ComponentChange,
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
