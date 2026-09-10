package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.Catalog

/** The language tag used for display-name fallback. */
public const val ENGLISH: String = "en"

/** Returns [name]'s natural-language display name in [locale], with per-entry English fallback. */
public fun displayName(
    catalog: Catalog,
    name: ClassName,
    locale: String = ENGLISH,
): String {
  val normalizedLocale = locale.replace('_', '-').lowercase()
  val namesByLanguage =
      catalog.displayNamesByLanguage.mapKeys { (language) ->
        language.replace('_', '-').lowercase()
      }
  val fallbackChain =
      generateSequence(normalizedLocale) { current ->
            current.substringBeforeLast('-', missingDelimiterValue = "").ifEmpty { null }
          }
          .toList() + ENGLISH
  return fallbackChain.firstNotNullOfOrNull { namesByLanguage[it]?.get(name) }
      ?: defaultEnglishDisplayName(name)
}

/** Derives ordinary English display text by separating the words in [name]. */
public fun defaultEnglishDisplayName(name: ClassName): String {
  val source = name.toString()
  return buildString {
    source.forEachIndexed { index, character ->
      val previous = source.getOrNull(index - 1)
      val startsWord =
          index > 0 &&
              character != '_' &&
              previous != '_' &&
              ((character.isUpperCase() && previous?.isLowerCase() == true) ||
                  (character.isDigit() && previous?.isDigit() == false) ||
                  (!character.isDigit() && previous?.isDigit() == true))
      if ((character == '_' || startsWord) && lastOrNull() != ' ') append(' ')
      if (character != '_') append(character)
    }
  }
}
