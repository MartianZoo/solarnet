package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.pre

data class ChangeLogEntry(
    val ordinal: Int,
    val change: StateChange,
    val cause: Cause? = null,
    val hidden: Boolean = false
) {
  init {
    require(ordinal >= 0)
    if (cause != null) {
      require(cause.trigger < ordinal)
    }
  }

  override fun toString() = "$ordinal: $change${cause.pre(' ')}"

  data class StateChange(
      /**
       * How many of the component were gained/removed/transmuted. A positive integer. Often 1,
       * since many component types don't admit duplicates.
       */
      val count: Int = 1,

      /** The concrete component that was gained, or `null` if this was a remove. */
      val gaining: TypeExpr? = null,

      /**
       * The concrete component that was removed, or `null` if this was a gain. Can't be the same as
       * `gained` (e.g. both can't be null).
       */
      val removing: TypeExpr? = null,
  ) {

    init {
      require(count > 0)
      require(gaining != removing) { "both gaining and removing $gaining" }
    }

    override fun toString() =
        when (gaining) {
          null -> "-$count $removing"
          else -> "$count $gaining${removing.pre(" FROM ")}"
        }
  }

  data class Cause(
      /** The concrete component that owns the instruction that caused this change. */
      val actor: TypeExpr,

      /** The ordinal of the previous change which triggered that instruction. */
      val trigger: Int,
  ) {
    init {
      require(trigger >= 0)
    }

    override fun toString(): String {
      return "BY $actor BECAUSE $trigger"
    }
  }
}
