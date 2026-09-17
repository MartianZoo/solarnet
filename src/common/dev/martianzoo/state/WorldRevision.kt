package dev.martianzoo.state

import kotlin.jvm.JvmInline

/**
 * Identifies one exact revision of a live world's event-backed state. Unlike a [Checkpoint], a
 * revision is never reused after rollback.
 */
@JvmInline
public value class WorldRevision private constructor(private val sequence: Long) {
  internal fun next(): WorldRevision {
    check(sequence < Long.MAX_VALUE) { "world revision exhausted" }
    return WorldRevision(sequence + 1)
  }

  internal companion object {
    internal val INITIAL: WorldRevision = WorldRevision(0)
  }
}
