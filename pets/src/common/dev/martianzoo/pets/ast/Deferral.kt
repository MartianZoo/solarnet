package dev.martianzoo.pets.ast

/**
 * How dispatched work enters the queue: selected work, ordinary work, `LATER` work once no ordinary
 * work remains, and `FINALLY` work once no ordinary or `LATER` work remains ([rules L2-17 to
 * L2-19](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md#2-instructions)).
 */
public enum class Deferral(public val keyword: String) {
  /** A single selected task, as `NOW` admits it; stored tasks use SOON plus their selected flag. */
  NOW("NOW"),

  /** Ordinary queue priority, whether admitted directly or by `SOON`. */
  SOON("SOON"),

  /** Work admitted by `LATER`. */
  LATER("LATER"),

  /** Work admitted by `FINALLY`. */
  FINALLY("FINALLY"),
}
