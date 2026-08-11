# Shuffle-and-deal real cards mode

> **Agent record:** This is not user documentation, just an agent architecture record written
> neither by humans nor for humans.

## Purpose

Define the smallest coherent model in which Solarnet owns physical cards, shuffled decks, deals,
draws, discards, drafts, reveals, played-event piles, hidden information, and reproducible chance.
The design must preserve the useful fiction that components have only types and multiplicity. It
must not add a second card database, mirror hand counts beside physical cards, or let randomness
escape the timeline.

This is a target design, not a description of current behavior. Follow mode remains useful and
should remain the default until the state, chance, and observation boundaries below are complete.

## Recommendation in one page

1. A card which is not in play is one ordinary `CardBack` component. Its two dependencies are the
   represented `Class<CardFront>` and a `CardLocation`. Moving it normally transmutes only the
   location dependency.
2. A card in play is its existing exact `CardFront<Owner>` component. Playing a card atomically
   transmutes the matching hand `CardBack` into that front. An event later transmutes its front
   into the same card identity at `Events<Owner>`. Thus every physical card is represented by
   exactly one `CardBack` or one `CardFront`, never both.
3. Keep `ProjectCard`, `PreludeCard`, and `CorporationCard` as the familiar card-back kinds. A small,
   named real-card lowering turns a pure gain such as `ProjectCard` into `ProjectCard<..., Hand>
   FROM ProjectCard<..., Deck>` and a removal into `ProjectCard<..., Discard> FROM
   ProjectCard<..., Hand>`. The result is an ordinary linked transmutation before execution.
4. Randomness does not perform a special mutation. It narrows a deck-sourced abstract
   transmutation to one exact legal transmutation, after which the ordinary instructor, limiter,
   event log, effects, and rollback machinery apply.
5. Use a deterministic, timeline-tracked chance source. A mutable `Random` instance outside the
   event history is unacceptable: rollback, replay, forks, and duplicated-seed evaluation must
   reproduce the same choice.
6. A player reader is a projection of the master world, not the reader used by the rules engine.
   It maps secret or not-yet-committed card identities to an abstract face bound before answering
   queries. Even a player's own newly drawn card stays sealed while the player can still roll back
   past its draw. Scrubbing only `getComponents()` would still allow exact `count()` queries to
   leak the deck.
7. Visibility is not ownership. `Events<OtherPlayer>` is owned but public; `Deck` is unowned but
   secret. Declare or centrally define private locations instead of scrubbing every
   `OwnedCardLocation<!Viewer>`.
8. Printed facts used to filter an unplayed card need an honest metadata metric, for example
   `PrintedTag<Class<Tag>, Class<CardFront>>`. Do not pretend that a `MicrobeTag<CardFront>`
   component exists while its card is in the deck.
9. Preserve the follow-mode `F` definitions and add their real-mode counterparts under mutually
   exclusive mode requirements. Do not mix follow-mode trust shortcuts into real-card rules.

The added concepts are therefore limited to location, chance narrowing/history, and observation
projection. The actual state change remains the existing atomic transmutation.

## State model

The following is illustrative Pets, not settled declaration syntax:

```pets
ABSTRACT CLASS Card

ABSTRACT CLASS CardLocation {
  CLASS Deck { HAS =1 This }
  CLASS Discard { HAS =1 This }
  CLASS Revealed { HAS =1 This }
}

"A location belonging to an Owner; ownership alone says nothing about visibility"
ABSTRACT CLASS OwnedCardLocation<Owner> : Owned<Owner>, CardLocation {
  CLASS Hand
  CLASS Events
  CLASS DraftHand
  CLASS Drafted
}

"A location whose card identities are not public"
ABSTRACT CLASS PrivateCardLocation : CardLocation

"An out-of-play physical card"
ABSTRACT CLASS CardBack<Class<CardFront>, CardLocation> : Card

CLASS ProjectCard : CardBack, Atomized
CLASS PreludeCard : CardBack, Atomized
CLASS CorporationCard : CardBack, Atomized

"A physical card currently in play"
ABSTRACT CLASS CardFront : Card, Owned<Player>
```

