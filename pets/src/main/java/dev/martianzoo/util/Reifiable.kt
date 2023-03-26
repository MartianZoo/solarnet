package dev.martianzoo.util

import dev.martianzoo.tfm.api.Exceptions.InvalidReificationException

interface Reifiable<R : Reifiable<R>> {

  /**
   * Completes normally if this is a minimal concrete narrowing of [abstractTarget]. Rules:
   *
   * * `this` must not be abstract
   * * if [abstractTarget] is concrete then `this` must equal it
   * * `this` must be a narrowing of [abstractTarget]
   * * considering all reifications of [abstractTarget], this must not be a proper narrowing of any
   *   of them
   */
  fun ensureReifies(abstractTarget: R) {
    if (abstract) throw InvalidReificationException("abstract")
    if (!abstractTarget.abstract && abstractTarget != this) {
      throw InvalidReificationException("already concrete: $abstractTarget; can't reify to $this")
    }
    this.ensureNarrows(abstractTarget)
  }

  /** Completes normally if this is a narrowing of [that]. */
  fun ensureNarrows(that: R)

  val abstract: Boolean
}
