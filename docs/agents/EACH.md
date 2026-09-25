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
EACH @Selector { InstructionTree }
EACH Name@Selector { InstructionTree }
```

`EACH` takes one snapshot of the current World, finds every existing component matching `Selector`,
and resolves one independent sibling instruction branch for each match. It is a fanout, not a loop:
branches have no index, accumulator, short-circuiting, or authored order. Encounter order comes
directly from the component graph and is not a contract. If nothing matches, the result is `Ok`.

Enumeration uses `ComponentGraph.getAll`, so it ranges over component occurrences that exist when
the fanout is selected, not all possible Types. A queued fanout is one task until selected; only then
do its branches become sibling tasks. This timing matters when components are created during setup
and when tests or clients explicitly select tasks. Indistinguishable occurrences of the same exact
Type each contribute a branch; those branches have equal text but remain independent work.

## Selector and body scope

The selector can explicitly mark each selected concrete Type for use in its body. Repeating an
unmarked selector Type in the body is independent:

```pets
EACH Player { Plant }                         // each selected Player gains a Plant
EACH Player(HAS StartToken) { ChooseOceanArea } // only the start Player gets the request
EACH @Player(HAS StartToken) { AdminOceanPlacement<@Player> }
```

The marked selection is bound before nested `THEN` or full `FROM` scopes are resolved, so a matching
marker anywhere in that body—including on both sides of a transmutation—continues to mean the
selected concrete Type. A bare marked reference retains the selector's dependency arguments during
elaboration, but the selector refinement only filters candidates; it is not copied onto the
reference exposed to the body.

A Class selector can instead mark its represented Class. This permits a structurally present Class
representative to create one component of the Class it represents:

```pets
EACH Class<@MarsArea> { @MarsArea }
```

The selector's main expression still reads the enclosing context. For example,
`EACH ProjectCard<Owner> { ... }` selects cards belonging to the enclosing owner, while
`EACH ProjectCard<Anyone> { ... }` can select cards belonging to any owner. Its refinement instead
describes each candidate: dependencies omitted there remain available for candidate specialization.
Thus `Player(HAS StartToken)` tests each concrete Player for their own StartToken without requiring
`StartToken<Anyone>`. A nested `RANK` establishes its own candidate context instead.

Selector refinements decide participation using requirement semantics:

```pets
EACH Player(NOT Owner) { PROD[-2 MC] BY Owner }
```

An unmet gate inside the body fails normally; it does not omit that branch. The body need not name
the selected component: the selector may exist only to determine how many branches are produced.
`EACH` rejects a concrete selector, nested fanouts, and an empty body. A `NOT` refinement may filter
its selector like any other refinement.

Class-property syntax in the body remains inert while the enclosing Class effect is prepared. Once
the fanout snapshot is selected, each branch binds its selected component and, for an Owner
selection, contextual `Owner`, then evaluates its class properties independently. Property syntax
in the selector instead belongs to the enclosing context; award ranking expands the funded Award's
metric there. A marker on a `RANK` selector likewise exposes a candidate only through its marked
reference:

```pets
EACH Player(HAS =1 (RANK Player { EVAL Award.metric })) { FirstPlace<Award> }
```

## Ownership and attribution

Inside the body, `Owner` means the selected component only when it is an `Owner`. A non-Owner
selection leaves the enclosing contextual owner unchanged; it does not implicitly expose the owner
of an `Owned` component. `This` continues to mean the surrounding effect-bearing component.

The selected owner does not automatically become the actor, controller, or assignee. Every branch
inherits attribution and task control from the surrounding effect. Use `BY Owner` when the selected
owner must receive attribution. A fanout can produce independently narrowed choices for one
surrounding controller, as Colonial Envoys does. It cannot express “each player makes their own
choice”; such work must remain on an owned component that gives the existing task-routing machinery
the correct player context.

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

A branch corresponds to a component occurrence, even though occurrences of one concrete Type are
otherwise indistinguishable. Multiplicity repeats the branch; it does not scale the body. This is
observable whenever the body remains abstract: two identical colonies in
`EACH Colony<Owner> { PartyDelegate }` produce two delegate choices that may be narrowed
independently, not one instruction to place two delegates in the same party.

The selected expression still records only the occurrence's concrete Type. Selector substitution,
property evaluation, and ownership therefore behave identically in equal branches; independence is
represented by their separate positions in the resulting instruction group.

## Choosing the mechanism

Use `EACH` when one component owns a one-time rule that acts independently on the components present
at that moment. Prefer a Class effect when each recipient owns the rule, especially when
each player must make a choice. Prefer a persistent listener when the reaction must remain installed
throughout the game. A fanout triggered before its intended recipients exist silently does nothing.
During staged bootstrap, however, Premise-created Modules and all seated Players exist before queued
self-effect tasks execute, so a queued initializer can reliably fan out over that seed layer. An
immediate initializer cannot; outside that special staging, `SetupPhase` remains the earliest
general host for fanout over all seated Players.

English rendering currently declines fanouts as `UNSUPPORTED_FANOUT`. Per-player task routing is
unsupported; it is separate from per-branch class-property evaluation and is not implied by `EACH`.

## Implementation and tests

- [`Instruction.kt`](../../src/common/dev/martianzoo/pets/ast/Instruction.kt) — syntax and static
  restrictions (`class Each` and `_each`).
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — snapshot enumeration,
  refinement filtering, specialization, and branch creation (`resolveEach`).
- [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt) — Owner-selection body
  binding and contextual-owner shielding (`selectionSuppliesOwner`).
- [`InstructionResolutionTest.kt`](../../test/common/dev/martianzoo/engine/InstructionResolutionTest.kt)
  — runtime semantics (`testFanout`).
- [`Lang02InstructionsTest.kt`](../../test/common/dev/martianzoo/pets/Lang02InstructionsTest.kt) —
  syntax and static restrictions.
- [`Prelude2CardsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/Prelude2CardsTest.kt) —
  independently chosen Colonial Envoys for equal Colony occurrences.