`Hand`, `DraftHand`, and `Drafted` would also narrow `PrivateCardLocation`; `Deck` is private
without being owned. `Events`, `Discard`, and `Revealed` are public. The two draft names are
working names: one is the packet currently controlled by a player and one is the player's
already-selected set. Their semantic distinction matters more than the final names.

Every concrete location is a real singleton component. An owned location has one singleton for
each participating Owner. This satisfies the existing rule that dependency targets have maximum
multiplicity one. The location is not a string property on a card and not an external map.

The root `Card` can no longer itself be `Owned<Player>`, because a deck card is not owned. A
`CardFront` is directly owned as today. A `CardBack` obtains any player relationship through its
location. Code which needs the owner of an out-of-play card must deliberately inspect an
`OwnedCardLocation`; it must not extend the generic `Component.owner` heuristic with an implicit
"first nested Owner wins" rule.

### Example states

```text
ProjectCard<Class<Card131>, Deck>
ProjectCard<Class<Card131>, Revealed>
ProjectCard<Class<Card131>, Hand<Player2>>
ProjectCard<Class<Card131>, Discard>
Card131<Player2>
ProjectCard<Class<Card131>, Events<Player2>>
```

The first dependency is immutable throughout real-card play. The four `ProjectCard` transitions
change only the second dependency. Playing the card consumes the hand component and creates
`Card131<Player2>`; resolving an event consumes that front and recreates the back at `Events`.

`CardBack` means "the card is not a live card front", not necessarily "the physical card is face
down". Cards at `Revealed`, `Discard`, and `Events` are normally public after their identities are
committed and published even though their engine state is a `CardBack`. Location and knowledge
horizon jointly control visibility.

## Identity and multiplicity

Components still have no instance identity. Two equal physical cards in one location are two
copies of the same exact component type. That is sufficient while the rules cannot distinguish
the copies. Multiple Beginner Corporation cards are therefore ordinary multiplicity, not invented
classes such as `BeginnerCorporationCopy3`.

The physical supply of a printed face is the total multiplicity across all of these states:

```text
that exact CardFront, for any owner
OR
any CardBack whose class dependency represents that CardFront, at any location
```

For an ordinary unique card the maximum is one. A definition which has several physically
interchangeable copies supplies the larger maximum. The existing exact-card `MAX 1` invariant
needed by `Cardbound` dependencies remains; the physical-supply restriction is an additional
cross-state restriction.

The current `Limiter` cannot enforce this as written: it accepts only one counted component
expression, not an additive union such as:

```pets
HAS MAX 1 (Card131 OR CardBack<Class<Card131>, CardLocation>)
```

The principled extension is narrow. Permit invariants over a union of component-count expressions,
using the reader's existing no-double-count union semantics. For limit calculation, a concrete
source or destination contributes either zero or one to the union. An atomic move whose source and
destination both contribute has delta zero and therefore preserves the restriction. Do not expand
class invariants to arbitrary custom metrics or general arithmetic as part of this project.

Card definitions may derive this restriction from their canonical physical-copy count, just as
they already derive their exact-card invariant. This is canonical metadata producing a general
invariant, not a card-specific rule implementation. Initial real-card assembly must separately
validate that the selected supply was created exactly once. Thereafter ordinary card operations
are transmutations, so the mathematical metric `CardFront OR CardBack` is conserved. A trusted raw
state editor may still bypass game rules, as it can today.

If a future shuffled deck contains indistinguishable duplicate faces and a rule can distinguish
their positions, multiplicity is no longer enough. Add copy identity only for that demonstrated
case. Beginner Corporation copies, which do not occupy a shuffled deck, do not justify it.

## Movements

