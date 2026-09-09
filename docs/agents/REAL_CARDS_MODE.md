# Card handling today and real-card mode

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing card locations, `CARDS[...]`, `SearchForCard`, follow-mode lowering, or
> the proposed shuffle-and-deal mode.
>
> **Skip when:** changing ordinary behavior of a known card face. Read the card declaration and its
> tests instead.
>
> **Status:** the first half describes current follow mode. The second half is the clearest plan
> found for a future real-card mode; its state shape and ownership choices are settled direction,
> while syntax, shuffle details, and observation APIs remain proposals. No real-card mode exists.

## Orientation

Solarnet currently follows a physical or external game. It knows how many generic card backs a
Player has and where those backs are, but it does not own a shuffled deck or retain the face of each
unplayed card. The client supplies face-dependent outcomes.

Real-card mode would make Solarnet the dealer. Each in-World card would retain its exact face, while
deck and discard would be deterministic views derived from the premise and Event Log. The engine
would continue to execute ordinary Pets; an Admin policy would supply chance-selected faces, and a
viewer-specific projection would hide faces a viewer may not know.

These are two modes over the same card operations, not two card systems.

## Source map

- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — search for `ABSTRACT CLASS CardLocation`, `ABSTRACT CLASS Card`, `CLASS PlayedEvent`, and
  `CLASS BuySelectedCards` for the current state model.
- [`CardOperation.kt`](../../src/common/dev/martianzoo/tfm/canon/CardOperation.kt) — the validated
  semantic forms accepted inside `CARDS[...]`.
- [`FollowModeNeutralizer.kt`](../../src/common/dev/martianzoo/tfm/canon/FollowModeNeutralizer.kt) —
  the current follow-mode lowering.
- [`ClassDeclaration.kt`](../../src/common/dev/martianzoo/pets/data/ClassDeclaration.kt) — search
  for `executableEffects` for the temporary authored/executable duplication.
- [`CatalogTest.kt`](../../test/common/dev/martianzoo/tfm/canon/CatalogTest.kt) — the focused proofs
  of current `CARDS[...]` lowering.
- [`InspectAndKeepCardsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/InspectAndKeepCardsTest.kt)
  and [`SearchForLifeTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/SearchForLifeTest.kt)
  — representative location behavior.
