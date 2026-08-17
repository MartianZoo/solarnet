# Shuffle-and-deal real-card mode

**Status: proposal.** Follow mode is committed and remains the default. Do not implement an isolated
piece of this design unless its gate below is selected and the preceding invariants remain intact.

## Goal

Let Solarnet own physical cards, shuffled decks, deals, draws, discards, drafts, reveals, event
piles, hidden information, and reproducible chance without abandoning the component model.

The minimum new concepts are:

1. card location;
2. timeline-owned chance resolution; and
3. player-relative observation with a committed knowledge horizon.

Do not add a second card database, mirrored hand counters, default card-copy identity, or an
off-timeline RNG.

## State model

An out-of-play physical card is one `CardBack` component with dependencies on its represented
`Class<CardFront>` and a `CardLocation`. A live card is its existing exact
`CardFront<Owner>`. Exactly one representation exists at a time.

Illustrative Types:

```pets
ABSTRACT CLASS Card
ABSTRACT CLASS CardLocation
ABSTRACT CLASS OwnedCardLocation<Owner> : Owned<Owner>, CardLocation
ABSTRACT CLASS PrivateCardLocation : CardLocation

CLASS Deck : CardLocation, PrivateCardLocation
CLASS Discard : CardLocation
CLASS Revealed : CardLocation
CLASS Hand : OwnedCardLocation, PrivateCardLocation
CLASS Events : OwnedCardLocation

ABSTRACT CLASS CardBack<Class<CardFront>, CardLocation> : Card
CLASS ProjectCard : CardBack, Atomized
CLASS PreludeCard : CardBack, Atomized
CLASS CorporationCard : CardBack, Atomized

ABSTRACT CLASS CardFront : Card, Owned<Player>
```

Exact syntax and draft-location names are not settled. The semantics are:

- location is a singleton component dependency, not a string property or external map;
- ownership and visibility are separate;
- `Card` cannot itself be owned because deck cards have no owner;
- code finds an out-of-play owner only through an `OwnedCardLocation`; and
- card movement normally transmutates one location dependency atomically.

Example lifecycle:

```text
ProjectCard<Class<Card131>, Deck>
ProjectCard<Class<Card131>, Hand<Player2>>
Card131<Player2>
ProjectCard<Class<Card131>, Events<Player2>>
```

The represented Class is immutable. Playing transmutates the hand back into the front. Event cleanup
transmutates the front into the matching back at Events. Live tags, effects, and card resources exist
only while the front exists.

## Multiplicity and physical supply

Equal physical copies remain multiplicity of one Type. Add per-copy identity only if a supported
rule distinguishes equal copies through hidden ordering. Beginner Corporation copies do not justify
identity.

For one printed face, physical supply is conserved across:

```text
exact CardFront at any owner
OR
CardBack representing that front at any location
```

An ordinary card has maximum one; canonical metadata may declare a larger supply. The current
Limiter cannot express this additive union invariant. The narrow proposed extension is to permit a
class invariant over a union of component-count Expressions using existing no-double-count union
semantics. A source and destination both inside the union produce net zero.

Do not generalize invariants to arbitrary arithmetic or custom metrics as part of this work.
Setup separately proves that it assembled exactly the selected supply.

## Movements

| Operation | Atomic change | Choice owner |
| --- | --- | --- |
| Draw | `Hand<Owner> FROM Deck` | Chance |
| Reveal | `Revealed FROM Deck` | Chance |
| Keep reveal | `Hand<Owner> FROM Revealed` | Player/rule |
| Reject reveal | `Discard FROM Revealed` | Player/rule |
| Discard from hand | `Discard FROM Hand<Owner>` | Player |
| Deal draft packet | `DraftHand<Owner> FROM Deck` | Chance |
| Draft | `Drafted<Owner> FROM DraftHand<Owner>` | Player |
| Pass packet | `DraftHand<Next> FROM DraftHand<Owner>` | Seat rule |
| Finish draft | `Hand<Owner> FROM Drafted<Owner>` | Research rule |
| Play | exact front `FROM` matching hand back | Player |
| Finish event | matching Events back `FROM` front | Automatic |
| Recover event | `Hand<Owner> FROM Events<Owner>` | Player/rule |
| Reshuffle | each eligible `Deck FROM Discard` | Automatic |

Link the represented face across both sides. Never implement movement as gain `THEN` removal; the
duplicate/gap would be observable.

`Revealed` is a real location so committed choices and rollback use ordinary state. One reveal
scope must drain before another. Add a scope dependency only if supported rules prove overlapping
reveals are necessary.

## Familiar card-back shorthand

In real mode, after ordinary Owner defaults:

```text
ProjectCard
  -> ProjectCard<Class<CardFront>, Hand<Owner>>
       FROM ProjectCard<Class<CardFront>, Deck>

-ProjectCard
  -> ProjectCard<Class<CardFront>, Discard>
       FROM ProjectCard<Class<CardFront>, Hand<Owner>>
```

