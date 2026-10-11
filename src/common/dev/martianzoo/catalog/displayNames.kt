package dev.martianzoo.catalog

import dev.martianzoo.pets.ast.ClassName

// Pets itself has one namespace of canonical Class Names and no notion of display ([rule
// L10-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-names));
// which name a concept gets, and how it is shown to a person, is `NAMING.md`'s
// subject, and this file implements only the lookup.

/** The language tag used for display-name fallback. */
public const val ENGLISH: String = "en"

/**
 * Returns [name]'s display name using [locale], then its parent language tags, then [ENGLISH]. Tags
 * are compared case-insensitively with `_` treated as `-`; a missing entry falls back to
 * [defaultEnglishDisplayName]. For example, `fr-CA` tries `fr-CA`, then `fr`, then `en`.
 */
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

/**
 * Derives English display text from [name] without catalog metadata: underscores become spaces, and
 * a space is inserted at a lower-to-upper or letter-to-digit transition. For example, `Player2_Hex`
 * becomes `Player 2 Hex`.
 */
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
