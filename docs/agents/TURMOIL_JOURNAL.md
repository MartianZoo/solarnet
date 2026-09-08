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
