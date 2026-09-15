package dev.martianzoo.state

/** One event-prefix position in a game world. */
public data class Checkpoint(public val ordinal: Int) {
  init {
    require(ordinal >= 0)
  }

  override fun toString(): String = "$ordinal"
}
