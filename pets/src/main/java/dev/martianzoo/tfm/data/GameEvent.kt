package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.wrap

sealed class GameEvent { // TODO move to data? Organize?
  abstract val ordinal: Int
  abstract val player: Player

  sealed class TaskEvent : GameEvent() {
    abstract val task: Task
  }
  data class TaskAddedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
    override val player by task::player
    override fun toString() =
        "$ordinal: +Task${task.id} { ${task.instruction} } ${task.cause}" +
            task.whyPending.wrap(" (", ")")
  }

  data class TaskRemovedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
    override val player by task::player
    override fun toString() = "$ordinal: -Task${task.id}"
  }

  data class TaskReplacedEvent(
      override val ordinal: Int,
      val oldTask: Task,
      override val task: Task
  ) : TaskEvent() {
    init {
      require(task.id == oldTask.id)
    }
    override val player by task::player
    override fun toString() =
        "$ordinal: Task${task.id} { ${task.instruction }" +
            " (${task.whyPending}) FROM Task${task.id}"
  }

  /** All interesting information about a state change that happened in a game. */
  data class ChangeEvent(
      override val ordinal: Int,
      override val player: Player,
      val change: StateChange,
      val cause: Cause?
  ) : GameEvent() {
    init {
      require(ordinal >= 0)
      require((cause?.triggerEvent ?: -1) < ordinal)
    }

    override fun toString() = "$ordinal: $change FOR $player ${cause ?: "(manual)"}"

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
