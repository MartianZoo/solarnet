package dev.martianzoo.util

import dev.martianzoo.tfm.api.TypeInfo

interface Reifiable<R : Reifiable<R>> {
  /** Completes normally if this is a narrowing of [that]. */
  fun ensureNarrows(that: R, info: TypeInfo)

  val abstract: Boolean
}
