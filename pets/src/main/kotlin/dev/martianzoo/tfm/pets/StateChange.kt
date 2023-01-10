package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class StateChange(
    /** Matches this object's position in the game history list, 1-referenced. */
    val ordinal: Int,

    /**
     * How many of the component were gained/removed/transmuted. A positive integer. Often
     * 1, since many component types don't admit duplicates.
     */
    val count: Int = 1,

    /** The concrete component that was gained, or `null` if this was a remove. */
    val gaining: GenericTypeExpression? = null,

    /**
     * The concrete component that was removed, or `null` if this was a gain. Can't be the same as
     * `gained` (e.g. both can't be null).
     */
    val removing: GenericTypeExpression? = null,

    /** Information about what caused this state change, if we have it. */
    val cause: Cause? = null,
) {

  init {
    require(ordinal >= 0) // 0 used only for undocked changes
    require(count > 0)
    require(gaining != removing) { "both gaining and removing $gaining" }
    require((cause?.change ?: 0) < ordinal) { "${cause!!.change} >= $ordinal" }
  }

  override fun toString(): String {
    var desc = "$ordinal: "
    when (gaining) {
      null -> desc += "-$count $removing"
      else -> {
        desc += "$count $gaining"
        if (removing != null) desc += " FROM $removing"
      }
    }

    val a = cause?.agent ?: "Unknown"
    val c = cause?.change ?: "Unknown"
    return "$desc BY $a BECAUSE $c"
  }

  data class Cause(
      /** The concrete component that owns the instruction that caused this change. */
      val agent: TypeExpression,

      /** The ordinal of the previous change which triggered that instruction. */
      val change: Int,
  ) {
    init {
      require(change > 0)
    }
  }
}