Prelude and Corporation backs follow the same rule. `N ProjectCard` atomizes into N one-card moves
before chance narrowing so it can select N different faces without replacement.

Use one named, mode-aware lowering. It recognizes only omitted-location pure gains/removals,
preserves one linked face constraint, never chooses a card, never rewrites explicit `FROM`, and
rejects physical pure gains/removals outside controlled setup. Do not teach every gain to infer a
source from invariant failure.

Follow mode may use the same shape with a system-only `UntrackedCardFront` in hand. A client-supplied
play consumes one untracked back and creates the supplied front. Real mode must never accept an
untracked back or expose it as a playable face.

## Chance narrows; it does not mutate

After lowering, a deck-sourced transmutation is abstract. Chance:

1. enumerates exact matching source components from the master World;
2. applies the named physical search policy;
3. chooses by deterministic canonical candidate order and multiplicity;
4. narrows every linked face occurrence; and
5. executes the resulting ordinary instruction.

Only chance-owned sources auto-narrow this way. Hand discard and revealed-card selection remain
Player tasks. Deck exhaustion and reshuffling are explicit rules, not selector fallbacks.

This requires a focused abstract-instruction resolution seam. Do not put a `ProjectCard` branch in
`Instructor.prepareChange` or per-card Kotlin draw implementations.

### Timeline requirements

- Premise includes seed plus algorithm/version.
- Candidate order is identical on JVM and JavaScript.
- Every outcome is recorded atomically with the instruction it narrows.
- Rollback restores chance position and retry repeats the outcome.
- Replay consumes recorded results, not a future RNG implementation.
- Forks share history and diverge deterministically only after their histories differ.
- The original Actor still performs the change; chance provenance is not another Actor.

A mutable RNG cursor outside the Event Log is invalid. Preparation is currently observational, so
chance cannot secretly append an event inside a read-only prepare call. Prototype the transaction
boundary before canonical card work.

Repeatability alone does not prevent rollback fishing across different pre-draw branches. Card
identity must remain sealed until the decision is irrevocably committed.

## Printed facts

A card back in Deck has no live `MicrobeTag` component. Do not make `HAS` silently jump through
every CardBack's represented Class.

Use an honest metadata property or metric:

```text
ProjectCard<
  Class<CardFront>(HAS PrintedTag<Class<MicrobeTag>>)
>
```

A temporary custom metric could read canonical `CardDefinition` metadata. The chance selector
receives an already filtered source Type and knows nothing about tags. Cost and requirement filters
reuse the card Class's immutable properties; color, deck, and other printed facts need similarly
honest properties or metadata bridges.

Do not generate subclasses or live components for every printed fact.

The abbreviated argument form above still needs a focused type proof; use the fully explicit form
if dependency specialization cannot justify it. Any future `ProjectCard(HAS MicrobeTag)` sugar must
lower to the explicit printed-fact constraint rather than changing the global meaning of `HAS`.

## Player-relative observation

Rules, workflow, chance, and setup always use the exact master reader. A Player receives a projected
read model.

Committed location policy:

| State | Owner sees face | Others see face |
| --- | --- | --- |
| Deck | no | no |
| Hand / private draft zone | yes | no |
| Revealed / Discard / Events | yes | yes |
| live CardFront | yes | yes |

This policy proves that ownership is not visibility: Deck is secret but unowned; Events is public
but owned.

Visibility is also bounded by the viewer's **knowledge horizon**. An exact identity produced after
the latest irrevocably published checkpoint remains abstract even to its eventual owner and even at
a normally public location. Before commit:

```text
master:  ProjectCard<Class<Card131>, Hand<Player1>>
viewer:  ProjectCard<Class<CardFront>, Hand<Player1>>
```

Publishing a chance result must atomically:

1. validate the deterministic work before the reveal;
2. record the exact chance movement internally;
3. advance the rollback floor through it;
4. advance eligible viewers' knowledge horizons; and
5. only then return the exact permitted observation.

A reveal-dependent choice is offered after this commit-and-reveal barrier. The engine cannot later
ask a player to forget a card because downstream work dead-ended.

Projection occurs before query evaluation. It collapses hidden exact Types into an abstract visible
Type and sums multiplicity. Exact `count`, `has`, refinement, union, custom-metric, task, result,
event, cause, replay, and history access must all use the same projection or reject the query.
Scrubbing only `getComponents()` leaks information.

Do not add `KnownTo` components or fake playable `UnknownCardFront` Classes to the master
ontology. The observation schema may represent unknowns.

The target observation builder derives a per-viewer knowledge ledger from committed event history.
An identity already published to a viewer stays known if that card later enters a private location.
Unique represented faces are sufficient to follow ordinary cards; multiplicity is sufficient for
interchangeable copies. Add physical copy identity only if a supported hidden mixing rule makes
equal known copies distinguishable.

