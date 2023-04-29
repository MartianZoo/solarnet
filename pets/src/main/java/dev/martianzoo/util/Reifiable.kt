package dev.martianzoo.util

import dev.martianzoo.tfm.api.ExpressionInfo

interface Reifiable<R : Reifiable<R>> {
  /** Completes normally if this is a narrowing of [that]. */
  fun ensureNarrows(that: R, einfo: ExpressionInfo)

  val abstract: Boolean
}
