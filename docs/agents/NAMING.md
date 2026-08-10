# Class Naming

Every class has exactly one engine-facing `ClassName`. Rules, declarations, Kotlin implementations,
events, saved state, and engine APIs use that canonical name. Numerous structured definitions use
language-neutral names (`Card072`, `MilestoneHM6`, `AwardBA1`, `ColonyTile03`). Standard actions
and projects instead use semantic canonical names such as `PlayCardSA` and `AsteroidSP`.

Bundles keep presentation text in `language/<tag>.json5`. Each file is an object from canonical
class name to natural-language display name. Vocabulary lookup follows the requested locale from
most specific to least specific and then English, independently for each entry. Entries are grouped
by content category and sorted by canonical name within each group; milestones precede awards and
cards are last.

Each engine world owns a session `Vocabulary`. It provides:

- `canonicalName` and `canonicalize` for localized Pets input and separately configured input-only
  synonyms;
- `displayName` for ordinary natural-language UI;
- `petsName` and `renderPets` for localized, parseable Pets-oriented output.

Input-only synonyms are never rendering candidates. For cards, milestones, awards, colonies, and
similar structured content, the Pets name is derived only from the effective localized display
name using the [Google Java Style camel-case conversion][camel-case]: remove apostrophes, split on
other punctuation and whitespace and on conventional internal camel-case boundaries, lowercase
every word, uppercase each word's first character, and join the words while retaining supported
letters, marks, and digits. Thus `UNMI Contractor` becomes `UnmiContractor`, `PolderTECH Dutch`
becomes `PolderTechDutch`, and `Asteroid (Card)` becomes `AsteroidCard`. The current common
implementation does not promise Unicode normalization or supplementary-code-point handling.
There are no per-entry Pets-name overrides. Vocabulary construction rejects alternate/canonical
collisions.

[camel-case]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case

`ClassName` itself accepts an ASCII uppercase letter followed by ASCII letters, digits, or
underscores. UI code must render through the session vocabulary rather than relying on canonical
`toString()` output.