A custom metric running against a projected view must not regain the master reader by casting. If a
reveal-dependent path can be intrinsically impossible, detect that before publication or keep the
whole operation engine-controlled; committing merely to expose a choice must never freeze an
illegal prefix.

## Playing and leaving play

**Proposed real-mode behavior.** An exact play transition proves that the card is in this Player's
hand, that its represented Class is the front being played, and that its back kind belongs to the
right deck family:

```text
Card131<Player2>
  FROM ProjectCard<Class<Card131>, Hand<Player2>>
```

The real-mode path would no longer need `CheckCardDeck`; the exact source Type authenticates the
deck. Follow mode may retain that custom check while it consumes an untracked back.

An Event should later transmutate its live front into the same represented face at
`Events<Owner>`. Its back kind must come from canonical deck metadata rather than assuming that
every future Event is a Project. Removing the front naturally removes its live tags, Effects, and
card resources.

Rules and queries over the current mirrored `PlayedEvent<Class<CardFront>>` representation should
migrate to located backs at Events. Do not preserve a second `PlayedEvent` component merely for
compatibility.

If supported content removes a physical card from the game, add a public `Removed` location rather
than deleting the card. Do not add that location before a selected rule requires it.

## Mode-specific definitions

Real-card mode is an affirmative Module fixed in the premise and mutually exclusive with follow
mode. `F` definitions remain follow-mode variants whose client supplies card outcomes. Real
counterparts use the non-`F` identity and explicit physical movements.

Migrate by operation family, not by stripping `F` mechanically:

1. reveal one and optionally keep/buy;
2. reveal several and choose;
3. reveal and test a printed fact;
4. search with a printed filter;
5. ordinary consequential draws; and
6. recover a known event.

Shared cards with no material transition difference need one definition.

## Rejected designs

- mirrored hand counters;
- add-then-remove movement;
- source inference from invariant failure;
- off-timeline randomness;
- “owned means private”;
- output-only hidden-state scrubbing;
- a universal CardBack `HAS` substitution rule;
- per-card draw code;
- eager deck-position components without a supported ordering rule;
- default copy identity; or
- a Chance Actor.

## Gates

1. **Types and invariant:** prove locations, singleton dependencies, linked moves, physical-supply
   union limits, and one printed-metadata refinement in a synthetic table.
2. **Tiny deterministic deck:** move three exact cards through every location and live-front state;
   prove conservation and shorthand atomization.
3. **Timeline chance:** specify cross-platform RNG/order, event recording, rollback/replay/forks,
   reshuffle, filtering, and sealed results.
4. **Observation:** project all query/task/history paths and couple commit, rollback floor, knowledge
   horizon, and publication.
5. **Real setup:** assemble selected supply, deal all starting material, and migrate one complete
   example of every operation family.

Do not touch canonical card data if Gate 1 requires global Type-system exceptions. Do not expose the
mode before Gates 3 and 4. Keep follow mode green and default until a real setup can play through
event scoring.

## Acceptance properties

**Target acceptance criteria, not current guarantees.** Integration and property coverage must
establish at least:

1. Every selected face has the configured total multiplicity across fronts and backs.
2. Ordinary movement preserves represented Class and changes only location.
3. Playing and finishing an Event preserve identity and total physical-card count.
4. `N ProjectCard` makes N sequential selections without replacement.
5. Equal seeds and histories produce equal JVM/JavaScript draws; different seeds vary usefully.
6. Rollback and retry reproduce an outcome; replay uses the recorded result.
7. Printed-tag filtering selects only qualifying definitions without live tag components.
8. Discarding from hand is the owning Player's exact-card choice.
9. Private own zones, hidden other zones, and public zones obey the visibility matrix.
10. Exact count, refinement, union, custom-metric, task, and history queries cannot recover hidden
    identities.
11. The master rules engine still validates and executes against exact hidden state.
12. Real and follow definitions for one printed card cannot both be active.
13. Interchangeable duplicate backs coexist without invented copy Classes.
14. Before commit, a Player's own draw and every public reveal remain abstract in all Player-facing
    component, task, result, and history views.
15. Commit disables earlier rollback before publishing newly visible identities; a simulation fork
    cannot advance the live session's knowledge horizon.
16. A reveal-dependent choice appears only after its reveal is committed, so a later dead end never
    asks a Player to forget a card.

## Open design decisions

These are aspirations requiring proof during the gates, not missing current behavior:

- final names for the two draft locations;
- whether one global `Revealed` location is sufficient;
- the transaction boundary for chance resolution during task preparation;
- the relationship among the live rollback floor, fork floors, and per-session knowledge horizons;
- which real `F` counterparts search and reshuffle versus reveal and discard;
- whether supported content requires persistent deck order; and
- whether `UntrackedCardFront` remains isolated enough to avoid mode-selected declarations.

None changes the proposed central model: exact inactive back plus location, exact live front, atomic
movement, timeline-owned chance, and Player-relative observation.
