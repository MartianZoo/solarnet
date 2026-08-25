package dev.martianzoo.engine

import kotlin.jvm.JvmInline

/**
 * Identifies one exact revision of a live world's event-backed state.
 *
 * Unlike a [Timeline.Checkpoint], a revision is never reused after rollback. It is therefore safe
 * to retain when later code needs to detect that the same backing world changed in any way.
 */
@JvmInline
// TODO: Contract this temporary tfm-tests seam.
public value class WorldRevision private constructor(private val sequence: Long) {
  internal fun next(): WorldRevision {
    check(sequence < Long.MAX_VALUE) { "world revision exhausted" }
    return WorldRevision(sequence + 1)
  }

  internal companion object {
    internal val INITIAL: WorldRevision = WorldRevision(0)
  }
}
