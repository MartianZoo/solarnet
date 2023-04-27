package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.wrap

sealed class GameEvent {

  abstract val ordinal: Int
  abstract val player: Player

  sealed class TaskEvent : GameEvent() {
    abstract val task: Task
    override val player by task::player
  }
  data class TaskAddedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
    override fun toString() =
        "$ordinal: +Task${task.id} { ${task.instruction} } ${task.cause}" +
            task.whyPending.wrap(" (", ")")
  }

  data class TaskRemovedEvent(override val ordinal: Int, override val task: Task) : TaskEvent() {
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

    /** The part that describes why it changed -- if there WAS a reason! */
    data class Cause
    constructor(
        /**
         * The type of the existing component that owns the effect activated in response to
         * [triggerEvent]. For an "initiated" change this should be `Engine` or the appropriate
         * player.
         */
        val context: Expression,

        /**
         * The ordinal of the previous change which triggered this to happen, or `null` if this was
         * done ex machina.
         */
        val triggerEvent: Int,
    ) {
      constructor(
          context: HasExpression,
          triggerEvent: ChangeEvent,
      ) : this(context.expression, triggerEvent.ordinal)

      init {
        require(triggerEvent >= 0)
      }

      override fun toString() = "BY $context BECAUSE $triggerEvent"
    }
  }
}