| Operation | Atomic state change | Selector |
| --- | --- | --- |
| Draw | `Hand<Owner> FROM Deck` | chance |
| Draw a matching card | `Hand<Owner> FROM Deck` with a face constraint | chance plus the rule's search semantics |
| Reveal top card | `Revealed FROM Deck` | chance |
| Keep a reveal | `Hand<Owner> FROM Revealed` | player or rule |
| Reject a reveal | `Discard FROM Revealed` | player or automatic rule |
| Discard from hand | `Discard FROM Hand<Owner>` | player unless already exact |
| Deal a draft packet | `DraftHand<Owner> FROM Deck` | chance |
| Draft a card | `Drafted<Owner> FROM DraftHand<Owner>` | player |
| Pass a packet | `DraftHand<Next> FROM DraftHand<Owner>` | seating rule |
| Finish draft | `Hand<Owner> FROM Drafted<Owner>` | purchase/research rule |
| Play | exact `CardFront<Owner> FROM ...<Class<CardFront>, Hand<Owner>>` | player chose the face |
| Finish an event | `...<Class<CardFront>, Events<Owner>> FROM CardFront<Owner>` | automatic |
| Recover an event | `Hand<Owner> FROM Events<Owner>` | player or rule |
| Reshuffle | each eligible `Deck FROM Discard` | automatic; begins a new shuffle epoch |

The same represented class expression must be linked across both sides of every back-to-back move.
The play and finish-event transitions must derive one side from the other's exact class rather than
accepting two independent card choices.

`Revealed` is intentionally a real location rather than a transient Kotlin list. After the reveal
is committed, ordinary player tasks can choose among its cards; before commit those task views must
not disclose the faces. The location also makes rollback exact. The first version must ensure that
one reveal scope drains before another begins. If supported rules demonstrate overlapping reveal
groups, add an explicit reveal-scope dependency; do not allow a player to choose from an unrelated
global reveal pool.

A card which leaves the game should move to a future public `Removed` location rather than cease to
exist. Do not add that location until a supported rule needs it.

## The familiar `ProjectCard` shorthand

The terse card vocabulary is worth preserving. In real-card mode, after contextual Owner defaults,
the semantic expansions are:

```text
ProjectCard
  => ProjectCard<Class<CardFront>, Hand<Owner>>
       FROM ProjectCard<Class<CardFront>, Deck>

-ProjectCard
  => ProjectCard<Class<CardFront>, Discard>
       FROM ProjectCard<Class<CardFront>, Hand<Owner>>
```

`PreludeCard` and `CorporationCard` use the same mechanism where their rules call for it. A count
such as `3 ProjectCard` must atomize into three one-card moves before chance narrowing; one exact
Pets transmutation cannot draw three different component types. The card-back kinds should
therefore be `Atomized`.

This expansion should be one named, mode-aware preprocessing rule with a deliberately small
surface:

- it recognizes only a pure gain or removal of a card-back kind whose location was omitted and
  filled by the ordinary contextual default;
- it preserves the authored face constraint on both sides as one linked choice;
- it does not select a face, consult the deck, or mutate the world;
- it never rewrites an already explicit `FROM` instruction; and
- an explicit pure gain/removal of a physical back is rejected during ordinary real-card play,
  except at the controlled setup boundary.

This is preferable to teaching every `Gain` to infer a source from whatever invariant it happens
to violate. If a declaration-level change-template syntax later proves useful, it may replace the
named lowering. Inventing such a language feature solely to hide three transparent expansions is
not a prerequisite.

The lowering produces ordinary Pets. The subsequent chance selector is a separate concern.

### Follow-mode compatibility

The common type shape can also represent follow mode without materializing a deck. A follow-mode
hand contains backs represented by a concrete system-only `UntrackedCardFront` class. Gaining and
removing `ProjectCard` changes that untracked hand multiplicity as it does today. Playing a client-
supplied card consumes an untracked back and produces the supplied exact front.

Thus the represented class is immutable in real-card mode but intentionally unknown until play in
follow mode. Keep that trust boundary explicit. Do not weaken the real-mode play path to accept an
untracked back, and do not expose `UntrackedCardFront` as a playable card choice.

