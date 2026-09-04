# `EACH` fanout

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** designing or implementing the proposed `EACH Type { ... }` fanout, or deciding
> whether a rule that applies to several components should be a fanout, a listener, or an ordinary
> Class effect.
>
> **Skip when:** changing quantified gain/removal, task delegation, trigger scaling, or a single
> card's listener. Those are separate mechanisms.
>
> **Status:** proposal. Nothing described here as "the construct" is in the tree. A complete
> prototype was built, converted real Canon call sites, passed the whole JVM suite, and was then
> set aside; this document records what it settled, what it cost, and what it left open, so the
> decision does not have to be re-derived. The construct is generic despite the historical
> filename.

## What is and is not in the tree

Only one piece of the prototype was kept, because it stands on its own and does not depend on
fanout at all: `TrWatcher` (in both bundles) and `SpliceTacticalGenomicsWatcher` are now unowned
`System` singletons instead of one instance per Player. See "Persistent per-player subscriptions".

Everything else — the `EACH` keyword, `Instruction.Each`, snapshot resolution, body shielding, the
Canon conversions, the `Player` rule moves — is proposal only. The prototype survives as a stash
commit, `979f4ad8d`, recoverable with `git stash apply 979f4ad8d` (a bare SHA, so it does not
depend on the stack position; the object is subject to eventual GC).

## Why the construct would exist

A rule that acts on several components at one moment currently needs a permanent component to live
in. Pets can say "this component has this rule", so expressing "every player loses 2 M€ production"
means inventing an `Owned<Player>, System` class with `HAS =1 This`, hanging the rule on it, and
letting the engine instantiate one per player. Nine such declarations existed when this was
prototyped (`TrWatcher` twice, once per bundle, because a bundle cannot add an effect to `Player`).
Their names all end in `Watcher` or `Setup` because there is nothing else to call a component whose
only job is to be an address.

`EACH` would remove the need for that address. The rule would stay on the component that is the
reason it exists — `MonsInsurance` says what Mons Insurance does — and name its recipients where it
acts.

This would **not** subsume every one of those classes, and the ones it leaves behind are the useful
finding. See "Deliberately not converted".

## Implementation entry points

- [`Instruction.kt`](../../src/common/dev/martianzoo/pets/ast/Instruction.kt) — the sealed
  instruction model gains one subclass; the `Parsers` object gains one keyword.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search for
  `resolveTree` for where snapshot enumeration and branch specialization would go.
- [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt) — search for
  `replaceOwnerWith` call sites for the contextual-`Owner` binding a fanout body must opt out of.
- [`Transforming.kt`](../../src/common/dev/martianzoo/pets/Transforming.kt) — search for
  `replaceOwnerWith` for the transformer itself.
- [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt) — search for
  `headerVariableBindings` and its `ANYONE` exclusion, next to which the selector exclusion goes.
