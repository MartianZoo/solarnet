# Turmoil implementation journal

This is an append-only record of the staged Turmoil implementation. Existing entries are never
rewritten; later discoveries and corrections are appended with their own evidence.

## 2026-09-08 — Stage 0: scope and evidence baseline

The `turmoil` branch was created directly from the clean `main` tip at `2da51569c`. The requested
stopping condition is the complete expansion described by `docs/agents/TURMOIL.md`, including the
base and solo political rules, all 31 base Global Events, and the five optional events associated
with Venus Next or Colonies. The existing independent `TurmoilCardPack` remains useful: it already
contains four supported cards and explicitly does not activate the political rules.

The local official rulebook is the normative physical-game description. The local FAQ v1.8 is a
test-design source and correction index. In particular, it supplies observable cases that the terse
event cards do not fully specify: dominance changes require a strict lead; a displaced party leader
does not lose a tie; influence is applied after the printed maximum-five cap; penalties are ignored
to the extent that resources or production are unavailable; Global Event clauses resolve left to
right; and several events have card-specific ownership, choice, or tie behavior. A disputed ruling
will still require the source standard stated in `AGENTS.md`.

The implementation will proceed in vertical slices instead of importing the aspirational Pets draft
all at once. The first slice will make Turmoil a selectable, independently testable Module and model
the stable setup vocabulary that current Pets already expresses. Subsequent slices will earn any
general language or engine change through a functional rule that needs it. Likely pressure points are
party maintenance with two distinct tie rules, instruction-valued immutable Class facts, exact
Solar-phase sequencing, and the predetermined Global Event queue. None is permission to add a
Turmoil-only Kotlin shortcut.

Verification begins with focused parser/type or player-level rule tests, expands to the affected JVM
suite at each commit, and ends with the repository-wide build. Each checkpoint also receives a
VALUES review, a minimality review, and a normal defect-oriented code review before it is committed.

## 2026-09-08 — Stage 1: selectable module and political setup

Added `TurmoilExpansion` as a normal convention-backed Bundle and Module. Selecting it now selects
the existing `TurmoilCardPack` by the same declarative default mechanism used by other related rule
and content modules; selecting the card pack alone still does not activate political rules.

The Module constructs the six singleton Party components and singleton Neutral owner during module
bootstrap. This is earlier than physical setup but carries no game choice or transient state: these
components are the persistent committee identities required by later typed dependencies. During
Setup, each Player receives one `TurmoilPlayer` participation component and one `LobbyDelegate`,
while the committee receives the neutral Chairman and Greens ruling status specified by the
rulebook. Constructing all six named parties directly also makes activation honest; a generic Class
fanout could only enumerate Party Classes already activated by some other constructive reference.

The functional setup test observes the resulting world through gameplay counts. Module-selection
and representative-composition tests cover the default card-pack relationship and composition with
Venus Next, Prelude 2, Colonies, a nonstandard map, and promos. The complete JVM suite and
`spotlessCheck` pass.

VALUES and minimality review: permanent behavior is entirely Pets. The only Kotlin production line
is the conventional Bundle registration, and no custom instruction or engine exception was added.
The six explicit Party gains are a finite physical-game fact and smaller than adding activation
machinery to make an otherwise inapplicable generic fanout work. Defect review found no unresolved
issue in this slice. The next slice will add normal and leader delegates plus paid/free lobbying,
then use those real transitions to determine how much of party maintenance current ranking and
effects can express without custom code.

## 2026-09-08 — Stage 2: delegate placement and strict incumbency on gains

Added the two printed lobbying choices to a normal `StandardAction`: a player may move the one
Lobby delegate to a selected Party for no money, or pay 5 M€ to place a delegate. A delegate gain
now derives both political roles needed immediately after placement. The party's first delegate
appoints its owner as leader; a challenger replaces that leader only with strictly more delegates.
The party with the most delegates becomes dominant, while the incumbent remains dominant on an
equal total. All of this behavior is expressed through existing selectors, ranking metrics, and
effects.

The leader marker is deliberately a role in addition to the owner's ordinary `PartyDelegate`, not
a second physical delegate. The ranking metric therefore counts every ordinary delegate once and
uses the leader marker only as its second, tie-breaking metric. A strict challenger briefly gains
the role; the new role's own effect removes the rank-two incumbent. This avoids an owner-changing
`FROM` expression, whose missing source owner would legitimately be completed from the target and
could therefore select the challenger rather than the incumbent. Dominance has no owner dependency,
so its corresponding party-to-party transfer remains one atomic `FROM` change.

The functional tests perform real standard actions and cover the FAQ's two distinct strict-lead
rules: equal party totals retain the dominant party, and equal personal totals retain a party
leader. They also cover the free Lobby move, the paid 5 M€ choice, first-leader appointment, and
both kinds of strict-lead replacement. The focused Turmoil class, complete JVM suite, and
`spotlessCheck` pass.

VALUES and minimality review: the slice adds no Kotlin and no custom instruction. The correction
after the same-party test removed an unreliable implicit dependency choice instead of teaching the
engine a political exception. Defect review leaves three intentionally unclaimed behaviors for the
next maintenance slices: the seven-delegate player supply, Lobby refresh during government
formation, and leader/dominance recomputation after removals. The next slice will model finite
delegate supply and removal-side maintenance before influence depends on either role.

## 2026-09-08 — Stage 3: finite delegate reserves and Lobby refill

Added the physical delegate reserves omitted by the initial modeling draft. Each player now starts
with the complete printed supply of seven markers: one `LobbyDelegate` and six
`ReserveDelegate`s. Neutral starts with fourteen markers as well: one Chairman and thirteen in its
reserve. Paid lobbying now transmutates a reserve marker into a party delegate, while the free
choice still transmutates the Lobby marker. Running out of reserve therefore makes the paid choice
impossible through ordinary source availability and never charges the 5 M€ cost.

This revises the earlier tentative analogy to unplaced board tiles. The local rulebook explicitly
keeps every delegate marker on the committee board, and the FAQ makes the supply observable in
several ways: each player has exactly seven, neutral placement is ignored when its fourteen are
exhausted, mandatory placements can make a card unplayable, and Lobby refill fails without a
reserve marker. A reserve component is one physical fact that composes with every one of those
rules; a numeric cap would duplicate the same state and could not identify the marker returned by
government formation.

Added `RefillLobby` as a small reusable Signal. It moves at most one reserve marker into each
player's vacant Lobby and leaves an occupied Lobby untouched. It is intentionally not subscribed
to `SolarPhase` yet: government formation must return ruling-party delegates before the refill,
and a broad phase subscription would not prove that order. A later sequencing slice will invoke
this Signal in its printed step 3f position.

The functional tests use standard-action doorways to spend all seven player markers, verify that a
seventh paid placement is rejected without payment, and observe reserve consumption by existing
lobbying scenarios. A separate outward task invokes Lobby refill and verifies both the vacant and
already-occupied results. Setup assertions cover player and neutral counts. The focused Turmoil
class, complete JVM suite, and `spotlessCheck` pass.

VALUES and minimality review: the implementation is Pets-only and represents the real pieces
rather than adding arithmetic, a custom supply service, or a Turmoil engine rule. Production cost
is one component Class, one source term on the paid action, setup counts, and one Signal. Defect
review found no issue inside this finite-supply slice. Removal-side political maintenance remains
next; it must honor its different active-player and clockwise tie rules rather than reuse the
gain-side incumbent metric by convenience.