This shared type shape is preferred to two conflicting declarations of `ProjectCard`. If the
system-only marker proves to contaminate enumeration or invariants, stop and compare it with
mode-selected declarations before proceeding; do not accumulate exclusions throughout the engine.

## Selecting the card from a deck

An expanded draw remains abstract because several exact source components match. That is a choice,
but not the player's choice. Chance is a narrowing authority:

1. Query the master component graph for exact source components satisfying the whole source type.
2. Apply the operation's physical search rule: next shuffled card, first matching card with the
   specified treatment of skipped cards, or another explicitly named policy.
3. Narrow every linked occurrence of the represented face to the selected exact class.
4. Verify that the exact instruction narrows the authored instruction.
5. Execute it through the ordinary instructor.

This belongs behind a focused custom narrowing/resolution seam, not as a `CardBack` mutation API
and not as scattered custom implementations on individual cards. The present `CustomClass`
facility translates only a concrete pure gain and therefore is not quite the right seam: a deck
selector must inspect an abstract linked transmutation and return its exact narrowing. Generalize
custom instruction resolution only enough to support that contract rather than inserting a
`ProjectCard` special case directly in `Instructor.prepareChange`.

A hand-to-discard move remains abstract for its assignee to narrow. A revealed-card choice also
remains a normal task. Only a source governed by a chance policy is automatically narrowed by
chance.

Deck exhaustion is a rule, not an accident of the selector. The appropriate deck rule may first
move the eligible discard pile back to `Deck` and establish a new shuffle epoch. A filtered search
with no qualifying card must follow that effect's specified optional/failure behavior; it must not
silently draw an arbitrary card.

## Filtering by printed card facts

This attractive spelling is not currently meaningful:

```pets
ProjectCard(HAS MicrobeTag)
```

The candidate is a `ProjectCard` back, while `MicrobeTag` depends on a live `CardFront`. Moreover,
printed tags are currently instantiated only when the front enters play. There is no Microbe-tag
component for a card still in `Deck`.

Do not change refinement substitution so that every `HAS` on a `CardBack` secretly jumps through
its class dependency. That would make ordinary Pets type reading context-dependent and would still
confuse printed facts with live components.

Instead add the same kind of honest metadata bridge already used by `ClassCardRequirement`:

```pets
"A custom metric reporting how many of Tag are printed on CardFront"
CLASS PrintedTag<Class<Tag>, Class<CardFront>> : Custom
```

Then a canonical face constraint can be expressed along these lines:

```pets
ProjectCard<
  Class<CardFront>(HAS PrintedTag<Class<MicrobeTag>>)
>
```

The represented-class refinement binds the candidate front class into `PrintedTag`; the custom
metric reads the existing `CardDefinition.tags` authority. The card selector itself knows nothing
about Microbe tags. It merely receives a source type which the normal state-aware query has already
filtered.

The exact argument elision above depends on existing dependency specialization and must be proven
with a focused type test. A fully explicit form is acceptable if needed. Parser sugar resembling
`ProjectCard(HAS MicrobeTag)` may be considered only after the canonical Pets meaning is settled;
the sugar must lower to the explicit printed-metadata constraint and never redefine `HAS` globally.

Cost, requirement, color, deck, or other printed filters should reuse existing class metadata
metrics or add similarly honest ones. Do not generate one subclass per printed property and do not
instantiate every printed fact as a live component while the card is in the deck.

Different physical instructions must remain different even if they use the same face filter. A
rule which searches and reshuffles is not the same as revealing cards and discarding misses. Real
counterparts of the `F` definitions must spell out the appropriate locations and choices rather
than calling one universal "draw matching" shortcut.

## Reproducible chance

The component graph is a multiset and contains no deck order. For rules which only consume cards
from the top, a lazy random permutation is enough: selecting uniformly from the remaining deck on
each draw is distributionally equivalent to shuffling once and consuming the permutation. A
sequence of reveals records the resulting order through its sequence of moves.

