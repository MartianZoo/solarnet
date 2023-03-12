package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasExpression

sealed class LogEntry {

  data class TaskAddedEvent(val task: Task) : LogEntry()

  data class TaskRemovedEvent(val task: Task) : LogEntry()

  data class TaskReplacedEvent(val id: TaskId, val oldTask: Task, val newTask: Task) : LogEntry() {
    init {
      require(oldTask.id == id)
      require(newTask.id == id)
    }
  }

  /** All interesting information about a state change that happened in a game. */
  data class ChangeEvent(val ordinal: Int, val change: StateChange, val cause: Cause? = null) :
      LogEntry() {
    init {
      require(ordinal >= 0)
      if (cause != null) {
        require((cause.triggerEvent ?: -1) < ordinal)
      }
    }

    override fun toString() = "$ordinal: $change ${cause ?: "(by fiat)"}"

    /** The part that describes why it changed. */
    data class Cause(
        /**
         * The type of the existing component that owns the effect activated in response to
         * [triggerEvent]. For an "initiated" change this should be `Game` or the appropriate
         * player.
         */
        val context: Expression,

        /**
         * The ordinal of the previous change which triggered this to happen, or `null` if this was
         * done ex machina.
         */
        val triggerEvent: Int?, // TODO remove ? here and below

        /**
         * The player who owns (or *is*) the [context] component, or if none, the actor for the
         * [triggerEvent]. Tasks initiated by the engine itself use `Game`.
         */
        val actor: Actor?,
    ) {
      constructor(context: HasExpression, triggerEvent: ChangeEvent?, actor: Actor?) :
          this(context.expressionFull, triggerEvent?.ordinal, actor)

      init {
        require((triggerEvent ?: 0) >= 0)
      }

      override fun toString(): String {
        return buildString {
          append("BY $context")
          actor?.let { append(" FOR $it") }
          triggerEvent?.let { append(" BECAUSE $it") }
        }
      }
    }
  }
}