- [`CardTrackingFullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt)
  — a follow-mode client that tracks exact names outside the World.
- [IDENTITY.md](IDENTITY.md) — current controller, assignee, Actor, and delegation semantics that a
  future dealer must reuse.

## How cards work today

### Follow mode owns counts, not hidden identities

The implemented model has three forms:

- `CardBack<CardLocation>` is an unplayed generic card directly owned by a Player.
- a concrete `CardFront<Class<CardBack>>` is a known card in play.
- `PlayedEvent<Class<CardFront>>` records the exact face of a completed Event.

A back does not retain the Class of its represented front. Several project-card backs in one
Player's hand are therefore fungible components of the same Type. Playing a card consumes a generic
back and creates the concrete front named by the client-facing operation. Completed Events retain
their exact face because scoring, recovery, and other published rules query that history.

The World contains no deck, discard pile, shuffle state, physical-copy identity, or hidden face.
Those facts belong to the physical game or client being followed. A client may ignore identities,
or it may correlate names with generic changes as `CardTrackingFullGameTest` does; that tracking is
not authoritative World state.

### Locations are ordinary Pets state

`Hand`, `Selecting`, and `Revealed` are permanent, ownerless `CardLocation` Components. Cards remain
directly owned by their Player; location does not confer ownership.

`Hand` is the default for bare gains and removals. `Selecting` represents a temporary offer or
inspection pool, and `Revealed` represents a temporarily exposed card. Moving a known quantity
between locations is an ordinary transmutation, so a retained card is never modeled as an unrelated
removal and gain.

Entering `Selecting` or `Revealed` creates at most one `CardLocationCleanup`. At World idle, that
temporary cleanup removes and recreates the location; dependency cleanup discards any generic backs
left there. This lifecycle is authored in Pets rather than implemented by the card-operation
lowerer.

Common procedures compose from this state:

- inspect-and-keep gains generic backs at `Selecting`, moves the retained count to `Hand`, and lets
  cleanup remove the rest;
- reveal-and-test gains a back at `Revealed`, offers the reported outcome, and lets cleanup remove
  the back;
- buying offered cards prices the backs remaining at `Selecting`, settles one invoice, and then
  moves that count to `Hand`; and
- Event completion and recovery use exact `PlayedEvent` transmutations with no mode-specific
  handling.

The engine proves these procedures over counts and locations. The follow-mode client is responsible
for reporting which physical faces matched a printed predicate or were selected from an offer.

### Canonical card-operation source

`CARDS[...]` marks only authored operations whose meaning depends on an unplayed card's face. It
preserves that intent in source even though follow mode cannot execute the face-dependent part.
Ordinary card procedures remain ordinary Pets and should not be wrapped for hypothetical future
use.

`CardOperation` currently recognizes four semantic families:

1. choose a known card Class using printed metadata;
2. search sequentially for matching cards;
3. reveal a card and test its printed facts; and
4. reveal cards, retain matching ones, then buy or discard the others.

Catalog construction validates each marked form and `FollowModeNeutralizer` removes only the part
that requires hidden identity:

- a filtered search becomes the same quantified generic gain as an ordinary draw;
- a refinement selecting a represented front Class is erased;
- a reveal-and-test outcome becomes optional; and
- filtered retention becomes an optional movement whose count the client reports.

The marker does not change plain `HAS`, invent live tags on card backs, manage locations, or own
cleanup. Completed-Event operations are already exact through `PlayedEvent` and never need it.

### Current design debt: one declaration stores two effect forms

`FollowModeNeutralizer` stores lowered effects in `ClassDeclaration.executableEffects` while
retaining `authoredEffects`. This makes a core Pets declaration carry two representations of one
behavior solely for a Terraforming Mars catalog choice. `DerivedClassLowerer` must preserve the
shadow field, and readers must know whether they need authored or executable effects.

Remove this duplication for today's follow mode, independently of real-card work. The smallest
promising direction is to lower at Catalog construction while keeping one executable declaration,
with any source-rendering need reading parsed source or a rendering-only record. A future mode must
not add another effect field; it should select an operation lowering while building the game.

## Clearest future plan

### Exact cards extend the current Type model

An in-World card should retain the facts needed to identify the physical face:

```text
card family + Player owner + represented opposite-face Class + location while face-down
```

The exact syntax is provisional, but the intended relationships are:

```pets
ABSTRACT CLASS CardBack<CardLocation, Class<CardFront>> : Owned<Player>
ABSTRACT CLASS CardFront<Class<CardBack>> : Owned<Player>
CLASS PlayedEvent<Class<EventCard>> : Owned<Player>
```

An exact project back might therefore be
`ProjectCard<Player1, Hand, Class<Decomposers>>`. Playing it atomically transmutates that back into
`Decomposers<Player1, Class<ProjectCard>>`; completing an Event atomically transmutates its front
into `PlayedEvent<Player1, Class<SearchForLife>>`.

Only cards associated with a Player exist as Components. `Hand`, `Selecting`, and `Revealed` remain
unowned locations. Deck and discard do not become Components.

Counted card gains atomize before defaults, ownership specialization, and exact-face selection.
Two promised cards become two tasks because different faces are different concrete Types.

### The dealer is a deterministic projection of history

The game premise fixes each deck family's selected face set, a root seed, canonical face order, and
shuffle-algorithm version. The Event Log records exact card entries, removals, and in-World
transmutations.

The default dealer policy folds those facts to derive the remaining deck order, discard set,
reshuffle epoch, and next face. An exact gain into `Hand`, `Selecting`, or `Revealed` consumes the
next derived face. A pure removal of an exact back adds that face to derived discard. Moving a card
inside the World or turning a back into its front does neither.

When a deck is exhausted, the next entry request deterministically shuffles the derived discard set
with the next epoch seed. If both sets are empty, the gain is unavailable. A cache may accelerate
the fold, but deleting it and replaying the same premise and history must reproduce the same answer.
There is no independent mutable deck, discard list, or RNG cursor.

For every selected face, exactly one position is authoritative at a time:

```text
derived deck
OR derived discard
OR one exact in-World back
OR one exact in-World front
OR one exact PlayedEvent
```

### The Player controls timing; Admin supplies chance

An abstract draw contains two decisions. The controlling Player decides when to select the pending
draw among eligible work. After selection, its unresolved face is delegated to Admin, the
controlling scope waits, and the default Admin policy narrows it to the next derived face. The
resulting event still has the Actor the original instruction requires.

This should extend the existing Philares delegation mechanism rather than introduce a Chance Actor
or reinterpret `BY Admin`. The engine enforces that only Admin may narrow the face and that the
choice is legal. The default dealer policy, not the engine, promises that the chosen face is next in
its deterministic shuffle.

### Reveals and searches expose real transitions

A draw enters `Hand`; a reveal enters `Revealed`. A search examines the actual shuffled order. Each
rejected face first enters `Revealed` and is then removed to derived discard; the dealer must never
prefilter the deck to matching faces.

Printed predicates inspect immutable metadata on the represented front Class. Card backs do not
gain live tag Components, and plain `HAS` does not implicitly traverse a represented Class.
Operations such as “reveal until three matches” compose three sequential one-match searches, so
every inspected card and every discard remains visible in history.

### Hidden information is an observation concern

The master World and Event Log retain exact Types. A viewer who may know only that Player1 holds a
project card sees a broader projection of the exact back, not an `UnknownCard` Component.

That projection must cover every observation path together: component counts, queries, tasks,
results, and history. Hiding only one API would leak the face through another. Future deck order is
already secret because derived deck entries are absent from the World.

Do not expose real-card mode until the observation projection is complete. Ownership alone is not a
visibility rule: another Player's hand may be private while `Revealed`, in-play fronts, and
`PlayedEvent` are normally public.

### Introduce operations incrementally

Real-card behavior should be selected explicitly by the game premise, with follow mode remaining
the default. Lower one semantic operation family at a time: first draws, then simple reveals,
multi-card choices, sequential searches, and finally drafting. Definitions whose behavior is
already ordinary Pets remain shared.

Before implementation, prove the design in this order:

1. **Exact Types:** mutual face/back references, ownership, locations, defaults, atomization, and
   atomic play/Event transitions work in a synthetic Class Table.
2. **Delegation:** selecting a draw delegates only exact-face narrowing to Admin and blocks its
   controlling scope until completion.
3. **Dealer projection:** a tiny deck reproduces entries, discards, exhaustion, reshuffles,
   rollback, replay, and forks on JVM and JavaScript.
4. **Lifecycle:** every transition preserves the one-position rule for each selected face.
5. **Operation lowering:** one example of each operation family works without card-specific engine
   branches or duplicated card declarations.
6. **Observation:** every client-visible path hides unavailable identities consistently.

Keep follow mode passing throughout. Stop if exact Types or delegation require ProjectCard-specific
exceptions in the generic engine.

## Decisions still required

- the final dependency syntax and rendered argument order;
- the real-mode lowering form for `CARDS[...]`;
- the canonical ordering, seed derivation, and shuffle algorithms;
- how overlapping `Selecting` scopes are distinguished;
- the exact visibility and irreversible-knowledge policy; and
- whether a supported variant ever needs repeated distinguishable copies of one face.

These decisions do not alter the central plan: exact directly owned cards in the World, unowned
locations, no Deck or Discard Components, deterministic dealer state derived from premise plus
history, Admin narrowing of chance-selected faces, and viewer-specific loss of Type concreteness.
