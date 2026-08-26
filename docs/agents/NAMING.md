# Class names and vocabulary

> **Read when:** naming or renaming a Class, resolving a printed-name collision, changing player
> aliases, or deciding whether a concept needs a separate identifier.
>
> **Skip when:** changing only grammatical wording; use [LANGUAGE.md](LANGUAGE.md).
>
> **Status:** current model.

## Source map

- [`ClassName.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/ast/ClassName.kt) — inspect
  syntax and canonical identity constraints.
- [`Vocabulary.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/Vocabulary.kt) — read when
  changing display lookup, aliases, or inheritance.
- [`GamePremise.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/data/GamePremise.kt) —
  search for `playerNames` only when changing configured Player display names.
- Bundle `language/en.json5` files under
  [`tfm-canon/src/commonMain/resources/canon/bundles`](../../tfm-canon/src/commonMain/resources/canon/bundles)
  own localized printed names; inspect only the bundle containing the renamed Class.

Every class has one engine-facing `ClassName`. Pets, declarations, Kotlin implementations, events,
saved state, and engine APIs use it. A `ClassName` begins with an ASCII uppercase letter and then
contains ASCII letters, digits, or underscores. Pets keywords are reserved; keywords parsed without
case sensitivity, such as `HAS`, are unavailable in every capitalization.

Structured content uses globally unique semantic English names such as `Birds`, `Builder7`,
`Landlord`, and `Enceladus`. A definition's Class Name is its sole identity; there is no separate
card, milestone, award, colony, or standard-action identifier. Replacement relationships name the
replaced Class directly.

When printed English names collide, add the smallest meaningful qualifier. Revised milestones use
their threshold, as in `Builder7` and `Builder8`; revised promo cards use `Promo`, as in
`DeimosDownPromo`. Derived card-local classes append their structural suffix to the card name, as in
`NaturalPreserve_SpecialTile`.

Each Game World owns a locale-specific `Vocabulary`:

- `canonicalName` and `canonicalize` resolve localized Pets input and input-only synonyms;
- `displayName` produces ordinary UI text; and
- `petsName` and `renderPets` produce localized, parseable Pets.

Bundle files at `language/<tag>.json5` map Class Names to display names. Lookup falls back from the
requested locale to less-specific locales and then English, independently for each entry. Keep
entries grouped by content category and sorted by Class Name within each group; milestones precede
awards and cards are last.

English display text defaults to the canonical Class Name with its words separated; English language
files contain only exceptions to that rule. English Pets uses the canonical Class Name directly.
Other locales derive a Pets name from the effective localized display name with the [Google Java Style camel-case conversion][camel-case]:
remove apostrophes, split on other punctuation, whitespace, and conventional internal camel-case
boundaries, lowercase each word, capitalize its first character, and join. The current
implementation accepts ASCII display text only. Examples: `UNMI Contractor` becomes
`UnmiContractor`, `PolderTECH Dutch` becomes `PolderTechDutch`, and `Asteroid (Card)` becomes
`AsteroidCard`. A localized name that collides with another canonical Class Name falls back to the
canonical name.

There are no per-entry Pets-name overrides. Input-only synonyms never become rendering candidates.
Vocabulary construction rejects collisions among Class Names, localized Pets names, and synonyms.
Display text remains presentation rather than identity; UI code must render through the session
Vocabulary rather than `ClassName.toString()`.

There is no Unicode normalization because non-ASCII display text is currently rejected.

[camel-case]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case
