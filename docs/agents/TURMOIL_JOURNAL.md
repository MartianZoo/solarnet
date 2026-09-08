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

## 2026-09-08 — Stage 4: influence snapshots and political victory points

Added an explicit `MeasureInfluence<Player>` Signal and three generational, owner-qualified
results. The Chairman supplies one `ChairmanInfluence`; a player who leads the Dominant party
receives one `PartyLeaderInfluence`; and every owned delegate in that party attempts the same
AMAP `DelegateInfluence` gain. The concrete influence limit consolidates any number of those
delegate attempts into the printed single point for having one or more delegates. Because the
leader role supplements rather than replaces `PartyDelegate`, that physical marker correctly
contributes both delegate presence and the additional leader point.

The measurement is a snapshot request, not a continuously maintained score. This matches Global
Events, which measure each player while resolving and then add that result after the event's
printed maximum-five calculation. The generational lifetime gives later event effects a stable
fact during that calculation and lets the existing `Generation` cleanup remove it. A second
snapshot for the same player in one generation is intentionally outside the rule; mandatory
Chairman and leader contributions are not weakened to AMAP merely to tolerate such a call.

The same political role components now provide their printed end scoring: each Chairman and each
party leader grants its owner one VictoryPoint. A functional scenario establishes a player with
two dominant-party delegates, the party leadership, and the Chair, while another has one delegate.
Outward measurement produces influence 3 and 1 respectively, proving both source addition and the
multi-delegate cap. Transitioning to End produces the expected 22 versus 20 VictoryPoints.

During this slice, reusable `RefillLobby` and `MeasureInfluence` Signals were activated through
`Class<...>` module requirements rather than by gaining the Signals during module bootstrap. This
keeps activation distinct from execution and removes a meaningless early firing. The focused
Turmoil class, complete JVM suite, and `spotlessCheck` pass.

VALUES and minimality review: all behavior remains in the components that provide it, using only
existing Signal, Generational, effect, and AMAP rules. No Kotlin, event-specific accumulator, or
general arithmetic was added. Defect review found no issue in influence or scoring. The earlier
removal-maintenance problem remains open by design: current state does not expose active-player
rotation or clockwise party distance, and arbitrary Class iteration would not be a legitimate
substitute for either printed tie rule.

## 2026-09-08 — Stage 5: non-leader influence correction

The post-commit source reread found that Stage 4's delegate source was too broad. The rulebook says
the third influence comes from owning one or more **non-leader** delegates in the Dominant party.
A party leader's marker counts as a delegate for party totals, but does not by itself satisfy that
third source. The prior effect listened to every `PartyDelegate` and therefore gave a sole leader
both the leader point and the non-leader point.

Corrected the gate to require at least one `PartyDelegate - PartyLeader` for the measured owner and
party. The existing saturating metric subtraction captures the physical statement directly: a
leader with one marker has zero non-leaders, a leader with two markers has one, and an owner who is
not leader subtracts zero. The capped `DelegateInfluence` effect remains AMAP because multiple
eligible physical delegates still provide only one point.

The original influence scenario continues to prove that a leader with a second delegate receives
all three possible influence. The first-lobbying scenario now measures its sole party leader and
proves that it receives leader influence 1, delegate influence 0, and total influence 1. The
focused Turmoil class, complete JVM suite, and `spotlessCheck` pass.

VALUES and minimality review: the correction replaces an overbroad condition with one existing
metric expression. It adds no representation, Class, Kotlin, or instruction mechanism. Defect
review found no further mismatch in the three influence sources after checking the rulebook's
examples and the FAQ cap clarification. Political-cycle work resumes next.

## 2026-09-08 — Stage 6: ruling policies

Implemented all six Policy tiles as components that exist only while their party is ruling during
the Action phase. Each concrete Party supplies its matching Policy when `ActionPhase` begins;
the `Policy` family removes itself when that phase ends. The global `HAS MAX 1 Policy` invariant
keeps the active tile singular and makes ruling status the sole source of policy selection.

The passive tiles own their reactions. `MarsFirstPolicy` grants one Steel when a Player places any
tile on Mars. `UnityPolicy` gives every Player one additional titanium `ResourceValue` while live
and removes those values with itself. `GreensPolicy` grants 4 M€ when a Player places greenery.
`RedsPolicy` captures the number of player-attributed TR steps and requires 3 M€ per step; an
unpayable automatic consequence rolls the entire attempted TR gain back, which directly enforces
the printed prohibition against raising TR with less than the required money.

The other two tiles are themselves ordinary `StandardAction` providers.
`ScientistsPolicy` charges 10 M€, draws three ProjectCards, and records a generational per-player
use marker. `KelvinistsPolicy` charges 10 M€ and raises both Heat and Energy production. Passive
Policies do not implement `StandardAction`, so the ordinary turn menu has no unusable Turmoil
action under those governments. This is smaller and more direct than the draft's permanent
`UsePartyActionSA` router.

Functional tests enter Action phases under each government. They place real Mars tiles and
greenery, exercise both policy actions through `stdAction`, verify Scientists' second use rolls
back, observe Unity's fourth titanium payment value, and prove that a two-step Reds gain charges
6 M€ while an underfunded gain changes neither TR nor money. Policy removal is checked on entry to
Production. The focused policy suite, complete JVM suite, and `spotlessCheck` pass.

VALUES and minimality review: the behavior lives on the active tiles that create it. No instruction
properties, evaluator, custom action, or engine change was needed; six finite physical tiles and
one Unity-specific value component are the only new vocabulary. Code review found no inactive
doorway, setup bonus, stale Unity value, or multi-step Reds undercharge. The next slice will apply
the separate one-time ruling bonuses during new-government formation.

## 2026-09-08 — Stage 7: ruling bonuses

Added one typed `ApplyRulingBonus<Party>` Signal for the discrete new-government step. Each concrete
Party owns its printed response: Mars First counts Building tags, Scientists counts Science tags,
Unity counts the complete Planetary-tag family, Greens counts the complete Bio-tag family, and
Kelvinists counts Heat production. All five rewards iterate over every Player, including players
whose matching count is zero, and derive the exact M€ amount from existing component metrics.

Reds has the two official forms. In multiplayer, candidates rank by the total TerraformRating owned
by every other player. Since that metric is total rating minus the candidate's own rating, its
greatest value selects the lowest rating without introducing a guessed numeric ceiling or an
inverse-ranking mechanism. Competition ranking preserves friendly ties, so every tied lowest player
gains one TR. In solo, ranking is irrelevant and the player gains one TR only while at 20 or less.
Separate gates on `SoloMode` make the two forms mutually exclusive.

Functional tests invoke the typed government request through the normal administrative API. A
small test-only `TagHolder` supplies real Building, Science, Earth, Jovian, Plant, Microbe, and
Animal components; the assertions prove both abstract tag-family aggregation and that players with
zero matching tags receive no M€. Further scenarios cover Heat production, a unique multiplayer
low, a friendly multiplayer tie, and the FAQ's solo 20-TR threshold. The focused suite, complete JVM
suite, and `spotlessCheck` pass.

VALUES and minimality review: the one Signal is a reusable domain event that later government
formation can invoke; it does not store a second ruling identity or route six instruction-valued
properties. All permanent behavior remains Pets, and the engine and Kotlin production code are
unchanged. Code review found no setup-time bonus, optional-expansion tag omission, incorrect Reds
tie, solo leakage, or bonus given to only one player. The next slice will form a government around
this request, including the ruling change, delegate returns, Chair succession, TR award, dominance
recalculation, and Lobby refill in printed order.