This avoids `DeckPosition1` through `DeckPosition500`, adjacency chains, and order-maintenance
effects. It is valid only while supported mechanics do not return a known card to a specified top
or bottom position, inspect without removing, or otherwise preserve relative order. If such a rule
is selected, add an explicit order model before claiming support. Do not emulate it with another
random draw.

Chance has these requirements:

- the premise contains a seed and an algorithm/version identifier;
- candidate order is canonical across JVM and JavaScript, never collection iteration order;
- multiplicity weights equivalent candidates correctly;
- every committed outcome is recorded at the same atomic history boundary as the instruction it
  narrows;
- rollback removes the outcome and restores the chance position;
- retry from the same checkpoint selects the same outcome, preventing rollback fishing;
- persisted replay consumes the recorded outcome rather than depending on a newer random
  algorithm; and
- state forks share the prefix and diverge deterministically only after their histories diverge.

The chance position must be timeline state, whether represented by dedicated `ChanceDecisionEvent`
entries or an equivalently explicit event-log record. It must not be a mutable RNG cursor held by a
service which `Timeline.rollBack` cannot reverse. The exact prepared instruction or its internal
task data must retain the decision until execution; a prepared task's global lock prevents the
deck from changing underneath it.

Preparation is currently observational. A chance design must preserve that contract or explicitly
move chance resolution into a timeline-recorded preparation operation. It must not append a hidden
event from deep inside an otherwise read-only `Instructor.prepare()` call. This transaction detail
must be prototyped before implementing card data.

The seed, chance position, and secret decision events belong to the trusted world. A player-facing
reader must not expose enough information to reconstruct future cards. Duplicated-seed evaluation
can still supply the same secret seed to several environments.

Deterministically replaying one draw is necessary but not sufficient to prevent rollback fishing.
A player could explore different pre-draw branches and learn different counterfactual cards even
if each individual branch repeats perfectly. No chance-derived identity may be published while the
viewer can roll back before the decision which exposed it. Internal rollback used to test and
abandon an illegal branch must reproduce the same decision, and the observation boundary must keep
that decision sealed until commitment.

## Player-relative observation

The rules engine, workflow, setup assembler, and chance selector require the exact master reader.
Never run them against a scrubbed reader. The later client boundary may expose another
`GameReader`-shaped projection scoped to a viewer, but it is a read model over the master world.

### Base visibility after commitment

Once the event which established the relevant state is committed and publishable, the location
policy is:

| Location/state | Owner sees face | Other player sees face |
| --- | --- | --- |
| `Deck` | no | no |
| `Hand<Owner>` | yes | no |
| `DraftHand<Owner>` | yes | no |
| `Drafted<Owner>` | yes | no |
| `Revealed` | yes | yes |
| `Discard` | yes | yes |
| `Events<Owner>` | yes | yes |
| live `CardFront<Owner>` | yes | yes |

This table is policy, not a consequence of `OwnedCardLocation`. In particular, Events is public
and Deck is private. If a draft variant exposes different information, use a distinct location or
an explicit visibility rule; do not overload ownership.

### Commit-gated knowledge

Location policy is only the upper bound on what a viewer may learn. A second bound is the viewer's
**knowledge horizon**: the latest checkpoint which has been made irrevocable and published to that
viewer. The engine may already contain an exact card after that checkpoint, but the projected view
must retain an abstract face until the horizon advances through the identity-producing event.

This applies even to the card's owner:

```text
master speculative state:
  ProjectCard<Class<Card131>, Hand<Player1>>

Player1 before commit:
  ProjectCard<Class<CardFront>, Hand<Player1>>

Player1 after commit through the draw:
  ProjectCard<Class<Card131>, Hand<Player1>>

Player2 both before and after that commit:
  ProjectCard<Class<CardFront>, Hand<Player1>>
```

A public reveal after the rollback floor is also sealed from every player until it is committed.
Otherwise a player could reveal, inspect, roll back, and choose a different branch. Once committed,
`Revealed`, `Discard`, and `Events` use their normal public policy.

