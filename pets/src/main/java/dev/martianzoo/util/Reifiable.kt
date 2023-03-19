package dev.martianzoo.util

import dev.martianzoo.tfm.api.Exceptions.InvalidReificationException

interface Reifiable<R : Reifiable<R>> {

  /**
   * Completes normally if this is a minimal concrete narrowing of [that]. Rules:
   *
   * * `this` must not be abstract
   * * if [that] is concrete then `this` must equal it
   * * `this` must be a narrowing of [that]
   * * considering all reifications of [that], this must not be a proper narrowing of any of them
   */
  fun ensureReifies(that: R) {
    if (abstract) throw InvalidReificationException("abstract")
    if (!that.abstract && that != this) throw InvalidReificationException("already concrete")
    this.ensureNarrows(that)
  }

  /**
   * Completes normally if this is a narrowing of [that].
   */
  fun ensureNarrows(that: R)

  val abstract: Boolean
}
