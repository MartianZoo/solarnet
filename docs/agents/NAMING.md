# Class names and vocabulary

**Status: current model.**

Every class has one engine-facing `ClassName`. Pets, declarations, Kotlin implementations, events,
saved state, and engine APIs use it. A `ClassName` begins with an ASCII uppercase letter and then
contains ASCII letters, digits, or underscores.

Structured content commonly uses language-neutral names such as `Card072`, `MilestoneHM6`,
`AwardBA1`, and `ColonyTile03`. Rules and standard actions usually use semantic names such as
`PlayCardSA` and `AsteroidSP`. Never use display text as persistent identity.

Each Game World owns a locale-specific `Vocabulary`:

- `canonicalName` and `canonicalize` resolve localized Pets input and input-only synonyms;
- `displayName` produces ordinary UI text; and
- `petsName` and `renderPets` produce localized, parseable Pets.

Bundle files at `language/<tag>.json5` map Class Names to display names. Lookup falls back from the
requested locale to less-specific locales and then English, independently for each entry. Keep
entries grouped by content category and sorted by Class Name within each group; milestones precede
awards and cards are last.

For structured content, the Pets name is derived from the effective localized display name with the
[Google Java Style camel-case conversion][camel-case]: remove apostrophes, split on other punctuation,
whitespace, and conventional internal camel-case boundaries, lowercase each word, capitalize its
first character, and join. The current implementation accepts ASCII display text only. Examples:
`UNMI Contractor` becomes `UnmiContractor`, `PolderTECH Dutch` becomes `PolderTechDutch`, and
`Asteroid (Card)` becomes `AsteroidCard`.

There are no per-entry Pets-name overrides. Input-only synonyms never become rendering candidates.
Vocabulary construction rejects collisions among Class Names, localized Pets names, and synonyms.
UI code must render through the session Vocabulary rather than `ClassName.toString()`.

There is no Unicode normalization because non-ASCII display text is currently rejected.

[camel-case]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case