`Timeline.commit()` already advances a global rollback floor, but the current API neither exposes
that floor as an observation horizon nor publishes a commit result. The safe client boundary needs
one atomic operation which:

1. advances the authoritative rollback floor to a checkpoint;
2. makes rollback before that checkpoint impossible;
3. advances the eligible viewers' knowledge horizon; and
4. only then returns observations containing the newly visible identities.

Never reveal first and commit second. A private simulation fork may have its own rollback floor;
advancing that fork must not implicitly publish facts to the live player session.

Some workflows need a choice based on newly revealed cards. Such a task cannot expose its exact
options while its reveal remains reversible. Treat it as a **commit-and-reveal barrier**:

1. complete and validate every deterministic pre-reveal choice which must remain undoable;
2. perform the chance move in the trusted world while its identity is still sealed;
3. commit through the chance outcome;
4. publish the newly visible cards; and
5. only then offer the card-dependent choice, whose own later work may be speculative from the new
   floor.

If the post-reveal path could be intrinsically impossible, detect that before publishing or keep
the whole operation engine-controlled. The engine cannot ask a player to unlearn a card after a
later dead end.

The safe client must permit `commit()` only at a validated command or workflow boundary. Advancing
the floor in the middle of an arbitrary unresolved task graph could freeze an illegal prefix merely
to reveal its cards.

The observation builder therefore needs more than `(viewer, current location)`. It needs the
viewer's committed observation history or an equivalent knowledge checkpoint. A deterministic
builder can derive a per-viewer knowledge ledger from the exact event-log prefix and cache it as a
read model. Do not add `KnownTo<Player, Card>` components to the physical component graph.

The ledger must distinguish already-known entries from exact-but-sealed entries in the current
multiset. For the overwhelmingly unique card supply, the represented face is enough to follow a
known card through committed moves. Interchangeable duplicate faces can use multiplicity. If a
future rule mixes individually known equal copies through a hidden ordering operation, extend the
observation model only then; do not add physical copy identity preemptively.

For a hidden card, map:

```text
ProjectCard<Class<Card131>, Hand<Player2>>
```

to an observation carrying no narrower face claim than:

```text
ProjectCard<Class<CardFront>, Hand<Player2>>
```

Using the abstract bound is preferable to installing a fake playable `UnknownCardFront` in the
master class table. The same projection represents both permanently secret identities and exact
identities merely sealed until commit. An observation multiset may contain abstract projected
types even though the master `ComponentGraph` may contain only concrete components. If a
serialization format needs an unknown marker, keep that marker in the observation schema rather
than game ontology.

Projection collapses all hidden exact types which map to the same visible type and sums their
multiplicity. Query evaluation then runs against the projected multiset:

- `count(ProjectCard<..., Hand<Player2>>)` reports the public hand size;
- `count(ProjectCard<Class<Card131>, Hand<Player2>>)` reports zero to Player1 unless that
  observation explicitly retains the identity; and
- enumeration never returns the hidden exact type.

Scrubbing the returned list after a master `count()` is not sufficient. `has`, `containsAny`,
refinement checks, unions, and custom metrics must all observe the same projection or reject a
query whose answer is not visible. A custom metric must not regain the master reader by casting.

Components are only one leak path. Player-facing tasks, task revisions, change events, causes,
chance events, summaries, and replay/history APIs need the same projection. A master draw event
names its exact source and destination; another player may see that one card was drawn, but not
which face. The owner also sees only an unknown draw until the knowledge horizon passes it.
Publicly revealed events retain the face only after commitment. Previously published history
should remain visible even if the current card later enters a private location.

The restrictive observation API belongs around the trusted workhorse described in
[API.md](API.md). It is not a reason to make the existing internal `GameReaderImpl` forget facts.

## Playing and leaving play

Real-mode `PlayCard` no longer needs to trust a separately supplied back kind. Its exact source
proves all relevant facts:

