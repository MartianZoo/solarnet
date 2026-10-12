package dev.martianzoo.pets.ast

/**
 * When dispatched work may be selected, in declaration order: ordinary work at once, `LATER` work
 * once no ordinary work remains, and `FINALLY` work once no ordinary or `LATER` work remains
 * ([rules L2-17 to
 * L2-19](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
 */
public enum class Deferral(public val keyword: String) {
  /** Ordinary work, as `TASK` admits it. */
  NONE("TASK"),

  /** Work admitted by `LATER`. */
  LATER("LATER"),

  /** Work admitted by `FINALLY`. */
  FINALLY("FINALLY"),
}
