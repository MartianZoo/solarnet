# Class names and vocabulary

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** naming or renaming a Class, resolving a printed-name collision, adding a Module,
> changing player aliases, editing a `language/*.json5` file, or deciding whether a concept needs a
> separate identifier.
>
> **Skip when:** changing only grammatical wording; use [LANGUAGE.md](LANGUAGE.md).
>
> **Status:** current model. [Pending naming work](#pending-naming-work) records unresolved names or
> mechanisms; settle each one before implementation.

## Source map

- [`ClassName.kt`](../../src/common/dev/martianzoo/pets/ast/ClassName.kt) — inspect syntax and
  canonical identity constraints.
- [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt) — the root
  vocabulary every Catalog inherits.
- [`Vocabulary.kt`](../../src/common/dev/martianzoo/pets/Vocabulary.kt) — read when changing display
  lookup, aliases, or inheritance; `defaultEnglishDisplayName` defines the display default.
- [`GamePremise.kt`](../../src/common/dev/martianzoo/pets/data/GamePremise.kt) — search for
  `playerNames` only when changing configured Player display names.
- [`Bundle.kt`](../../src/common/dev/martianzoo/tfm/canon/Bundle.kt) — read before adding a Module,
  for the bundle-name coincidence rule.
- Bundle `language/en.json5` files under
  [`src/common/dev/martianzoo/tfm/canon`](../../src/common/dev/martianzoo/tfm/canon) own localized
  printed names; inspect only the bundle containing the renamed Class.

## Identity

Every class has one engine-facing `ClassName`. Pets, declarations, Kotlin implementations, events,
saved state, and engine APIs use it. A `ClassName` begins with an ASCII uppercase letter and then
contains ASCII letters, digits, or underscores. Pets keywords are case-sensitive and their exact
spellings are reserved as class names.

Structured content uses globally unique semantic English names such as `Birds`, `Landlord`, and
`Enceladus`. A definition's Class Name is its sole identity; there is no separate card, milestone,
award, colony, or standard-action identifier. Replacement relationships name the replaced Class
directly.

Two bundles may declare the same Class Name when the declarations are byte-identical, as
`HasRaisedTr` and `TrWatcher` do in `TerraformingMars` and `TurmoilCardPack`. This is deliberate:
each bundle stands alone, and identical declarations merge. Differing declarations under one name
are an error.

## Choosing a name

### Represent the published name; never invent one

We do not choose the names of published components, only how to spell them in Pets. Convert the
printed English title with the [Google Java Style camel-case conversion][camel-case]: remove
apostrophes, split on other punctuation, whitespace, and conventional internal camel-case word
divisions, lowercase each word, capitalize its first character, and join. `UNMI Contractor` becomes
`UnmiContractor`; `PolderTECH Dutch` becomes `PolderTechDutch`; `L1 Trade Terminal` becomes
`L1TradeTerminal`.

Do not expand what the card does not expand. Digits stay digits. Spell a number out only when the
printed title *begins* with one, since a `ClassName` cannot: `16 Psyche` becomes `SixteenPsyche`,
which is currently the only such case.

A placeholder implementation still uses the real name. Never ship a class whose name announces its
own incompleteness — an unimplemented card belongs in the `docs/what-is-supported.md` table plus, if
it is worth exercising, a test-only fixture.

### Supertype suffixes

Do not append a supertype's name to a subtype *mechanically*, as a substitute for the type system:
not `BankerMilestone` or `ClaimMilestoneSA`. The declaration already records the supertype, and an
initialism bolted on to every member of a category is usually noise.

Standard projects deliberately use the `SP` suffix consistently, including `PowerPlantSP`,
`AsteroidSP`, and `SellPatentsSP`. The game uses “standard project” as the name of this action
family, and the uniform suffix distinguishes its members from cards, resources, and ordinary game
concepts without deciding each collision differently.

We are not zealots about this. A category word that reads naturally in English earns its place, and
most of ours do: `GreeneryTile`, `BuildingTag`, `ActionPhase`, `ColonyTile`, and `ProjectCard` all
keep their nouns because the game says them out loud. `UseCardAction` is the pleasant edge case —
the trailing `Action` is simultaneously part of the printed phrase and the category, and it is
welcome for both reasons. The test is whether a fluent speaker of the game would say the whole
phrase, not whether the word happens to name a supertype.

### Qualifying a collision

When two things want one name, the bare name goes to **the one the rest of the game refers to**, and
the other takes the smallest meaningful qualifier.

The sharpest test is trigger position. A class that other declarations *trigger on* is a concept the
game talks about; a class that is only ever *invoked* is plumbing, and plumbing yields the name.
Grep the candidate in trigger position before deciding:

- `Trade` is triggered on by cards — `Trade: 3 MC` on Spire, `Trade<ColonyTile>::` on two Colonies
  cards. It is a real game concept that happens to share the published game's icon with the standard
  action. So the concept keeps `Trade` and the standard action becomes `TradeAction`, and we are
  improving on the printed game by distinguishing them at all.
- `PlayCard` names the reusable card-play operation invoked by standard turns, setup, and card
  effects. The standard action is specifically `PlayCardFromHand`.

Worked cases:

| Collision | Bare name goes to | Qualified |
| --- | --- | --- |
| Power Plant: card vs. standard project | card `PowerPlant` | standard project `PowerPlantSP` |
| Asteroid: card vs. resource vs. standard project | resource `Asteroid` | `AsteroidCard`, `AsteroidSP` |
| Trade: game concept vs. standard action | concept `Trade` | `TradeAction` |
| Play card: operation vs. standard action | operation `PlayCard` | `PlayCardFromHand` |
| Required action: component vs. its signal | component `RequiredAction` | `RequiredActionsSignal` |
| Reprinted goals | the newer, revised printing | the superseded one (see below) |

### Reprints and variants

When one printed goal exists with several rules, **one version gets the unqualified name and the
others carry a numeric qualifier.**

The bare name goes to the version we consider primary:

- Across reprints, the newer revision — the one the designers preferred. `Builder` (7 building tags,
  from `MilestonesAwardsExpansion`) over `Builder8` (the original Tharsis printing).
- Across rules variants of one goal, the standard version. `Producer` (16 total production) over
  `Producer22`, which is the goal under `QuickStartVariant`, where you begin with 6 production
  already on the board. Likewise `Generalist` over `Generalist2`, which asks for 2 of each
  production rather than 1.

For thresholds, the qualifier is always the number a player reads off the board, never an
engine-side count. Note that some requirement expressions carry a GrossHack offset that the name
must not inherit.

### Abbreviations

Spell a concept out when it stands alone; abbreviate it only where it is a *component* of a longer
name. `GlobalParameter`, `TerraformRating`, and `VictoryPoint` are classes; `GpComplete`,
`GpGameEndBarrier`, `Tr63SoloObjective`, `HasRaisedTr`, and `GetEventVps` are compounds. Do not mix
the two forms in one name.

Bundle-specific abbreviations are not allowed as prefixes: write `CimmeriaColonyBonus`, not
`TcColonyBonus`.

### Derived and card-local classes

The `{}` sugar generates a derived class named `<CardName>_<SupertypeName>`, as in
`NaturalPreserve_SpecialTile` and `SponsoredAcademies_Signal`. The underscore is the marker of a
structurally derived class and is intentional. A hand-written declaration that fills the same slot
uses the same spelling, as `LavaFlows_SpecialTile` does.

A hand-written helper that is *not* a structural derivative — a singleton observer, an extra action
host — does not take the underscore.

## Grammar, by kind of thing

Match the shape of the name to the kind of thing, so a reader can tell what a class is before
looking it up.

- **Persistent components** are noun phrases: `GreeneryTile`, `TradeFleet`, `ColonyProduction`,
  `TerraformRating`. Name the *unit* you actually instantiate, not the track it sits on.
- **Signals and Customs** — things that happen — are imperative verb phrases: `CreateMapAreas`,
  `AssignAwardPlaces`, `AdvanceColonyTracks`, `ResetColonyProduction`, `PassLeft`. Prefer the
  domain's verb over a programming one.
- **Capabilities** (supertypes that say what a component can do) read as predicates or agent nouns:
  `HasActions`, `ResourceHolder`, `TagHolder`. Reserve the `Has` prefix for this use.
- **Records** that something already happened use the passive voice when the actor does not matter
  (`SuitableInfrastructurePaid`, `ActionUsedMarker`) and the `My` prefix when it does: `My` marks
  that the *victim* is the owner while the actor rides along in a separate parameter, as in
  `MyResourceWasRemoved<Class<Resource>, Player>`.
- **Markers** name real physical components players handle: `ActionUsedMarker`, `LandClaimMarker`,
  `CapitalMarker`. `StartToken` keeps `Token` because the honest `StartPlayerMarker` is long and
  `StartMarker` reads wrong.
- **Card locations** use noun phrases for places (`Hand`, `EventPile`) and participles for
  explicitly transient states (`Selecting`, `Revealed`, and eventually `Drafting`).
- **Singular vs. plural** may distinguish one-of from all-of over the same subject —
  `GiveColonyBonus` (one colony) against `GiveColonyBonuses` (every colony the player owns) — and
  may distinguish a whole operation from its per-item step, as `BuySelectedCards` does over
  `BuyCard`. It may **not** distinguish two different *kinds* of thing; give those unrelated names.
- **Do not use implementation or game-design vocabulary** as a component name. "Mechanic", "hack",
  "fake", and Pets grammar terms such as "effect" describe how we built something, not what it is in
  the game.

## Modules

Most `Module` subtypes extend `Module` directly, and that is fine — they need no intermediate
supertype just to justify a suffix. Three loose families exist today:

1. **Content and card packs** — published products contributing cards and components use their own
   noun: `CorporateEraExpansion`, `ColoniesExpansion`, `VenusNextExpansion`, `PreludeExpansion`,
   `Prelude2Expansion`. `CardPack` marks a card-only selection that can be included independently
   from its product's rules: `Prelude1CardPack`, `Prelude2CardPack`, `PromoCardPack`, and
   `TurmoilCardPack`. The published expansions and their Bundles retain the official
   `PreludeExpansion` and `Prelude2Expansion` names; the card packs use `Prelude1` and `Prelude2`
   to distinguish their contributions to the merged Prelude deck.
2. **Exclusive choices** — a closed set behind an abstract supertype, exactly one selected. These
   already borrow the supertype's word, which reads well: `MultiplayerMode` and `SoloMode` under
   `GameMode`; `TharsisMap` and `HellasMap` under `MarsMap`; `StandardSoloObjective` and
   `Tr63SoloObjective` under `SoloObjective`.
3. **Independent toggles** — optional rules switched on or off on their own:
   `QuickStartVariant`, `WorldGovernmentRule`, `MandatoryVenusVariant`.

The third family currently uses two words for one kind. **A convention for choosing that suffix is
deferred**; nothing here is a violation until we settle one, and no new abstract supertype is wanted
just to supply the word.

A Module whose Class Name equals its bundle name automatically claims that bundle's ordinary cards
and colony tiles. Any other Module needs an explicit `moduleContentSelections` entry. This
coincidence is load-bearing, not decorative — check [`Bundle.kt`](../../src/common/dev/martianzoo/tfm/canon/Bundle.kt)
before renaming a Module or adding one to an existing bundle. A bundle whose content is claimed by
map Modules or explicit selections has no self-named Module at all, which is fine.

## Display names and localization

Each Game World owns a locale-specific `Vocabulary`:

- `canonicalName` and `canonicalize` resolve localized Pets input and input-only synonyms;
- `displayName` produces plain UI text; and
- `petsName` and `renderPets` produce localized, parseable Pets.

Bundle files at `language/<tag>.json5` map Class Names to display names. Lookup falls back from the
requested locale to less-specific locales and then English, independently for each entry. Keep
entries grouped by content category and sorted by Class Name within each group; milestones precede
awards and cards are last.

**English display text defaults to `defaultEnglishDisplayName`**, which separates words at
lower-to-upper transitions, at digit boundaries, and at underscores. English language files contain
**only exceptions to that default** — an entry whose value equals the default is noise and must be
deleted.

Printed card titles are typeset in all caps and therefore carry no case information. English display
text is Title Case with **every** word capitalized, including articles and prepositions and
including the word after a hyphen: `Import Of Advanced GHG`, `Board Of Directors`,
`Anti-Desertification Techniques`.

Other locales derive a Pets name from the effective localized display name with the same
[camel-case conversion][camel-case] used for printed titles. The current implementation accepts
ASCII display text only. A localized name that collides with another canonical Class Name falls back
to the canonical name.

There are no per-entry Pets-name overrides. Input-only synonyms never become rendering candidates.
Vocabulary construction rejects collisions among Class Names, localized Pets names, and synonyms.
Display text is presentation, not identity. UI code must therefore render through the session
Vocabulary instead of `ClassName.toString()`.

There is no Unicode normalization because non-ASCII display text is currently rejected.

## Pending naming work

### Second action signal

`SecondAction`, the Signal for the second action slot of an action-phase turn, collides in the
reader's head with `Action2`, but is expected to go away entirely; do not rename it in the meantime.

### Independent-toggle Modules

The convention that chooses `Option` or `Variant` for an independent-toggle Module remains
undecided.

### Known and accepted

`SoloGenerationsLeft` deliberately names the counted collection: a solo game begins with fourteen
and removes one whenever a Generation begins. The plural reads naturally at its principal uses and
is clearer than treating each copy as a separately named token.

`Barrier` and `GameEndBarrier` are **unrelated supertypes** that both use the word. `Barrier :
MustCleanUp` means "the player must remove this to unblock a task" and backs the open-ended query
`MAX 0 Barrier` in
[`classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets), which spans
`Owed`, `Billing`, `Required`, and `TradeBarrier`. `GameEndBarrier` extends nothing, means "the game
may not end yet", and is queried by name from
[`TfmWorkflow.kt`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt) and four tests. We are
keeping the shared word. The trap to watch: a new class that blocks game end will compile just as
happily under `Barrier`, and would then silently join the payment query — check which supertype you
mean.

`TerraformRating` names the track while instantiating one step of it. `TerraformRatingStep` is more
correct; we are not doing it.

[camel-case]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case