- the card is physically in this player's hand;
- its represented class is the front being played; and
- its card-back kind records the deck family.

The requirement and cost checks continue to read the selected `CardDefinition`. The final state
change is conceptually:

```text
Card131<Player2>
  FROM ProjectCard<Class<Card131>, Hand<Player2>>
```

The existing `CheckCardDeck` custom becomes unnecessary on this path because an exact mismatched
back cannot satisfy the source. Follow mode may retain its trusted check while it consumes an
untracked back.

The event-card rule becomes conceptually:

```pets
ABSTRACT CLASS EventCard {
  This: ProjectCard<Class<This>, Events<Owner>> FROM This
}
```

The actual back kind must come from canonical deck metadata rather than assuming every event is a
project if future material disproves that assumption. The automatic tags and immediate effects
still fire when the front enters play; when the front leaves, ordinary dependency removal removes
its live tags and card resources. Its inactive back at Events carries only face identity and
location.

Existing queries and rules over `PlayedEvent<Class<CardFront>>` migrate to located backs at
`Events`. Do not retain a mirrored `PlayedEvent` component merely to avoid changing those rules.

## Follow-mode and `F` definitions (aspirational design)

Real-card mode is an affirmative Module and must be fixed in the premise. It is not a flag
consulted by every draw while a game runs. Follow mode and real-card mode are mutually exclusive.

The `F` suffix continues to identify the follow-mode definition whose client supplies a reveal,
selection, discard, or draw outcome. A real counterpart uses the printed non-`F` identity and a
mode requirement which prevents both definitions from entering one assembled game. Shared cards
without a material rule difference need only one definition.

Do not mechanically remove `F` or put a mode branch inside every affected card. Group the real
counterparts by the physical operation they demonstrate:

1. reveal one and optionally buy/keep it;
2. reveal several and choose some;
3. reveal and test a printed fact;
4. search for cards with a printed fact;
5. draw ordinary cards as a consequence; and
6. recover known cards from `Events`.

Implement one complete representative of each required operation before converting the rest.
Each real definition should consist mainly of ordinary location moves and player tasks; custom
code should supply only canonical metadata queries or chance narrowing.

Definition selection and replacement already occur before class assembly. Mode requirements
should use that boundary. If two mode-specific definitions need related printed identity for UI or
catalog purposes, expose that as metadata; do not make their live class names aliases.

## Guardrails against complexity

Reject these approaches:

- **Mirrored counters.** Do not keep `ProjectCard<Player>` quantities synchronized with physical
  hand backs. The physical backs are the count.
- **Add then remove.** Card movement is one transmutation, not two `THEN` stages with an observable
  duplicate or gap between them.
- **Invariant-driven inference.** A pure gain does not discover its source by asking which removal
  would make an invariant pass.
- **Mutable off-timeline RNG.** It breaks rollback, replay, and search forks.
- **Owned means private.** Ownership assigns rules and context; visibility is separate.
- **Output-only scrubbing.** Exact count and metric queries would still leak secrets.
- **A universal CardBack `HAS` trick.** Printed metadata and live components remain distinct.
- **Per-card custom draw code.** Chance narrows one general movement; card definitions describe
  their physical reveal/keep/discard workflow.
- **Eager deck-order components.** Add explicit order only when a selected supported rule requires
  persistent relative order.
- **Copy identity by default.** Multiplicity remains the simpler model until rules distinguish
  equal copies.
- **A chance Actor.** Choosing an outcome is not performing the resulting state change. The
  original Actor remains the Actor on the `ChangeEvent`; chance provenance is separate.

## Implementation gates

Proceed in small, reversible slices.

### Gate 1: prove the types and invariant

- Load the location/card declarations in a synthetic class table.
- Prove that `OwnedCardLocation` forms the intended intersection and every location dependency
  target satisfies the singleton rule.
- Prove linked location transmutations cannot change the represented face.
- Prototype the union-count limiter restriction, including overlap and multiplicity.
- Prove the printed-tag class-literal refinement against card metadata.

