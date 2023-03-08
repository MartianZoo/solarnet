package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.util.pre

/** All interesting information about an event in a game history. */
data class ChangeRecord(
    val ordinal: Int,
    val change: StateChange,
    val cause: Cause? = null,
    val hidden: Boolean = false
) {
  init {
    require(ordinal >= 0)
    if (cause != null) {
      require(cause.trigger < ordinal) { "${cause.trigger} should be < $ordinal"}
    }
  }

  override fun toString() = "$ordinal: $change${cause.pre(' ')}"

  /** The part that describes only what actually changed. */
  data class StateChange(
      /**
       * How many of the component were gained/removed/transmuted. A positive integer. Often 1,
       * since many component types don't admit duplicates.
       */
      val count: Int = 1,

      /** The concrete component that was gained, or `null` if this was a remove. */
      val gaining: Expression? = null,

      /**
       * The concrete component that was removed, or `null` if this was a gain. Can't be the same as
       * `gained` (e.g. both can't be null).
       */
      val removing: Expression? = null,
  ) {
    init {
      require(count > 0)
      require(gaining != removing) { "both gaining and removing $gaining" }
    }

    fun inverse() = copy(gaining = removing, removing = gaining)

    override fun toString() =
        when (gaining) {
          null -> "-$count $removing"
          else -> "$count $gaining${removing.pre(" FROM ")}"
        }
  }

  /** The part that describes why it changed. */
  data class Cause(
      /** The concrete component that owns the instruction that caused this change. */
      val actor: Expression,

      /** The ordinal of the previous change which triggered that instruction. */
      val trigger: Int,
  ) {
    companion object {
      fun from(hasEx: HasExpression, trigger: Int) = Cause(hasEx.expressionFull, trigger)
    }
    init {
      require(trigger >= 0)
    }

    override fun toString(): String {
      return "BY $actor BECAUSE $trigger"
    }
  }
}
