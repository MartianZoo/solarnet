package dev.martianzoo.state

/**
 * The position before transaction [serial], or the end before the next transaction. Serials address
 * retained history; clients discard checkpoints into a truncated future.
 */
public data class Checkpoint(public val serial: Int) {
  init {
    require(serial >= 0)
  }

  override fun toString(): String = "$serial"
}