Do not touch canonical card data if any of these require global type-system exceptions.

### Gate 2: represent and move a tiny deck without randomness

- Assemble three exact project backs in `Deck` under a test-only premise.
- Move an explicitly named one through Deck, Revealed, Hand, play, Events, and Hand again.
- Verify tags/effects exist only on the live front and `CardFront OR CardBack` remains constant.
- Add the bare gain/removal lowering and atomized multi-card behavior, using a deterministic test
  selector.

### Gate 3: make chance a timeline citizen

- Specify the cross-platform random algorithm and canonical candidate ordering.
- Record chance outcomes atomically, preserve preparation's transaction contract, and make
  rollback/replay/forks deterministic.
- Return exact outcomes as sealed internal state until a valid commit-and-reveal boundary.
- Cover empty decks, reshuffles, filtered domains, and several draws in one operation.

Do not expose real-card mode while the seed or chance cursor lives outside rollback.

### Gate 4: build the observation boundary

- Project components before all query evaluation.
- Project task and event history.
- Couple authoritative commit, rollback-floor advancement, knowledge-horizon advancement, and
  publication in that order.
- Keep a player's own speculative draws and speculative public reveals sealed.
- Test the complete visibility matrix with adversarial exact queries and custom metrics.
- Ensure engine rules and chance retain the master reader while clients receive only the scoped
  projection.

Do not call the mode hidden-information-safe before this gate.

### Gate 5: assemble a real setup and migrate rule families

- Create the selected physical deck only after replacements and setup requirements resolve.
- Deal corporations, preludes, research cards, and optional draft packets by location moves.
- Add real counterparts to `F` definitions one physical-operation family at a time.
- Keep follow mode green throughout and leave it the default until a complete real setup can play
  through end-of-game event scoring.

## Acceptance properties

At minimum, integration/property coverage must establish:

1. Every selected unique face has total multiplicity one across front and backs; configured
   duplicate faces respect their physical supply.
2. Every ordinary movement preserves the represented class and changes only location.
3. Playing and finishing an event preserve physical identity and total card count.
4. `N ProjectCard` performs N sequential without-replacement selections, not N copies of one face.
5. Equal seeds and histories produce equal draws on JVM and JavaScript; different seeds provide
   useful variation.
6. Rollback and retry reproduce an observed draw; replay uses the recorded result.
7. A printed-tag filter selects only qualifying definitions and does not depend on live tag
   components.
8. Discarding from hand is the owning player's choice and moves the selected exact card.
9. Own private locations retain identity, hidden locations retain only counts, and all public
   locations retain identity.
10. Exact `count`, `has`, refinement, union, custom-metric, task, and history queries cannot recover
    another player's hand or the deck.
11. The master rules engine can still validate and execute operations using exact hidden state.
12. Real and follow definitions for one printed card cannot both be active.
13. Multiple Beginner Corporation backs can coexist without invented per-copy classes.
14. Before commit, a player's own new draw and every new public reveal remain abstract in all
    player-facing component, task, result, and history views.
15. Committing atomically disables earlier rollback before publishing newly entitled identities;
    a speculative fork cannot advance the live session's knowledge horizon.
16. A reveal-dependent choice appears only after its reveal is committed, so no later dead end
    asks a player to forget an observed card.

## Decisions to settle during the gates

These are deliberately unresolved because code or supported rules must answer them:

- final names for the two draft locations;
- whether one global `Revealed` location is sufficient once real reveal workflows are tested;
- the exact transaction shape for a chance decision made while preparing a task;
- the exact relationship among the world's rollback floor, private fork floors, and per-session
  published knowledge horizons;
- which real `F` counterparts search-and-shuffle versus reveal-and-discard;
- whether any selected supported rule requires persistent deck order; and
- whether the shared `UntrackedCardFront` follow-mode representation stays invisible enough to
  avoid mode-selected card declarations.

None changes the central model: exact inactive card plus location, exact live front, atomic moves,
timeline-owned chance, and player-relative observation.
