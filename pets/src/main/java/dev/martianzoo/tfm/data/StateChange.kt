package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.pre

/** The part of a `ChangeEvent` that describes only what actually changed. */
data class StateChange(
    /**
     * How many of the component were gained/removed/transmuted. A positive integer. Often 1, since
     * many component types don't admit duplicates.
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

  override fun toString(): String {
    val ct = if (count == 1) "" else "$count "
    return when (gaining) {
      null -> "-$ct$removing"
      else -> "+$ct$gaining${removing.pre(" FROM ")}"
    }
  }
}
