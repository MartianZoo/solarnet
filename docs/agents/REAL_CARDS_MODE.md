# Shuffle-and-deal real-card mode

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** designing or implementing physical cards, deck/discard derivation, shuffle/deal,
> reveal/search, hidden information, dealer narrowing, or a real-card observation interface.
>
> **Skip when:** changing committed follow-mode behavior without making authored hidden procedures
> executable. Read only “Canonical card-operation source” when changing `CARDS[...]` transforms.
>
> **Status:** proposal with a settled card/state shape and layer ownership. Follow mode is committed
> and remains the default; type syntax, the default dealer algorithm, and observation interface
> remain unproved.

## Read only the relevant gate

| Task | Read |
| --- | --- |
| Component/location representation | State model through Ordinary transitions |
| Defaults, counted cards, or delegated face choice | Defaults and atomization; Selection-time delegation |
| Shuffle, replay, rollback, or forks | Deterministic dealer projection |
| Reveal, search, or card predicates | Reveals, searches, and printed predicates |
| `CARDS[...]` or follow-mode lowering | Canonical card-operation source; Follow mode and operation lowering |
| Conservation or hidden observations | Conservation; Information hiding is deferred |
| Begin implementation | Implementation gates; Acceptance properties; Remaining decisions |

## Source map

- [`CardOperation.kt`](../../src/common/dev/martianzoo/tfm/canon/CardOperation.kt)
  — inspect the canonical hidden-procedure representation.
- [`FollowModeNeutralizer.kt`](../../src/common/dev/martianzoo/tfm/canon/FollowModeNeutralizer.kt)
  — search for `neutralize` before changing current executable lowering.
- [`ClassDeclaration.kt`](../../src/common/dev/martianzoo/pets/data/ClassDeclaration.kt)
  — search for `executableEffects`, the shadow effects list this mode installs.
