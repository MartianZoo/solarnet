package dev.martianzoo.state

import dev.martianzoo.pets.ast.Expression

/**
 * An event at [ordinal] in a world's exact history. Task events edit the pending queue; change
 * events edit the component multiset. [notes] is optional commentary, not gameplay state.
 */
public sealed class GameEvent private constructor() {
  /** Zero-based position of this event in its world's history. */
  public abstract val ordinal: Int

  /** Mutable commentary excluded from this event's value equality and all gameplay semantics. */
  public var notes: String? = null

  /** A task-queue change whose [task] is the added, removed, or replacement task. */
  public sealed class TaskEvent : GameEvent() {
    /** Task state after an add or edit, or before a removal. */
    public abstract val task: Task

    internal fun taskToString() = buildString {
      append("$ordinal: +Task${task.id} { ${task.instruction}")
      task.then?.let { append(" THEN $it") }
      append(" }")
    }
  }

  /** Adds [task] at [ordinal]; its id must encode the same ordinal. */
  public data class TaskAddedEvent(
      override val ordinal: Int,
      override val task: Task,
  ) : TaskEvent() {
    init {
      require(task.id.ordinal == ordinal)
    }

    override fun toString(): String = taskToString()
  }

  /** Removes the previously pending [task] at [ordinal]. */
  public data class TaskRemovedEvent(
      override val ordinal: Int,
      override val task: Task,
  ) : TaskEvent() {
    override fun toString(): String = "$ordinal: -Task${task.id}"
  }

  /** Replaces [oldTask] with [task] at [ordinal], preserving its task id. */
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

        /**
         * The ordinal of the change event that activated the effect. Following this field through
         * earlier [ChangeEvent.cause] values reconstructs causal event ancestry until a manual
         * change with no cause is reached.
         */
        val triggerEvent: Int,
    ) {
      init {
        require(triggerEvent >= 0)
      }

      override fun toString(): String = "VIA $context BECAUSE $triggerEvent"
    }
  }
}

internal fun GameEvent.snapshot(): GameEvent =
    when (this) {
      is GameEvent.ChangeEvent -> copy()
      is GameEvent.TaskAddedEvent -> copy()
      is GameEvent.TaskEditedEvent -> copy()
      is GameEvent.TaskRemovedEvent -> copy()
    }.also { copy -> copy.notes = notes }
