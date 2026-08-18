# `EACH` fanout

**Status: proposal.** Nothing in this document is implemented. The historical filename reflects
the Player-focused discussion that exposed the need; the proposed construct is generic enough to
fan out over `ResourceCard` and other component Types.

## Goal

Pets sometimes expresses one ambient instruction for several components by giving every matching
component an identical listener. Examples include every Player receiving starting components,
every Player being scored by Vermin, and every Player receiving a Sponsored Academies draw. Turmoil
also has Global Events that apply separately to Resource Cards.

The proposed spelling is:

```pets
EACH Type { InstructionTree }
```

`EACH` is a **fanout**, not a loop. It takes one World snapshot, finds every matching component
occurrence, and produces one sibling instruction tree for each. That membership remains fixed as
the sibling tasks execute; ordinary later task preparation still reads the then-current World.
Fanout has no iteration order, index, first or last branch, accumulator, or short-circuiting.
Implementation traversal order must not become authored precedence.

`EACH Player` selects seated Players. `EACH Owner` intentionally also selects a `SoloOpponent`.
`EACH ResourceCard` selects matching cards rather than their owners.

## Proposed binding model

Each selected component supplies a lexical specialization environment for its branch:

- the repeated selector expression denotes the selected concrete component;
- `Owner` is the selected component's owner, or the selected component itself when it is an Owner;
- bare owned expressions use that contextual `Owner`; and
- `This` continues to denote the surrounding Effect component. Losing the outer `This` would make
  ordinary card-local expressions unusable.

Thus the intended source is:

```pets
CorporationPhase:: EACH Player { CorporationCard }
```

not `CorporationCard<Player>`. A Resource Card fanout could refer to its selected card by repeating
`ResourceCard` in the body.

This is a lexical Owner binding, not a claim that the selected component carries the Effect or that
execution has entered gameplay as its owner. Fanout does not by itself change the surrounding
Actor, assignee, or controller. Instruction-side `BY` retains its existing Actor-only meaning.

Whether a queued fanout may produce abstract work is unresolved. Assigning each abstract branch to
the selected Player would turn fanout into delegation; leaving every branch with the surrounding
assignee may be useful for controller-owned choices but cannot replace Player-owned Research work.
Until this is decided, cases whose Player must narrow the task should retain meaningful owned
listeners.

## Sequencing

Fanout branches are ordinary siblings. These compositions have local meanings:

```pets
A THEN EACH Player { B }     // completing A produces the B siblings
EACH Player { A THEN B }     // each branch has its own ordinary continuation
Trigger:: EACH Player { A }  // valid only when every A is choice-free and fully determined
```

There is no proposed meaning in which `EACH Player { A } THEN B` waits for every branch or every
transitive consequence to drain. Ordinary `THEN` waits for one task, not descendants. A genuine
fanout-wide join would require the distinct completion-scope or barrier design discussed in
[SEQUENCING.md](SEQUENCING.md); it must not arrive accidentally with `EACH`.

An automatic triggering Effect already provides the automatic form of fanout. Do not add a second
`EACH`-specific double-colon until a case requires semantics that the outer `::` cannot express.

## Candidate conversions

| Current behavior | Candidate fanout | Context verdict |
| --- | --- | --- |
| Starting TR and corporation cards on `Player` | `EACH Player { 20 TerraformRating }`; `EACH Player { CorporationCard }` | Pure recipient fanout |
| `PreludeSetup<Player>` singleton listeners | `EACH Player { 2 PreludeCard }` | Pure recipient fanout |
| Award tallying through every `Player` | `EACH Player { AwardTally<This> / EVAL This.metric }` | Pure scoring fanout |
| Sponsored Academies' `AllDraw` and Player watchers | `-ProjectCard THEN (3 ProjectCard, EACH !Owner { ProjectCard })` | Opponent fanout; complement domain unresolved |
| Mons Insurance setup watchers | `EACH Player { MAX 0 CardXC05: PROD[-2] BY Owner }` | Must name Player so solo opponent is excluded |
| Vermin's end-game Player watchers | `EACH Player { -VictoryPoint / CityTile }` | Pure scoring fanout |
| Kotlin `ColoniesSetup` fleet loop | `EACH Player { ReserveTradeFleet }` | Pure setup fanout |
| Turmoil Global Events affecting Resource Cards | `EACH ResourceCard { ... }` | Confirms the selector cannot be Player-specific |

The exact surrounding triggers and gates remain part of each rule. The table demonstrates candidate
fanout bodies, not complete replacement declarations.

## Cases that should retain listeners

- Research purchases, final-greenery choices, and other work the selected Player must narrow, at
  least until assignment/delegation is designed explicitly.
- `StartToken` reactions: the unique token honestly identifies the relevant Player and owns the
  ambient rule.
- Splice and TR-marker reactions: these find the owner of one triggering component rather than
  fanning out to every Player.
- Colony trade bonuses and Productive Outpost: each Colony identifies eligibility and recipient,
  and some bonuses contain owner-specific choices.
- Production, Energy conversion, TR/card/tile scoring, generational cleanup, and ordinary card
  passives: these are genuine behavior of actual state occurrences.

## Open questions

1. For `EACH !Owner { ProjectCard }`, should the selector candidates be statically intersected with
   the body's `ProjectCard<Player>` ownership bound, excluding `SoloOpponent`? Static Type
   compatibility may filter branches; runtime executability and limits must not.
2. May queued fanout branches remain abstract on the surrounding assignee's queue, or should the
   first implementation require every branch to prepare without delegation?
3. How exactly does the selector become a Type variable in the body, especially for a composite
   selector such as `ResourceCard<Class<CardResource>>`, without reviving broad Class-name
   substitution?
4. Does multiplicity produce one branch per component occurrence when several occurrences have the
   same exact Type? The fanout model says yes, producing indistinguishable repeated branches without
   exposing a synthetic occurrence identity or position.
5. Is an outer automatic Effect sufficient for every required choice-free fanout, or is there a
   concrete rule needing a narrower automatic mode?

## Implementation plan

1. Settle the selector/complement domain and abstract-branch questions above.
2. Add parser and rendering tests for one simple selector, one complement, and one composite
   selector without implementing execution.
3. Add synthetic engine tests proving snapshot fanout, contextual Owner specialization, preserved
   outer `This`, inherited Actor/assignee, multiplicity, and reorderable siblings.
4. Prove the three sequencing shapes above and a negative test showing that fanout adds no global
   join.
5. Prototype one automatic Player conversion and one Resource Card conversion. Review the resulting
   conceptual cost before migrating more Canon data.
6. Only after the model survives those cases, remove redundant watcher Classes or custom code and
   retain end-to-end behavior tests.
