package dev.martianzoo.util

import dev.martianzoo.api.TypeInfo

public interface Reifiable<R : Reifiable<R>> {
  /** Completes normally if this is a narrowing of [that]. */
  public fun ensureNarrows(that: R, info: TypeInfo)

  public val abstract: Boolean
}
