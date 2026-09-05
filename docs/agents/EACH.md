# `EACH` fanout

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** authoring or changing an `EACH Type { ... }` instruction, or deciding whether a
> rule applying to several components should use fanout.
>
> **Status:** current implementation. Source and tests win if they differ from this description.

## Contract

```pets
EACH Selector { InstructionTree }
```

`EACH` takes one snapshot of the current World, finds every existing component matching `Selector`,
and resolves one independent sibling instruction branch for each match. It is a fanout, not a loop:
branches have no index, accumulator, short-circuiting, or authored order. The stable implementation
order exists only for reproducible output. If nothing matches, the result is `Ok`.

Enumeration uses `ComponentGraph.getAll`, so it ranges over components that exist when the fanout is
selected, not all possible Types. A queued fanout is one task until selected; only then do its
branches become sibling tasks. This timing matters when components are created during setup and when
tests or clients explicitly select tasks.

Pets identifies components by exact Type rather than occurrence. Several fungible components with
the same exact Type therefore produce one branch, not one branch per copy. Current uses select
distinguishable components such as players, owned cards, adjacencies, or Class literals.

## Selector and body scope

The selector declares a fresh variable for its body. It is not a use of an enclosing Class variable
with the same spelling. Each matching concrete Type replaces occurrences of that selector in the
body:

```pets
EACH Player { Plant }                  // each selected Player gains a Plant
EACH CityTile<Player> { -VictoryPoint } // each selected city's owner loses a point
```

The selector itself still reads the enclosing context. For example,
`EACH ProjectCard<Owner> { ... }` selects cards belonging to the enclosing owner, while
`EACH ProjectCard<Anyone> { ... }` can select cards belonging to any owner. Within a refinement,
write ownership intended for candidate specialization explicitly as `<Anyone>`; a bare owned
expression may first acquire the enclosing contextual owner.

Selector refinements decide participation using ordinary requirement semantics:

```pets
EACH Player(HAS MAX 0 This<Anyone>) { PROD[-2 MC] BY Owner }
```

An unmet gate inside the body fails normally; it does not omit that branch. `EACH` rejects a
concrete selector, a selector unused by its body, complements, nested fanouts, an empty body, and a
body containing `EVAL`. Class properties are currently evaluated once before fanout, so permitting
`EVAL` would give every branch the enclosing context's value.

## Ownership and attribution

Inside the body, `Owner` means the selected component when it is an `Owner`, otherwise the owner of
the selected component. `This` continues to mean the surrounding effect-bearing component. The
enclosing owner is intentionally unavailable inside the body; put work concerning that owner
outside the fanout.

The selected owner does not automatically become the actor, controller, or assignee. Every branch
inherits attribution and task control from the surrounding effect. Use `BY Owner` when the selected
owner must receive attribution. Consequently, `EACH` is suitable for choice-free work but cannot
express “each player makes their own choice.” Such work must remain on an owned component that gives
the existing task-routing machinery the correct player context.

## Sequencing

Branches are ordinary siblings, so existing sequencing rules apply:

```pets
A THEN EACH Player { B }   // completing A produces the B siblings
EACH Player { A THEN B }   // each branch has its own continuation
Trigger:: EACH Player { A } // automatic branches execute inline
```

`EACH Player { A } THEN B` does not wait for every branch or its transitive consequences. `THEN`
waits for one task, and `EACH` provides no fanout-wide join or additional atomicity guarantee.

## Occurrence versus Type

**Audit.** A fanout branch corresponds to a distinct Type, never to a copy. This is the boundary
that decides whether a rule can use `EACH` at all, and it is easy to miss because a fanout over
components that happen to be unique looks like a fanout over occurrences.

Terraforming Mars' Productive Outpost pays one colony bonus per colony owned. A player may hold two
`Colony<Player1, Luna>` components, and the card must pay twice. No fanout can express that:

```pets
EACH Colony<Owner, ColonyTile> { ... }   // one branch for two identical colonies
```

The rule therefore lives on the `Colony` class, where the effect fires once per component:

```pets
CLASS Colony<ColonyTile> : Owned<Player> {
  FinishTrade<Anyone, ColonyTile> OR GainColonyBonuses:: GainColonyBonus<ColonyTile>
}
```

**Declined: fanning out over the tile instead.** `EACH ColonyTile(HAS Colony<Owner>) {
GainColonyBonus<ColonyTile> }` type-checks, binds correctly, and passes the suite, because the
selection is exactly what the body needs and `selectionOwnsBody` leaves the enclosing owner
available for an unowned selector. It is still wrong: it pays once per tile, so a player with two
colonies on one tile is underpaid. It also drops the card's English rendering to
`UNSUPPORTED_FANOUT`. Do not propose it again.

**Declined: selector destructuring.** Binding a selector's nested variables — `EACH Colony<Owner,
ColonyTile>` binding `ColonyTile` to `Luna` — would not have helped, because the branch count is
wrong before any binding happens. Independently, class-scoped dependency variables already
destructure in the direction that keeps the call site ignorant of the selected class's shape.

**Declined: scaling the body by multiplicity.** Emitting `body * n` for a component present `n`
times conflates `n` instructions with one `n`-sized instruction. They differ whenever the body is
abstract: two colonies on Titan owe two separate `Floater` requests, placeable on two different
cards, not one `2 Floater`. If `EACH` ever honors multiplicity it must emit `n` sibling branches,
which is the shape it already produces. No current selector would benefit — `Player`,
`Class<GlobalParameter>`, `CityTile<Player>`, and `Class` literals are unique per Type — so this
remains unbuilt.

The engine makes the same conflation today, ahead of any fanout: a trigger matching `n` components
specializes its instruction as `instruction * n` (`Hit.specialize`), so `2 Colony<Titan>` produces
one `6 Floater` task rather than two `3 Floater` tasks. `BugsTest` characterizes the current
behavior.

## Choosing the mechanism

Use `EACH` when one component owns a one-time rule that acts independently on the components present
at that moment. Prefer an ordinary Class effect when each recipient owns the rule, especially when
each player must make a choice. Prefer a persistent listener when the reaction must remain installed
throughout the game. A fanout triggered before its intended recipients exist silently does nothing,
so `SetupPhase` is the earliest reliable host for fanout over all players created during bootstrap.

English rendering currently declines fanouts as `UNSUPPORTED_FANOUT`. Per-branch class-property
evaluation and per-player task routing are also unsupported; they are separate features, not implied
by `EACH`.

## Implementation and proofs

- [`Instruction.kt`](../../src/common/dev/martianzoo/pets/ast/Instruction.kt) — syntax and static
  restrictions (`class Each` and `_each`).
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — snapshot enumeration,
  refinement filtering, specialization, and branch creation (`resolveEach`).
- [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt) — selected-owner body
  binding and contextual-owner shielding (`bindEachSelectorAsOwner`).
- [`InstructionResolutionTest.kt`](../../test/common/dev/martianzoo/engine/InstructionResolutionTest.kt)
  — runtime semantics (`testFanout`).
- [`InstructionTest.kt`](../../test/common/dev/martianzoo/tfm/pets/ast/InstructionTest.kt) — parsing
  and rejected forms (`fanout`).
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt) — trigger multiplicity as
  instruction scaling (`Hit.specialize`).
- [`ProductiveOutpostTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/colonies/ProductiveOutpostTest.kt)
  — per-colony rather than per-tile payout.
- [`BugsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt) — merged bonuses for
  two colonies on one tile.
