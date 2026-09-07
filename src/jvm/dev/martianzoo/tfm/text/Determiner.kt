package dev.martianzoo.tfm.text

/** A noun-phrase determiner realized against the selected noun form. */
internal enum class Determiner(private val fixedText: String? = null) {
  INDEFINITE,
  ANY("any"),
  YOUR("your"),
  NO("no"),
  THE("the"),
  THIS("this"),
  THAT("that"),
  ANOTHER("another"),
  OPPONENT_POSSESSIVE("an opponent's"),
  MOST("most"),
  HIGHEST("highest"),
  ;

  fun linearize(noun: String): String =
      fixedText ?: if (noun.first().lowercaseChar() in "aeiou") "an" else "a"
}