- [Promo `cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets)
  — search for `CARDS[` to sample canonical authored operations.
- [`CardClassTest.kt`](../../test/common/dev/martianzoo/tfm/canon/CardClassTest.kt)
  — read for the loaded card-Class queries and validation.

## Settled card and state direction

Real-card mode lets Solarnet shuffle, deal, reveal, draft, play, and discard exact physical cards.
The smallest coherent model discovered so far is:

1. A card exists as a Component only while it is associated with a Player.
2. Every such card is directly `Owned` by that Player. A card back also depends on one unowned
   singleton card-location Component; a card front needs no location because its existence already
   means it is in play.
3. Deck and discard are not Components. The default Admin dealer policy derives them from the
   selected card set, an immutable seed, and exact card-transition history.
4. A card back carries its represented `Class<CardFront>`; a card front carries its
   `Class<CardBack>` family.
5. Counted physical-card instructions atomize before defaults and ownership specialization.
6. A Player controls when a card gain is selected, but Admin alone narrows the remaining exact-face
   choice. Selection-time resolution delegates that narrowing and blocks the controlling scope until
   it completes.
7. Information hiding will eventually project exact Types to less concrete Types. It does not
   require unknown-card Components in the master World.

Deck, discard, and card locations have deliberately different roles: the first two are derived
dealer state, while locations are plain unowned Components. Ownership never propagates vicariously
through a location dependency.

## State model

The authoritative game still has one Event Log. The default dealer policy adds no mutable deck
list, discard list, RNG cursor, or second card database beside it.

The premise fixes:

- the selected set of faces for each deck family;
- one root seed;
- a canonical face order;
- the shuffle and seed-derivation algorithm version; and
- the real-card Module selection.

The Event Log records exact in-world card gains, movements, plays, and removals. The default policy
derives deck order, discard membership, reshuffle epoch, and next position by folding those facts. A
cache may retain the fold at an event cursor, but deleting that cache and replaying must reproduce
the same result.

The Event Log is already durable game state. A derived dealer projection is policy state that can
be reconstructed, not a second game authority.

## In-world type model

Exact syntax is provisional, but the dependency shape is settled:

```pets
ABSTRACT CLASS CardLocation { HAS =1 This }

CLASS Hand : CardLocation
CLASS EventPile : CardLocation
CLASS Selecting : CardLocation
CLASS Revealed : CardLocation

ABSTRACT CLASS Card : Owned<Owner>
ABSTRACT CLASS CardBack<CardLocation, Class<CardFront>> : Card
ABSTRACT CLASS CardFront<Class<CardBack>> : Card, TagHolder

ABSTRACT CLASS ProjectFront : CardFront<Class<ProjectCard>>
CLASS ProjectCard : CardBack<Class<ProjectFront>>, Atomized

ABSTRACT CLASS PreludeFront : CardFront<Class<PreludeCard>>
CLASS PreludeCard : CardBack<Class<PreludeFront>>, Atomized

ABSTRACT CLASS CorporationFront : CardFront<Class<CorporationCard>>
CLASS CorporationCard : CardBack<Class<CorporationFront>>, Atomized
```

The rendered argument order may differ after dependency inheritance is proved. Semantically, an
exact card includes:

```text
card family + Player owner + represented opposite-face Class + card location when it is a back
```

For example:

```text
ProjectCard<Player1, Hand, Class<Decomposers>>
Decomposers<Player1, Class<ProjectCard>>
ProjectCard<Player1, EventPile, Class<Decomposers>>
```

The two Class literals carry different facts:

- `Decomposers -> ProjectCard` is immutable family metadata on the front Class.
- one exact `ProjectCard -> Decomposers` is the identity of the current card Component.

The family bounds prevent a corporation back from representing a project front. They also let play
and event cleanup preserve physical identity without a separate deck-family check.

Exactly one representation of a face exists in the World at a time. Playing transmutates its back
into its exact front; it does not retain a parallel back Component underneath the live card.

## Card locations

Locations are singletons and are never owned. The card's direct `Owner` dependency partitions each
location by Player. Only card backs have a location; the presence of a card front in the World is
the single representation of that card being in play.

| Location | Representation | Meaning |
| --- | --- | --- |
| `Hand` | back | acquired card available to its Player |
| `EventPile` | back | completed Event retained for scoring or recovery |
| `Selecting` | back | temporary Player-associated selection pool |
| `Revealed` | back | exact face exposed by a reveal operation |

Direct ownership is intentionally present even in temporary locations. It identifies whose choice
or reveal operation the card belongs to and supplies the usual contextual `Owner`, task routing,
defaults, and queries.

Ownership does not determine visibility. `Revealed` may be public, while another Player's `Hand`
cards are private.

Deck and discard are absent from this table because they are not Components or Pets Types.

## Normal transitions

Once an exact card is in the World, location and face changes remain ordinary atomic
transmutations:

| Operation | State change |
| --- | --- |
| Move into a selection pool | exact `Selecting FROM Hand` |
| Keep a revealed card | exact `Hand FROM Revealed` |
| Play | exact front matching back at `Hand` |
| Finish Event | matching back at `EventPile FROM` exact front |
| Recover Event | exact `Hand FROM EventPile` |

Playing Decomposers is conceptually:

```text
Decomposers<Player1, Class<ProjectCard>>
  FROM ProjectCard<Player1, Hand, Class<Decomposers>>
```

Finishing an Event reverses the representation and changes its location:

```text
ProjectCard<Player1, EventPile, Class<SearchForLife>>
  FROM SearchForLife<Player1, Class<ProjectCard>>
```

The represented face and back family must link across both sides of each atomic instruction. Never
model one movement as a gain followed by a removal; the temporary duplicate or absence would be
observable.

## Derived-deck transitions

A pure exact card gain enters the World from the derived deck. A pure exact card-back removal leaves
the World for the derived discard.

| Operation | World event | Derived projection consequence |
| --- | --- | --- |
| Draw | exact gain at `Hand` | consume next face from deck |
| Reveal | exact gain at `Revealed` | consume next face from deck |
| Offer cards | exact gain at `Selecting` | consume next face from deck |
| Discard from a location | exact pure removal | add that face to discard |

Nothing ever moves directly from deck to discard. Even a rejected card from Search for Life first
exists at `Revealed`; its later pure removal records the separate discard transition.

Playing is not a discard because it is an in-World back-to-front transmutation. Event cleanup is not
a discard because it produces an `EventPile` back. These distinct event shapes let the derived fold
identify reservoir transitions without guessing from a generic removal.

If a future rule permanently removes a card from the game, add an explicit semantic transition for
that rule. Do not reinterpret discard.

## Defaults and atomization

`CardBack` has a gain default selecting `Hand`; its removal default likewise selects `Hand` for the
familiar discard shorthand. Contextual ownership supplies the Player.

Current preprocessing atomizes before inserting dependencies and replacing contextual `Owner`:

```text
2 ProjectCard
  -> ProjectCard, ProjectCard
  -> ProjectCard<Hand>, ProjectCard<Hand>
  -> ProjectCard<Player1, Hand>, ProjectCard<Player1, Hand>
```

Each resulting instruction is still abstract over its represented `Class<ProjectFront>`. That is
essential: the authored instruction promises two cards but does not choose either face.

Atomization follows from physical exactness. Two different faces are two different concrete Types,
so one counted concrete change cannot honestly select them together. This is the same reason a
counted OceanTile placement must split before choosing different areas.

The familiar forms remain meaningful:

```text
ProjectCard<Player1>     // default location Hand; exact face still unresolved
-ProjectCard<Player1>    // choose an exact Hand card, then discard it
```

Positive entry gains receive Admin narrowing. Hand removals remain Player choices because the
exact candidate cards already exist in that Player's World-visible domain.

## Selection-time delegation

An abstract card gain contains two decisions owned by different parties:

- the controlling Player decides when the promised draw or reveal proceeds; and
- Admin determines which exact face is supplied. The normal Admin policy consults the deterministic
  dealer projection.

The abstract task must initially remain under its controller. The Player may select other eligible
sibling work first. When the Player selects the card gain:

1. resolution recognizes that its remaining face variable is Admin-narrowed;
2. that same selected task moves to Admin while retaining its controller and future Actor;
3. the controlling scope is blocked from further task execution;
4. Admin's Agent policy chooses an exact face and its Agent issues the narrowing; and
5. completing the task releases the block and returns resulting work to its controller.

The Player must never be able to submit a preferred exact face. `BY Admin` is not this mechanism:
instruction-side `BY` changes event attribution, not narrowing authority or queue control.

Core engine enforces that only Admin narrows this task and that the submitted face is a currently
legal narrowing. It does not require that face to be next in the default policy's shuffle. The
named dealer policy owns that stronger promise; another permitted Admin policy may choose
differently. Replay records the chosen exact face either way.

Counted gains delegate one atom at a time. The first exact gain enters the Event Log before the next
atom is selected, so the second atom necessarily derives the following face.

### Philares is the controlling precedent

Philares establishes the current controller/delegation timing semantics:

1. the active Player retains control of the pending resource task and decides when to select it;
2. resolution delegates only the Standard Resource narrowing to the Philares owner;
3. the active Player can do no more work in that control scope while the delegated task is
   unresolved; and
4. after the Philares owner chooses and receives the resource, resulting work returns to the active
   Player.

Current code implements this precedent with the general delegation mechanism specified in
[IDENTITY.md](IDENTITY.md). `TaskDelegationTest` proves the mechanism directly, while
`PhilaresTest` proves controller ordering, owner-only narrowing, and blocking through Player-level
gameplay.

## Default deterministic dealer policy

For each deck family and epoch, the default policy derives a permutation from:

```text
shuffle(
  canonical eligible face set,
  deriveSeed(root seed, deck family, reshuffle epoch),
  algorithm version
)
```

The project, prelude, and corporation families use independently derived streams so activity in one
does not perturb another.

The initial selected supply is a set in this game: one occurrence per selected face. There is no
physical-copy identity and no multiplicity problem to solve. A future game variant with genuinely
distinguishable or repeated physical copies would require a separate extension.

The fold maintains only derived values:

- current epoch;
- current shuffled face order;
- position within that order; and
- current discard set.

It advances from exact events:

- an exact entry gain consumes the next expected face;
- an exact pure card-back removal adds that face to discard;
- in-World location and front/back transmutations leave the dealer projection unchanged.

When the current order is exhausted, the next entry request deterministically shuffles the exact
discard set with the next epoch seed, clears derived discard, and consumes the first resulting face.
The transition point and participating set are recoverable from prior events, so neither a stored second seed
nor a mutable reshuffle record is required. An explicit diagnostic event may be useful, but cannot
become a second authority.

If both derived deck and discard are empty, the entry gain is unavailable. Reshuffling is an
explicit dealer rule, not a selector fallback that invents a candidate.

### Replay, rollback, and forks

- Equal premises and equal histories derive equal next cards on JVM and JavaScript.
- Rolling back an exact gain removes its event, so retry derives the same face.
- A fork sharing the same prefix shares the same next face and diverges only after its history does.
- Exact recorded gains remain authoritative historical outcomes.
- The premise's algorithm version lets old histories be validated after implementation changes.
- A cache is indexed by history identity or event cursor and never advances independently.

The default policy computes a candidate without mutating game or policy state. Executing the exact
gain appends the event from which its next projection is derived. This avoids an off-timeline RNG
cursor and keeps failed resolution observational.

## Reveals, searches, and printed predicates

A normal draw enters `Hand`. A reveal enters `Revealed`. Search policies consume the deck in
order and must not filter the shuffled set first, because doing so would skip and fail to record
rejected cards.

“Reveal until three matching cards” quantifies successful matches, not cards inspected. Do not add
a quantifier meaning “as few as necessary to avoid a dead end”; that would introduce downstream
lookahead and let constraint solving inspect future deck order.

The compositional operation is three sequential one-hit searches. Conceptually:

```text
repeat until one hit:
  gain the next exact card at Revealed
  if its printed facts match:
    move it Revealed -> Hand
    finish this hit
  otherwise:
    remove it from Revealed into derived discard

perform that one-hit operation three times
```

The canonical authoring form for this family is a filtered card gain inside `CARDS`, parameterized
by deck family and printed predicate, not a Kotlin implementation per printed card. Its real-mode
lowering still has to make deck exhaustion explicit: fail, accept fewer, or reshuffle according to
the actual rule.

A back has no live tag Components. Printed predicates inspect immutable front-Class metadata, for
example through a property or represented-Class refinement:

```text
ProjectCard<
  Class<ProjectFront>(HAS PrintedTag<Class<BuildingTag>>)
>
```

Do not make plain `HAS` silently traverse every represented Class, and do not create live tag
Components for card backs.

## Canonical card-operation source

Canonical sources now preserve hidden card procedures in one
`Instruction.Transform`, `CARDS[...]`. The inner instruction tree carries the operation family:

```pets
CARDS[2 ProjectCard(HAS VenusTag)]
CARDS[7 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>]
CARDS[3 PreludeCard<Selecting>, PreludeCard<Hand FROM Selecting>, PlayCard<Class<PreludeCard>>]
CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS SpaceTag): Asteroid<This>) OR Ok)]
CARDS[2 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>(HAS VenusTag). THEN -2 ProjectCard<Selecting>? THEN BuySelectedCards]
CARDS[2 ProjectCard<Hand FROM EventPile>?]
CARDS[2 / ProjectCard<Hand>]
CARDS[CardBack<EventPile>]
CARDS[CardBack<EventPile, Class<This>> FROM This]
CARDS[4 ProjectCard<Selecting>, -4 ProjectCard<Selecting>? THEN BuySelectedCards]
```

A filtered plain gain means sequentially search for the requested matches. Its predicate is source
shorthand over the represented front's immutable printed metadata. It does not change plain
`HAS`, imply that a back owns a live tag, or prefilter the derived deck. Real-mode lowering must
reveal every inspected card in order and discard nonmatches.

A card procedure's follow-mode compilation creates the Player's temporary `Selecting` location
before the procedure body, and a `Hand FROM Selecting` instruction retains exact cards. Removing
that location discards every card still dependent on it through the engine's dependency cascade.
`Revealed` follows the same lifecycle. A purchase procedure first removes unwanted cards and then
invokes one unquantified `BuySelectedCards`. That signal counts every card remaining in the Player's
selection, creates the complete base debt, broadcasts the same multiplicity of `BuyCard` so
Polyphemos and Terralabs Research can adjust that established `Owed`, and then creates one invoice.
Once the invoice is fully paid, the purchase operation moves those exact selected cards to `Hand`.
Its optional removal count is the offered count, so the player may discard any subset before buying
the remainder; corporation setup uses ten, Research uses four, Venus Orbital Survey uses whatever
non-Venus cards remain from two, and single-card purchase actions use one.

Area-qualified card observations use the same transform. `ProjectCard<Hand>` counts only project
cards in the Player's hand, while `CardBack<EventPile>` counts completed Events in that Player's
event pile. Public Plans moves one linked quantity from `Hand` to `Revealed`, returns those exact
cards to `Hand`, and awards that quantity.

Follow mode has an intermediate model named `CardLocation`. A generic `CardBack`
depends on one of `Hand`, `EventPile`, `Selecting`, or `Revealed`, but does not
depend on the represented `Class<CardFront>`. Bare card references default to `Hand`.

`CARDS` retains those locations and movements through the Catalog's shared marked-syntax handler.
`Selecting` and `Revealed` are Player-owned temporary components. The handler sequences creation
before the procedure body and cleanup after it; purchase closure instead belongs to
`BuySelectedCards`. Removing a temporary location intrinsically removes cards still dependent on
it. Printed-face predicates are still delegated to
the follow-mode client: they are erased from generic backs, and a filtered retention becomes an
explicit optional movement so the client can report how many matching cards moved. A client may
ignore identities entirely or, as `CardTrackingFullGameTest` does, supply names precisely when cards
enter and leave `Hand`.

`BuySelectedCards` prices the cards remaining in `Selecting`, waits for the adjusted invoice to be
paid, and then moves that count to `Hand`. Public Plans performs an explicit `Hand` to `Revealed` to
`Hand` round trip. Exact Event movements lower through `PlayedEvent` because `CardBack` does not
carry its represented front.

The current source-level operation inventory is:

| Family | Cards |
| --- | --- |
| Search by printed facts | Sagitta Frontier Services, Atmospheric Enhancers, Nobel Prize, Planetary Alliance, Soil Bacteria, Venus Contract, Ishtar Expedition, Stratospheric Expedition, Experimental Forest, Acquired Space Agency, Splice, Factorum, Pharmacy Union, Aqueduct Systems, Celestic, Morning Star Inc. |
| Inspect N, keep K | Business Contacts, Invention Contest, Corporate Archives, Hi-Tech Lab, Tycho Magnetics, Spire |
| Inspect N, select and play one | Valley Trust, Merger, New Partner |
| Choose cards to buy from an offer | Corporation setup, Research phase, Inventors' Guild, Business Network |
| Reveal and test | Search for Life, Asteroid Deflection System |
| Reveal two, retain matches, buy or discard the rest | Venus Orbital Survey |
| Recover Events | Astra Mechanica |
| Observe cards in hand | Head Start, Planner, Visionary |
| Observe completed Events | Media Archives, Legend, Promoter |
| Finish or transfer Events | Event cards, Pharmacy Union, Law Suit |
| Reveal chosen hand cards temporarily | Public Plans |

The remaining card gains and removals still use the follow-mode shorthand directly; they
do not preserve deck or hand-location procedure yet.

## Conservation

For each selected face, exactly one of these positions exists:

```text
derived deck
OR derived discard
OR one exact in-World CardBack
OR one exact in-World CardFront
```

Dealer replay plus the current World can validate this partition. The generic component Limiter
does not need an invariant spanning hidden Deck and Discard Components because those Components no
longer exist.

Every normal transition preserves the face and back family. Entry and discard move into or out of the
World but remain exact logged events. Setup proves that every selected face begins in the derived
deck exactly once.

## Information hiding is deferred, not contradicted

Deck secrecy is immediate because deck faces are absent from the component World. In-world hidden
cards still require a Player-relative projection.

The master World stores an exact Type:

```text
ProjectCard<Player1, Hand, Class<Decomposers>>
```

A viewer who may know only that Player1 has one project card can receive a broader observation:

```text
ProjectCard<Player1, Hand, Class<ProjectFront>>
```

This is normal loss of Type concreteness, not an `UnknownCard` object. The same projection must
eventually cover counts, refinements, tasks, results, and event history; hiding only component-list
output would leak identities.

Exact visibility policy, irreversible publication, and rollback knowledge remain later gates. For
now:

- the normal Admin dealer policy uses the exact master history;
- a Player sees exact own-Hand and own-Selecting faces when the rules permit;
- `Revealed`, card fronts, and `EventPile` are normally public; and
- no observation API exposes future derived deck order.

Do not add `KnownTo` Components, fake playable unknown fronts, or ownership-based visibility rules.

## Follow mode and operation lowering

Real-card mode is an affirmative Module fixed in the premise and mutually exclusive with follow
mode. Both modes are permanent supported behavior; follow mode remains the default and delegates
card outcomes to a client.

Develop real-card lowering by operation family while preserving the corresponding follow lowering:

1. draws and deals;
2. reveal one and optionally keep or buy;
3. reveal several and choose;
4. sequential search by printed facts;
5. drafting and packet passing;
6. play and Event cleanup; and
7. recover a known Event.

Shared definitions with no transition difference need one form. Do not mechanically duplicate or
rename every card definition.

### Retire `ClassDeclaration.executableEffects`

**Working direction:** find a way to lower follow mode without a second stored copy of a class's
effects.

`ClassDeclaration` carries a nullable `executableEffects` beside `authoredEffects`, so a class can
hold two representations of the same behavior. Its only writer anywhere is
`FollowModeNeutralizer.neutralize`; `DerivedClassLowerer` then has to thread it through, and
`ClassDeclaration.effects` silently prefers it. A mode-specific shadow field on the language's core
declaration type is the wrong home for what is really one Catalog's lowering choice.

This is worth solving now rather than after real mode lands, because real mode adds a *second*
lowering of the same authored `CARDS[...]` sources. Two shadow fields, or one field whose meaning
depends on the selected Module, would be worse than today.

Directions to try, cheapest first:

1. **Lower at Catalog load, keep one field.** If neutralization runs while the Catalog is being
   built, the loaded declaration can simply *be* the executable one and `authoredEffects` stays the
   only stored effects list. Check what still needs the authored form: today it is `renderChange`
   (`authoredEffectsWithActions`) and `TfmCatalog`'s action check. If those callers can read the
   authored form from the parsed source or from a rendering-only side table, the field disappears.
2. **Make the mode a projection, not a rewrite.** Follow mode is already a Module selection. If the
   `CARDS[...]` handler is chosen per premise and applied during class loading, both modes read one
   authored declaration and neither stores a rewritten copy.
3. **Give the operation a component.** If the card-operation families in “Canonical card-operation
   source” become declared classes rather than a marker plus a recognizer, most of what
   `FollowModeNeutralizer` rewrites becomes ordinary mode-specific effects on mode-specific classes,
   and there is nothing left to shadow.

Any of these is acceptable. Storing both forms is not, once there is a third consumer.

## Rejected designs

- Deck or Discard as CardLocation Components;
- owned card-location Components or vicarious ownership through a dependency;
- a mutable Kotlin deck, discard list, or RNG cursor as independent authority;
- Player narrowing of an exact face supplied by chance;
- immediate Admin auto-selection that bypasses controller timing and delegation;
- `BY Admin` as a substitute for changing the narrower;
- direct deck-to-discard movement;
- filtering the shuffled set before reveal;
- a minimum-lookahead instruction quantifier;
- gain-then-remove movement;
- mirrored hand counters;
- per-card search implementations;
- live printed tags on backs;
- a universal represented-Class `HAS` rule;
- default physical-copy identity; or
- a Chance Actor.

## Implementation gates

1. **Types and defaults:** prove the mutual Class-literal dependencies, singleton locations, direct
   ownership, gain/removal defaults, atomization order, and linked play/Event transitions in a
   synthetic Class Table.
2. **Admin narrowing:** extend the current selected-task delegation model so real-card resolution
   names Admin as the narrower; prove with a synthetic card face that controller timing and the
   task's future Actor remain unchanged.
3. **Dealer projection:** derive a tiny three-face deck and discard set from premise plus events;
   prove independent family streams, exhaustion, reshuffle epochs, cache deletion, rollback, replay,
   and forks on JVM and JavaScript.
4. **Card lifecycle:** move exact cards through every location, front, and back state and prove the
   conservation partition.
5. **Operation families:** migrate one draw, reveal, choice, search, draft, play/Event, and recovery
   rule without card-specific engine branches.
6. **Observation:** project every component, query, task, result, and history path before exposing
   real mode to clients.

Keep follow mode green and default throughout. Stop if the type proof or delegation requires global
exceptions specific to ProjectCard.

## Acceptance properties

Properties about shuffled order and seed reproducibility below are promises of the named default
dealer policy. Core engine acceptance covers task assignment, legal narrowing, execution, and
recorded outcomes without judging that policy's strategy.

**Target criteria, not current guarantees:**

1. The premise-selected face set partitions exactly across derived deck, derived discard, backs,
   and fronts.
2. `N ProjectCard` creates N atomized gains and consumes N sequential faces without replacement.
3. The controlling Player decides when each gain is selected but cannot narrow its face.
4. Resolution delegates face narrowing to Admin and blocks the controller until completion.
5. Equal seed, algorithm version, deck family, and history produce equal outcomes across platforms.
6. Rollback and retry reproduce an outcome; forks share outcomes until their histories diverge.
7. Reshuffle uses exactly the discard set derived at the exhaustion point.
8. Every search candidate enters `Revealed` before it is kept or discarded.
9. Printed predicates do not create or query live tags on inactive cards.
10. Playing and Event cleanup preserve exact face and back family.
11. Hand discard is the owning Player's exact-card choice and updates derived discard once.
12. No Player-facing path exposes future deck order or another Player's hidden exact faces.

## Remaining decisions

- final dependency and rendered-argument order;
- final real-mode lowering of `CARDS`;
- exact shuffle, seed derivation, and canonical ordering algorithms;
- whether a derived reshuffle deserves an explicit diagnostic event;
- how `Selecting` scopes overlapping selections;
- the precise visibility matrix and irreversible knowledge limit; and
- whether any supported future variant truly needs repeated or distinguishable copies.

None changes the central model: directly owned in-World cards, unowned singleton locations, no Deck
or Discard Components, deterministic dealer state derived from premise plus history, and
selection-time delegation of exact-face narrowing to Admin.
