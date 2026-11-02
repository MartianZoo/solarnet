package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo

/**
 * A value that can retain choices and be replaced by a narrower value of the same kind.
 *
 * Narrowing is independent of state-driven evaluation. A specification can be narrowed while
 * preserving parts that still need evaluation. Types retain their abstract/concrete vocabulary;
 * instructions use "needs narrowing" for the separate question of an unfixed choice.
 */
public interface Specification<S : Specification<S>> {
  /** Completes normally if this specification narrows [that]. */
  public fun ensureNarrows(that: S, info: TypeInfo)

  /**
   * Returns whether this specification narrows [that] — the boolean form of [ensureNarrows], per
   * [rule L3-10](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md#3-narrowing).
   * Only a narrowing failure becomes `false`; a failure caused by something else, such as an
   * unknown class or a malformed proposal, propagates, so broken input stays distinguishable from a
   * well-formed move the current state simply forbids.
   */
  public fun narrows(that: S, info: TypeInfo): Boolean =
      try {
        ensureNarrows(that, info)
        true
      } catch (_: NarrowingException) {
        false
      }
}
