package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo

/**
 * A value that can retain choices and be replaced by a narrower value of the same kind.
 *
 * Narrowing is independent of state-driven resolution. An unresolved specification can be
 * non-abstract when resolution has only one possible result, or can be narrowed while preserving
 * the unresolved parts that are not choices.
 */
public interface Specification<S : Specification<S>> {
  /** Whether this specification still requires an externally supplied choice. */
  public fun isAbstract(info: TypeInfo): Boolean

  /** Completes normally if this specification narrows [that]. */
  public fun ensureNarrows(that: S, info: TypeInfo)

  /**
   * Returns whether this specification narrows [that] — the boolean form of [ensureNarrows], per
   * [rule L7-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open).
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
