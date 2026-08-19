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

`EACH` is a **fanout**, not a loop. It takes one World snapshot and groups the matching components
by exact concrete Type. Each distinct Type produces one sibling instruction tree multiplied by that
Type's snapshot multiplicity. That Type set and each multiplier remain fixed as the sibling tasks
execute; ordinary later task preparation still reads the then-current World. Fanout has no
iteration order, index, first or last branch, accumulator, or short-circuiting. Implementation
traversal order must not become authored precedence.

`EACH Player` selects seated Players. `EACH Owner` intentionally also selects a `SoloOpponent`.
`EACH ResourceCard` selects matching cards rather than their owners.

## Proposed binding model

The selector and its repetitions in the body form an ordinary implicit-Type-variable region. Each
selected concrete Type narrows every linked occurrence in its branch:

- no local identifier is introduced;
- `Owner` and bare owned expressions retain their surrounding context;
- `This` continues to denote the surrounding Effect component; and
- the surrounding context component, context owner, assignee, Actor, and automatic Effect mode are
  unchanged.

Thus the intended source is:

```pets
CorporationPhase:: EACH Player { CorporationCard<Player> }
```

The repeated authored `Player` is the variable occurrence. `EACH Player { CorporationCard }` would
instead leave the bare owned card in its surrounding context: it could remain abstract or bind to
the Effect owner, and would not mean "a card for the selected Player." To prevent that omission from
silently becoming selector-independent repetition, an `EACH` body must contain at least one linked
occurrence of its selector. A Resource Card fanout similarly refers to the selected concrete card
Type by repeating its authored selector expression in the body.

This is Type-variable substitution, not execution as the selected component. Instruction-side `BY`
retains its existing Actor-only meaning.

Queued fanout branches inherit the surrounding assignee and may remain abstract there exactly like
other instructions. Selecting a Player does not assign that branch to the Player. Cases whose
selected Player must narrow the task should retain meaningful owned listeners until an explicit
delegation mechanism exists.

## Multiplicity

Fanout selects concrete Types, not component occurrences. If the snapshot contains three equal
`Animal<Player1, CardX75<Player1>>` components, `EACH Animal { -Animal }` produces one branch whose
selected exact Animal Type is removed three times. There are not three occurrence branches, and
Pets gains no synthetic component identity.

Multiplication uses the existing `InstructionTree.times` meaning. It therefore scales the whole
specialized body, rather than adding branch-to-branch ordering or repeatedly preparing against
changing Worlds. Types with multiplicity zero produce no branch. Seated Player Types normally each
have multiplicity one.

## Sequencing

Fanout branches are ordinary siblings. These compositions have local meanings:

```pets
A THEN EACH Player { B<Player> }              // completing A produces the B siblings
EACH Player { A<Player> THEN B<Player> }      // one ordinary continuation in each branch
Trigger:: EACH Player { A<Player> }           // inline only when every A is choice-free
```

There is no proposed meaning in which `EACH Player { A<Player> } THEN B` waits for every branch or
every transitive consequence to drain. Ordinary `THEN` waits for one task, not descendants. A
genuine fanout-wide join would require the distinct completion-scope or barrier design discussed
in [SEQUENCING.md](SEQUENCING.md); it must not arrive accidentally with `EACH`.

An automatic triggering Effect already provides the automatic form of fanout. Do not add a second
`EACH`-specific double-colon until a case requires semantics that the outer `::` cannot express.

## Candidate conversions

| Current behavior | Candidate fanout | Context verdict |
| --- | --- | --- |
| Starting TR and corporation cards on `Player` | `EACH Player { 20 TerraformRating<Player> }`; `EACH Player { CorporationCard<Player> }` | Pure recipient fanout |
| `PreludeSetup<Player>` singleton listeners | `EACH Player { 2 PreludeCard<Player> }` | Pure recipient fanout |
| Award tallying through every `Player` | `EACH Player { AwardTally<Player, This> / EVAL This.metric }` | Pure scoring fanout |
| Sponsored Academies' owner-local `Signal` and Player watchers | `-ProjectCard THEN (3 ProjectCard, EACH (Player except Owner) { ProjectCard<Player> })` | Schematic opponent fanout; difference-selector syntax unresolved |
| Mons Insurance setup watchers | `EACH Player { MAX 0 CardXC05<Player>: PROD[-2 Megacredit<Player>] BY Player }` | Must name Player so solo opponent is excluded |
| Vermin's end-game Player watchers | `EACH Player { -VictoryPoint<Player> / CityTile<Player> }` | Pure scoring fanout |
| Kotlin `ColoniesSetup` fleet loop | `EACH Player { ReserveTradeFleet<Player> }` | Pure setup fanout |
| Turmoil Global Events affecting Resource Cards | `EACH ResourceCard { ... ResourceCard ... }` | Confirms the selector cannot be Player-specific |

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

1. What syntax expresses a difference selector such as "every seated Player except Owner"?
   Existing Complement syntax cannot stand alone, and the body's compatibility must not silently
   determine selector membership or exclude `SoloOpponent`.

## Implementation plan

1. Settle the difference-selector syntax above.
2. Add parser and rendering tests for one simple selector, one difference selector, and one
   composite selector without implementing execution. Reject a body with no linked selector
   occurrence.
3. Add synthetic engine tests proving snapshot concrete-Type fanout, implicit selector/body
   linkage, preserved surrounding `Owner` and `This`, inherited Actor/assignee, multiplicity
   scaling, and reorderable siblings.
4. Prove the three sequencing shapes above and a negative test showing that fanout adds no global
   join.
5. Prototype one automatic Player conversion and one Resource Card conversion. Review the resulting
   conceptual cost before migrating more Canon data.
6. Only after the model survives those cases, remove redundant watcher Classes or custom code and
   retain end-to-end behavior tests.