- [Promo `cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets) — search
  for `MonsInsuranceWatcher` for the reference conversion candidate.

## The construct

```pets
EACH Selector { InstructionTree }
```

`EACH` is a **fanout**, not a loop. Resolution takes one World snapshot, finds the components that
**currently exist** and match the selector, and produces one independent sibling branch for each.
Branches carry no order, no index, no first or last, no accumulator, and no short-circuiting; the
prototype emitted them in a stable sort purely so output is reproducible, and that order must never
become authored precedence.

It enumerates real components through `ComponentGraph.getAll`, never the space of possible Types, so
`EACH Adjacency<OwnedTile<Owner>, Tile> { ... }` sees the adjacencies on the board and nothing else.
The one place this is lossy is genuinely fungible components: Pets has no component identity, so ten
indistinguishable `ProjectCard<Hand<Player1>>` components are one exact Type and yield one branch.
Every real client selects something that is distinguishable anyway — players, resource cards,
adjacencies, class literals. There is no multiplication rule, because no rule needs one. (Trigger
scaling — gaining 3 animals as one event and paying out once per animal — is an unrelated mechanism
and would be untouched.)

A queued fanout is **one task** that expands into branches only when it is selected. That extra
layer is invisible under ordinary autoexecution but shows up wherever task order is driven
explicitly: it is why the prototype left `RecessionWatcher` alone, since `Prelude2CardsTest` pins
the exact per-victim ordering of Mons compensation and a fanout would insert a step before those
tasks exist.

Selecting nothing is `Ok`, not an error — which is also the sharpest edge here. Because a fanout is
a snapshot, the trigger has to be late enough that the components exist, and a too-early one
produces nothing at all rather than failing. Both global-parameter class literals and Players are
invisible at module-gain time and visible at `SetupPhase`. A body containing `EVAL` should be
rejected outright for the same family of reason: see "Rules that would move off `Player`".

### The selector is a fresh variable

A fanout selector declares its own variable and is never a use of an enclosing Class variable, even
when spelled the same way. Without that rule, `EACH Player` inside a card class — which already has
a visible `Player` variable through `Owned<Player>` — silently bound the selector to the card owner
and collapsed the fanout to one wrong branch. The prototype hit this.

The fix is one line in `Class.headerVariableBindings`, next to the existing exclusion for `Anyone`:
selector expressions are skipped when body occurrences are matched to header variables. That is
sufficient on its own. Binding only ever replaces expressions recorded as occurrences, so a selector
that was never recognized is never substituted; an earlier draft of this document claimed otherwise
and required authors to write `Player<>`, which is not necessary.

Resolving a selector that is already concrete should still be rejected with a message naming this
cause, so if the exclusion ever fails to apply the mistake reports itself instead of producing a
wrong fanout.

### The selection owns the body

Inside the braces, `Owner` would be the selection's owner — the selection itself when it is an
`Owner`, and otherwise whichever `Owner` it belongs to. That one rule covers both shapes:

```pets
This: 48 MC, PROD[4 MC], EACH Player(HAS MAX 0 This<Anyone>) { PROD[-2 MC] BY Owner }
End IF 10 Animal<This>: EACH CityTile<Player> { -VictoryPoint }
```

The second is the interesting one: each city tile costs *its own owner* a point, which is what the
card actually says, and no player is named anywhere. Without the second half of the rule this is
impossible — the body could only reach the enclosing card's owner — and it is what makes the
construct useful for anything other than players.

`This` would still mean the surrounding Effect component, unchanged. The enclosing owner is
deliberately **unreachable** from inside the braces, and `This` offers no way to project it back
out; a construct where `Owner` meant one player and a bare owned expression meant another would be
unreadable. When a rule needs both identities, the enclosing half goes outside the braces — usually
the more honest statement anyway. Sponsored Academies is the case: rather than "everyone except me
draws 1", it draws one fewer card in its own instruction and lets the fanout include the owner.

Mechanically the body is *shielded*: `Transforming.replaceOwnerWith` takes a predicate, and the
`Transformers` helper that wraps it uses that predicate to skip a fanout body, so ordinary
contextual-Owner binding leaves `Owner` alone and only the fanout's own branch specialization
supplies it. `fixEffectForUnownedContext` must skip shielded bodies too, so an unowned Module
hosting a fanout does not acquire a spurious `BY Owner` trigger. This replaced an earlier prototype
scheme that rewrote `Owner` to the selector's name before contextual binding; that scheme needed a
transformer paired with `insertDefaults` in three separate chains, only worked for Owner-typed
selectors, and made `EACH Owner { ... }` a silent no-op.

#### What is still unsatisfying

`BY Owner` above means the *selected* player, and nothing in the syntax says so. Shielding makes the
rule total — inside `EACH`, `Owner` is the selection's owner, no exceptions — but it does not make
the call site self-explanatory. An explicit `EACH Player AS Owner { ... }` binder was considered and
rejected as too much new grammar for what it buys.

### The selector reads its enclosing context; only the body is shielded

`Owner` is shielded inside the braces but not in the selector, so a selector still names components
the way any other expression would: `EACH ProjectCard<Owner> { ... }` selects the enclosing owner's
cards, not everyone's. Getting this wrong was silent in the prototype — shielding the whole node
made that selector match every player's cards.

Two consequences follow. `EACH Owner { ... }` is a one-branch fanout and should be rejected; `EACH
Anyone` is how you fan out over every owner including `SoloOpponent`, which is exactly the role
[TYPES.md](TYPES.md#contextual-and-other-binders) gives `Anyone`. And a body occurrence spelled like
the selector belongs to the fanout, not to an enclosing Class variable of the same name — so
`CLASS Foo : Owned<Player> { ... EACH Player { MC<Player> } }` credits each selected player rather
than `Foo`'s owner. That shadowing is scoped to the selector's own name; other Class variables in a
body still bind normally.

`EACH` may not contain another `EACH`. Nesting would make `Owner` and both selector names ambiguous,
no rule needs it, and banning it keeps exactly one selection in scope.

### Attribution is inherited, and it matters

A branch has no actor of its own: it inherits the surrounding effect's, which is normally the
effect-bearing component's owner. Three behaviors pin this down because getting it wrong is
invisible in the original state change and visible only in what it triggers. All three are covered
in the tree through an observable reaction or interaction:

- **Vermin** (`BugsTest`) — under a fanout, every `VictoryPoint` removal would be taken by Vermin's
  owner, whichever player lost the point. Today each victim's own `VerminWatcher<Player>` acts
  instead. A synthetic card records the credited Player through `BY PlayerN` reactions. **In the
  tree** as a characterization of that wrong behavior, so converting the card means flipping an
  existing assertion rather than writing a new one.
- **Mons Insurance** (`MonsInsuranceTest`) — the opposite, and the reason a converted body would say
  `BY Owner`: each opponent is credited for its *own* production loss. Crediting the Mons owner would
  make the card compensate its own victims, so the existing full-money expectation after setup
  distinguishes the attribution without a probe.
- **Recession** (`Prelude2CardsTest`) — the attacker is credited, which is what makes Mons Insurance
  compensate those victims. The existing ordering test settles each loss `BY Player1` and observes
  the resulting Mons payments, protecting the attribution if `RecessionWatcher` is ever converted.

### A selector refinement chooses which components take part

Participation should be decided by the selector, using ordinary refinement semantics:

```pets
This: 48 MC, PROD[4 MC], EACH Player(HAS MAX 0 This<Anyone>) { PROD[-2 MC] BY Owner }
```

`GroundType.formRequirement` specializes every expression in a refinement with the candidate — the
same rule that makes `LandArea(HAS Neighbor<CityTile<Anyone>>)` mean "this area's neighbors". So
`This<Anyone>` reads as "this card, owned by anyone", the candidate specializes it, and the whole
refinement means "each player who does not have this card". It stays correct even if two players
could somehow hold copies of the same corporation.

Write the ownership in a selector refinement as `<Anyone>`, not bare. A bare owned expression picks
up `Owner` from defaults, which is bound to the *enclosing* component's owner before the candidate
can specialize it, and the refinement then rejects every candidate.

An earlier draft instead let an unmet gate *inside the body* silently drop that branch. That was
inconsistent with how gates behave everywhere else and was abandoned: a gate in a fanout body should
fail exactly like any other gate. `X OR Ok` is not a substitute — it resolves to a live `Or` and
would offer the player a pointless choice — so if a filter ever cannot be written as a refinement,
the thing to invent is an optional-instruction syntax such as `(...)?`, not a fanout-local
exception.

This also settles the previous edition's only open question. `!Owner` was rejected: complements are
meaningful only where an upper bound is already known, so a selector-position complement would make
sense for an each-player feature and not for a generic one, and the working direction in
[TYPES.md](TYPES.md#7-complement-bounds) is to delete complements rather than extend them.

### Sequencing

Branches would be ordinary siblings, so the existing rules apply unchanged:

```pets
A THEN EACH Player { B }              // completing A produces the B siblings
EACH Player { A THEN B }              // one continuation in each branch
Trigger:: EACH Player { A }           // executes inline, like any automatic effect
```

`EACH Player { A } THEN B` does **not** wait for every branch or every transitive consequence.
Plain `THEN` waits for one task. A genuine fanout-wide join would need the scoped-completion design
in [SEQUENCING.md](SEQUENCING.md#selected-direction-scoped-completion), and must not arrive
accidentally with `EACH`.

An automatic triggering Effect already provides the automatic form of fanout; there is no
`EACH`-specific double-colon.

Resolution turns a fanout into an `InstructionGroup`. Enqueued, that group becomes separately
selectable sibling tasks through the existing `replace1WithN` path; reached during automatic or
inline execution, `Instructor.doExecute` has to run the members in place rather than rejecting the
group as abstract. **That is the only behavior change the prototype made to non-fanout code**, and
it is the only sensible meaning available: automatic effects execute inline by definition. It is
also the reason none of the engine changes were worth keeping on their own — without a construct
that produces a group there, the edit is unreachable and loosens an assertion for nothing.

`EACH` would therefore add no atomicity guarantee, and remove none. Whether one fanout is one game
event or several is the same unanswered question that
[SEQUENCING.md](SEQUENCING.md#research-on-file) already records for Mons Insurance, and this
construct neither settles nor worsens it.

## Deliberately out of scope

**Delegation.** A branch inherits the surrounding task's actor, controller, and assignee. Selecting
a player does not hand that player the choice. This is not an oversight and not free to add:
`LiveEffect.onChange` derives `contextualOwner`, `defaultActor` and `taskController` once per fired
effect, and every sibling in the resulting tree inherits them, so per-branch narrowers would be a
new concept rather than a reuse of the existing routing. Judged low value for the cost: every
conversion the prototype attempted is choice-free, and the cases that do need a player to narrow
already have a meaningful owned component to route through.

The consequence is a real limit worth stating: a rule such as "every player discards a card of their
choice" could not be a fanout, and neither could most Turmoil global events, whose printed
instructions resolve per player and several of which offer per-player choices. If Turmoil is
implemented, that is the case that will force this decision, and the shape to try first is a
per-branch contextual Actor derived from an Owner-typed selection — not a new delegation vocabulary.

**English rendering.** The prototype had the renderer decline a fanout with a new
`UNSUPPORTED_FANOUT` refusal, the same way it declines alternatives and sequences it cannot phrase;
two cards moved into the refusal report as a result. Rendering "each player…" is ordinary
[LANGUAGE.md](LANGUAGE.md) work and is not blocked by anything here.

## What the prototype converted

None of this is in the tree. It is recorded because it is the evidence that the construct works on
real Canon, and it is the list to re-apply if the proposal is picked up.

| Would be removed | Replacement | Effect |
| --- | --- | --- |
| `MonsInsuranceWatcher` | `EACH Player(HAS MAX 0 This<Anyone>) { PROD[-2 MC] BY Owner }` on the card | Rule moves onto Mons Insurance; solo mode stops producing a branch at all |
| `VerminWatcher` | `End IF 10 Animal<This>: EACH CityTile<Player> { -VictoryPoint }` on the card | Rule moves onto Vermin, says "this card's animals" instead of any Vermin's, and drops the per-player metric for what the card literally says |
| `SponsoredAcademiesWatcher` and its `Signal {}` | `-ProjectCard THEN (2 ProjectCard, EACH Player { ProjectCard })` | Both the watcher and the signal class disappear; the owner absorbs one draw |
| `PreludeSetup` | `PreludePhase:: EACH Player { 2 PreludeCard }` on `PreludeExpansion` | Setup rule moves onto the module that owns it |
| `ColoniesSetup` Kotlin `CustomClass` | one unconditional `SetupPhase: EACH Player { TradeFleet }` | A custom instruction removed, as [REDUCE_CUSTOM.md](REDUCE_CUSTOM.md) anticipates; `SoloColoniesSetup` shrinks to its discard and the `IF MAX 0 SoloColoniesSetup` gate goes away |
| Two hand-enumerated `GpIncomplete<Class<...>>` lists | `SetupPhase:: EACH Class<GlobalParameter> { GpIncomplete<Class<GlobalParameter>> }` | A non-player fanout; Venus no longer restates the base rule for its own parameter |

That is four `System` classes, one Kotlin custom class, and two hand-enumerated lists gone. The
whole JVM suite passed; the only fixture updates were the generated English snapshot and refusal
report. The prototype's notes claimed the refusal report had already drifted from its generator, but
re-running `EnglishTest` with `--rerun-tasks` at the current HEAD leaves both files clean, so that
drift belonged to the prototype's own conversions, not to the tree.

## Rules that would move off `Player`

`Player` carries rules that belong to whatever grants them. Two would move:

| On `Player` today | Would move to | Why there |
| --- | --- | --- |
| `SetupPhase:: 20 TerraformRating<This>` | `TerraformingMars` | Starting TR is a base-rules fact, not something a player does to itself |
| `SetupPhase IF QuickStartVariant: PROD[...]` | `QuickStartVariant` | The variant is the reason the rule exists, and the move deletes the gate |

Four would stay, each for a different reason:

- `CorporationPhase:: CorporationCard` and the two `ResearchPhase` rules stay because the per-Player
  Class effect is what routes each player's own choice to that player. A fanout branch inherits one
  actor, so converting these would collect every player's card choices onto one queue.
- `This:: GrossHack<This> BY Engine` and `This:: 2 BaseResourceValue<Class<Steel>>, 3
  BaseResourceValue<Class<Titanium>>` stay because they must hold from the moment a `Player` exists,
  and `Player`'s own `This` trigger is the only hook that early. Bootstrap (`Initializer`) gains
  Modules before Players, so a Module-hosted `This:: EACH Player { ... }` fans out over an empty
  snapshot, and a Module-hosted `Player::` trigger does not fire during bootstrap either — both
  measured, both silently produce nothing. (The prototype also moved the resource values from
  `SetupPhase` to player creation, which is strictly earlier; that move is independent of `EACH`
  but was not separated out, so it is not in the tree either.)
- `MeasureAward<Award>:: AwardTally<Award> / EVAL Award.metric` stays because of a real limitation:
  **a class property is evaluated once, before any fanout, with the enclosing contextual owner.**
  `LiveEffect.onChange` calls `evaluateProperties` with one owner for the whole effect, so moving
  this rule onto `MeasureAward` gave every player one player's award score. The prototype's answer
  was to reject a property inside a fanout body outright, turning the footgun into a parse error;
  lifting that rejection means evaluating properties per branch, which is the one engine change this
  proposal genuinely needs and does not have.

The general lesson is that a fanout is a snapshot, so its trigger has to be late enough that the
components exist. `SetupPhase` is the earliest trigger at which every Player is visible; nothing
that must be true before then can be a fanout.

## Where else this would apply

Fanning out over something other than players is real but uncommon, and the survey matters more than
the count.

**Turmoil** is the largest future client, and has exactly four fanouts in
[TURMOIL.md](TURMOIL.md) — two over players (`EACH Player { TurmoilPlayer, LobbyDelegate }`,
`EACH Player { MeasureInfluence }`) and two over cards, from Sponsored Projects and Cloud Societies.
Both card shapes were checked against today's Canon rather than assumed:
`EACH ResourceCard<Anyone> { CardResource<ResourceCard<Anyone>>. }` produces one branch per resource
card across all players and auto-narrows to each card's own resource kind, and a card with no
resources drops out on its own. Note `<Anyone>`: fanning out over *owned* non-player components
needs the ownership written explicitly, or the owner default captures the selector.

**Checked and rejected:**

- `GpGameEndBarrier` looks like the `GpIncomplete` enumeration but is not "every global parameter" —
  Venus is a barrier only under `MandatoryVenusVariant`. It stays enumerated.
- `AssignAwardPlaces` and `AssignMultiplayerVictory` need a maximum across players. Fanout offers no
  comparison, so they remain custom, as [REDUCE_CUSTOM.md](REDUCE_CUSTOM.md) already concluded. Only
  the second half of each ("give first place to every winner") is fanout-shaped.
- `CreateMapAreas` and `HandleCardTags` create components from card and map metadata, not from
  components that exist. A snapshot fanout has nothing to enumerate.
- `TileInLargestGroup` is a graph computation, not an iteration.

**The alternative to check first.** Pets already has a non-fanout way to act on every X: create a
`Signal` and let each X react through its own Class effect. Awards use it (`Award { End:
MeasureAward<This> }`) and so do colony tracks (`ColonyTile { AdvanceColonyTracks: ... }`). That is
the better shape when the reacting Class is the reason the rule exists, because the rule lives there.
Prefer `EACH` when the rule belongs to the acting component instead — a card's one-off effect on
everyone — or when the reacting Class is in another bundle and cannot be edited. Sponsored Academies
has a Signal purely for the second reason.

## Deliberately not converted

This is the part worth keeping. The nine classes that look alike are three different problems.

**Persistent per-player subscriptions** — `TrWatcher` (in two bundles) and
`SpliceTacticalGenomicsWatcher`. These install a listener that must stay live all game, not do one
job once. A fanout instruction cannot express them and should not try. `RecessionWatcher` is a
one-shot and *would* convert cleanly, but is left alone for the task-layer reason above.

They do not, however, need to be per-player, and **this part is in the tree**: two of them have been
collapsed to a single unowned `System` singleton. An unowned class effect already receives a
contextual `Owner` from the changed component's player (`effectOwner ?: changedComponentPlayer ?:
(triggerEvent.actor as? Player)` in `LiveEffect`, the documented `GlobalParameter` rule in
[IDENTITY.md](IDENTITY.md#implicit-trigger-owner)):

- `TrWatcher` is now plain `TerraformRating:: HasRaisedTr.` on an unowned class. Its old
  `IF MAX 0 SetupPhase` gate existed only because `Initializer.createSingletons` creates every
  `HAS =1 This` class at bootstrap, which is before `Player`'s `SetupPhase:: 20 TerraformRating`
  fires — so the watcher had to ignore its own arming window. Stating that as a lifetime instead
  (`HAS MAX 1 This`, created by `CorporationPhase:: TrWatcher.` on the hosting Module) lets the
  effect say only what it means. That is the established idiom: `CorporationPhase: Photosynthesis`
  arms greenery-driven oxygen the same way. `PristarTest` gained a discriminating case — an
  opponent raising TR must not suppress your Pristar bonus — because the existing single-player
  tests would have passed either way, and `Does not pay its production bonus after a TR increase`
  was confirmed load-bearing by checking that it fails when nothing creates the watcher.
- `SpliceTacticalGenomicsWatcher` is now simply unowned. An earlier draft added a `BY Player`
  trigger binder, which was removed: dumping the transformed class effect shows it changes nothing
  but itself. `insertDefaults` already produces
  `MicrobeTag<Owner, CardFront<Owner>> ...: Microbe<CardFront<Owner>>. OR 2 MC<Owner>!` either way,
  and the runtime binds that `Owner` from the changed tag's card. `BY Player` is only an
  Actor-domain filter on the triggering event, and no test distinguishes it. Triggering off an
  Owned component type is what routes the effect, exactly as expected.

Both simplifications also dropped now-unneeded spelling: neither `TrWatcher`'s trigger nor Splice's
needs `<Anyone>` or repeated `Player` arguments once the class is unowned.

**One-shot task holders** — `SoloColoniesSetup`. Even under the proposal, `SetupPhase:
-ColonyTileSelection THEN EACH Player<> { TradeFleet } THEN -This!` still needs a component, because
`-This!` is how the sequence retires itself. That is a completion idiom, not a fanout.

**Rules that were already clean** — an earlier edition of this document listed `Player`'s starting
TR, corporation cards, and award tallying as conversion candidates while also stating that fanout
implies no delegation; those two claims could not both hold. The resolution is in "Rules that would
move off `Player`" above: the choice-free ones would move to the modules that grant them, and the
ones that route a player's own choice stay exactly where they are.

**Also excluded for the same reasons as before:** research purchases and final-greenery choices,
`StartToken` reactions, TR-marker reactions, colony trade bonuses and Productive Outpost, and
production, energy conversion, scoring, generational cleanup, and card passives.

## Cost, as measured

The prototype was +421 / −82 lines across 31 source and test files, including the watcher collapse
that has since landed separately. The permanent conceptual additions are one `Instruction` subclass,
one keyword, one resolution case, a shielding predicate on an existing transformer, one refusal
reason, and one exclusion in Class-variable recognition. Against that: four Canon classes, one
Kotlin custom class, one `Signal` class, one custom-class registration, and two hand-enumerated
lists would be gone, and six rules would move onto the components that own them.

## Open questions

1. Is `BY Owner` inside a fanout readable enough? The rule is total, but the call site still does not
   say which owner it means. See "What is still unsatisfying" above.
2. Should the file be renamed (`FANOUT.md`)? The construct has been generic since the previous
   edition and the name still says otherwise.
3. Is the `GpIncomplete` conversion worth keeping? It works, but it moves creation from module gain
   to `SetupPhase` in core setup for a fairly small gain.

## If this is picked up

1. Recover the prototype (`git stash apply 979f4ad8d`) rather than rebuilding it; it is complete and
   was green.
2. The watcher-collapse half is already done and must not be re-applied.
3. Evaluate class properties per branch, which unblocks the award tally and is the one engine gap.
4. Render fanout in English rather than refusing it.
5. Leave delegation alone until Turmoil produces a rule that needs it.
