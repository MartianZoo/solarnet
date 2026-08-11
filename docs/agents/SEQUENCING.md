# Sequencing

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

This document states Solarnet's default task-ordering rule, chooses among the mechanisms that can
express genuine precedence, and audits the Terraforming Mars rules currently known to require one
thing to happen before another.

The main external references are the [base rulebook](https://fryxgames.se/wp-content/uploads/2023/04/TMRULESFINAL.pdf),
the [Colonies rulebook](https://fryxgames.se/wp-content/uploads/2023/07/TM_COLONIES_ENG_RULESi.pdf),
and Jeffrey Anchan's source-linked [Comprehensive FAQ v1.7](https://tesera.ru/images/items/2078855/FAQ_v1.7.pdf).
The FAQ is a compilation, not itself the game's authority; follow its cited designer and rulebook
sources when a conclusion is disputed. Local evidence includes [ENGINE.md](ENGINE.md),
[WORKFLOW.md](WORKFLOW.md), [game-insights.md](../game-insights.md), canonical Pets and JSON, and
behavioral tests. The unanswered BGG thread [Which instructions are atomic?](https://boardgamegeek.com/thread/2626062/which-instructions-are-atomic)
supplies useful working assumptions about atomicity, but no publisher or designer confirmed them.

## Default: a pool, not a queue

Pending tasks normally form an unordered pool. The acting player may resolve a card's direct
effects, card-dependent effects, and already-triggered consequences in any order. Stable task ids
and auto-execution order are implementation conveniences, never game precedence.

The fundamental exception is causality: a consequence cannot precede the event that triggers it.
Placing an ocean must happen before Arctic Algae can award plants; increasing production must
happen before Manutech can award the matching resource. Once a consequence has been triggered and
joined the pool, it normally has no priority over other work.

Tests should therefore prove only real precedence. They should also preserve important freedom by
showing that sibling effects can be interleaved when the rules permit it.

## Mechanism priority

### 1. Prefer an A-triggered B

If every A should cause B—including A usages introduced later—encode one `A: B` rule. The component
carrying that effect may be A itself, or a third component C that listens for A. For this document
those are the same sequencing mechanism; choose according to what should know about what.

`GlobalParameter` itself owns `This: TerraformRating`, because every parameter increase always
tries to award TR. World Government Terraforming uses the same rule; it simply has no eligible
Player recipient. Conversely, the separate `Photosynthesis` component owns
`GreeneryTile: OxygenStep`, because oxygen production is a switchable rule that must be absent
during setup and final greeneries rather than an intrinsic fact about every greenery.

Use an `IF` trigger when state can distinguish the A events that cause B. A normal `:` effect queues
B. A `::` effect is appropriate only for a choice-free consequence or hidden bookkeeping that must
be incorporated immediately; it must not hide a player decision or impose arbitrary priority on
otherwise equal tasks.

### 2. Otherwise consider source-local `A THEN B`

Use this only when some authored instruction creates the exceptional A that needs B, the distinction
cannot honestly be expressed by a trigger or `IF`, and that instruction conceptually owns both
stages. If every source of A needs `THEN B`, and a new A source would be expected to add it too, this
is the wrong mechanism: return to Part 1 and write the rule once.

An action legitimately owns its cost and payoff. Capital owns both its city choice and the marker
that identifies that city. Mining Rights owns its tile and the production linked to the selected
area. Do not teach an unrelated producer of A about B merely to obtain convenient ordering.

Operationally, `A THEN B` means: **as soon as A completes, enqueue B in its place**. B does not run
immediately and gains no priority over the rest of the pool. With `A1 THEN B1` and `A2 THEN B2`, the
order A1, A2, B1, B2 is possible. `THEN` also creates one type-variable region across the two stages,
which is often more important than temporal order. It waits for the A *task*, not for every
transitive consequence caused by A, so it cannot express “after A's whole delegated operation
drains.”

Mars University illustrates both the semantics and an open rules question. Solarnet currently lets
the two activations caused by Research discard twice before drawing twice. The Comprehensive FAQ
says that both Science tags trigger the effect and that the active player orders effects, but it
does not say that the two verbs inside one activation may be split. The printed “discard ... to
draw” reads more naturally as one optional operation. No publisher/designer ruling authorizing the
two-discards-first order was found; treat the current tests as evidence of engine behavior, not a
settled game rule. A real-cards model could readily make each discard/draw exchange atomic.

### 3. Otherwise use a barrier

A barrier is appropriate when distributed work must finish before some other instruction becomes
legal: card payment before the card enters play, all optional trade-track decisions before fleet
movement, or one delegated action before another begins. `Barrier` is nested under `Temporary`, so
every barrier is temporary without spelling both supertypes.

Prefer the narrowest meaningful gate, such as `MAX 0 TradeBarrier`, over `MAX 0 Barrier`. A broad
gate is correct only when *all* unfinished barriers should block the instruction. Barriers encode
legality, not queue priority: the player remains free to choose among every currently legal task.

Workflow precedence is separate. Phase topology and control-until-drain are not ordinary sibling
task ordering; [WORKFLOW.md](WORKFLOW.md) is authoritative for them.

### The mixed `::` / `:` trigger idiom

The engine executes every automatic (`::`) effect caused by a concrete change, recursively, before
it admits the queued (`:`) effects caused by that same change. Consequently, paired effects

```pets
A:: B
A: C
```

are an existing trick for making the choice-free state change B happen before C becomes pending.
This is not `B THEN C`: the two effects remain independently authored reactions to A, and B does
not own C. It is appropriate when B is synchronous structural maintenance or a prerequisite that
must be true for every queued reaction to A. It is a hack when the relationship exists only to
manipulate queue admission or when B represents a player decision.

These uses are considered principled and correct:

- Generated card tags use `This:: ...Tag!`, while printed immediate effects use `This: ...`. A
  player is not allowed to delay adding a played card's tags. The tags instantly determine the set
  of triggered effects, even though the player may delay resolving those effects.
- `Tile<This>:: CreateAdjacencies<This>` derives the hidden adjacency graph before area bonuses and
  tile/card reactions become tasks. This does not prioritize any of those queued consequences.
- `ProductionPhase:: Heat FROM This` removes old energy before TR income and production payouts
  become tasks, so newly produced energy survives.
- `PlayCard` automatically performs deck, requirement, and cost setup before its gated card-entry
  task. `PlayTag` and `Pay` similarly apply fixed payment facts before optional reactions.
- Pharmacy Union creates its exceptional starting money automatically. Its tags are also automatic,
  but their spending reactions are queued, so the two automatic effects need no relative order.

More generally, when a user action creates a `Hidden` component—or any bookkeeping component the
user does not regard as a game object—its creation should normally be automatic. The user should
not need to discover and execute engine-maintenance work. This usability rule supersedes the
sequencing question, provided the hidden change is choice-free and already fully determined.

This is an explicit but justified use of the idiom as an ordering trick:

- Trade Envoys and Trading Colony each use `Trade:: TradeBarrier` plus
  `Trade: ColonyProduction? THEN -TradeBarrier`. The decision cannot be automatic, while the gate
  must exist before fleet movement can become legal.

These mixed-mode families still need an audit:

- Setup, generation, and phase signals use `::` for configuration, bootstrap, and generational
  cleanup alongside queued choices and payouts. Do not depend on registration order among two
  automatic effects; if their relative order matters, make one trigger the other.
- The broad `MAX 0 Barrier` used by card play and some spend-enabled effects may be wider than the
  conceptual payment transaction.

Use the idiom more only for same-event, choice-free normalization or invisible bookkeeping. Event
cleanup needs to happen after descendant work, Head Start needs a completion scope, and candidate
card selection needs isolation; none is solved by changing a colon. Tests should prove both the
automatic prerequisite and the continued freedom to order queued siblings.

### Would an automatic `THEN` help?

The action-card marker exposes a real tension. Marking the card before `UseAction` prevents a
second use from starting, but makes Viron's own marker visible to Viron's target requirement; Canon
therefore needs the awkward `ActionUsedMarker<!CardVC5>` complement. Reversing the stages would
avoid that state during the action trigger, but an ordinary follow-up marker could be deferred and
allow another use to start.

An author-local “automatic `THEN`” could be useful for choice-free bookkeeping immediately after a
chosen stage, and might also shorten a few hidden-marker creation/cleanup sequences. Its semantics
must not be confused with “after all consequences of A”: that stronger operation is what Head Start
and event cleanup need. A simple inline tail would also not by itself remove Viron's complement,
because Viron's queued target is prepared against the later world in which the marker already
exists. The feature is therefore not yet justified as one general sequencing primitive; investigate
trigger-time linkage/frozen choice, forced continuation, and descendant-completion as separate
semantics before adding syntax.

## Audit by verdict

### These are considered principled and correct

- A matching change occurs before `Effector` adds its triggered effects to the pool. Global
  parameter steps therefore precede their TR, threshold bonuses, and completion effects; Manutech's
  `PROD[StandardResource]: StandardResource` likewise follows the production increase.
- Tile placement precedes derived adjacencies, area bonuses, ocean-adjacency money, Arctic Algae,
  Rover Construction, Tharsis Republic, and similar reactions. Once triggered, the visible
  consequences remain an unordered pool.
- Energy conversion, Pharmacy Union's starting money, generated card tags, and hidden adjacency
  creation are the justified automatic effects described above.
- Action arrows become `cost THEN instruction`, so payment precedes payoff. Ants, Predators, Sell
  Patents, and other ordinary actions need no additional special case.
- Direct “do A **to** do B” offers with settled encodings appear five times on four cards: Olympus
  Conference, Recyclon, Neptunian Power Consultants, and twice on St. Joseph of Cupertino Mission.
  Their `THEN`, atomic `FROM`, or payment/barrier encodings preserve the spend before its benefit.
- Dirigibles, Psychrophiles, Carbon Nanosystems, and Martian Lumber Corp are a different family.
  They consume a variable number of card resources while modifying the debt of an in-progress card
  payment; `THEN` principally carries X from the removal into the Owed reduction.
- Capital, Flooding, Mining Rights, and Mining Area use `THEN` principally to link the chosen city,
  player, or area into the follow-up. This shared identity is more important than task priority.
- Spend-enabled effects correctly establish `Owed` and `Accept` work before a barrier-gated payoff.
  Trade Envoys and Trading Colony correctly use one `TradeBarrier` per decision and work for both
  the standard action and Titan Floating Launch-Pad.

### These require no authored sequencing rule

- Mining Rights and Mining Area cannot produce from their unique tile before that tile exists.
  `THEN` still gives useful linkage and presentation, but legality already supplies the order.
- Immigrant City's city, production reductions, and ordinary `CityTile` production reaction join
  the pool. At -4 M€ production, the reduction is naturally illegal until the city reaction supplies
  1 production; otherwise the player may choose either order.
- Energy Tapping may increase the active player's production or decrease a target's first. Only
  when nobody currently has energy production is the decrease naturally unavailable until the
  increase occurs.
- Colony placement currently triggers both the track adjustment and placement bonus after creating
  the colony. This is probably model-equivalent because occupied track slots are not components;
  revisit it only if an effect can observe their relative order.

### These represent known bugs or missing rules

- **Pluto's individual colony bonus:** the publisher specifies draw one card, then discard one.
  Canon currently encodes `Ok`. It should be `ProjectCard THEN -ProjectCard`, not the reverse—which
  deadlocks an empty hand—and not two independent tasks. Independent tasks could expose a temporary
  empty hand to a hypothetical fan-card effect. Mars University avoids the empty-hand problem by
  making its entire discard-to-draw branch optional.
- **Event cleanup:** an event should remain in play through its immediate effect and tags, then move
  to the played-event pile. The current sibling cleanup lets Solar Probe disappear before counting
  its own science tag.
- **Head Start:** its first granted action and all descendants must finish before the second action.
  Two siblings are too weak, while plain `THEN` waits only for the `UseAction` signal task. This
  needs a barrier or workflow completion scope.
- **Ecology Experts / Splice:** this anomaly comes from the published rules themselves, not merely
  from Solarnet's payment protocol. Ecology Experts' bio tags must happen early enough for Splice
  income to pay for the selected card, yet the selected card must already be in play so its own
  effects can react to those same tags. The rules thus require the selected card to be both before
  and after the tags. No linear A-before-B encoding can express that simultaneous transaction; a
  provisional card-play/payment scope is likely required.

### These need a design or rules audit

- **Mars University:** this is the sixth direct spend-to-benefit offer, but as described under Part
  2, current incremental `THEN` permits both Research discards before either draw. The available
  official material establishes two activations and free ordering among effects, but not splitting
  one activation. Confirm or model each exchange atomically.
- **Card-action marker / Viron:** marker-first prevents reuse but forces Viron's complemented target;
  reversing it makes the marker deferrable. Settle the continuation semantics before changing it.
- **Candidate draw/select/play:** Valley Trust, Merger, and New Partner use ordinary hand components
  in incremental `THEN` chains. The chain neither forces prompt continuation nor identifies cards as
  this operation's candidates, so they can be counted or confused with pre-existing cards. Prefer a
  temporary candidate type or one scoped selection operation.
- **Card-play barrier scope:** `PlayCard` correctly establishes payment before card entry, but its
  `MAX 0 Barrier` may be broader than the conceptual payment transaction.
- **Trade settlement:** fleet movement correctly causes trade income and colony bonuses, which are
  intentionally unordered relative to each other. `ResetColonyProduction` is another sibling rather
  than structurally last; determine whether anything can observe an early reset.
- **Lifecycle mixed modes:** setup, generation, and phase triggers need confirmation that no rule
  depends on registration order among their automatic effects.

## Rules that deliberately do not impose task order

- A card's direct effects are normally resolvable in any order.
- Card-dependent reactions such as rebates, Mars University, Olympus Conference, and tag-resource
  effects may be resolved before, after, or between direct effects once triggered, subject to any
  order internal to one effect.
- Event-dependent effects become available only after their event, but then join the same pool.
- Trade income and individual colony bonuses are separate effects whose order is chosen by the
  active player. Pluto may therefore take trade-income cards before resolving a draw/discard colony
  bonus. Do not chain all colony bonuses after trade income, but do preserve draw-then-discard
  inside each Pluto bonus.
- Separate triggers of the same effect remain separate entries in the pool. Whether one Mars
  University activation may itself be split around another remains under audit.

Natural unavailability is preferable to an explicit ordering construct when it exactly matches the
rules. It keeps the task pool honest: A happens first only in worlds where B cannot yet be done.

## Procedural and phase precedence

These rules are real ordering constraints, but most belong to workflow rather than task mechanics:

1. Starting choices are committed before corporations and Preludes are revealed. Corporations are
   revealed and resolved in player order; starting cards are paid for before Prelude play. Each
   player then resolves their selected Preludes one at a time in player order.
2. Later generations advance the first-player marker and run Research before Action. Action ends
   only after every player passes; Production follows.
3. Within Production, convert existing energy to heat before producing new resources. Production
   payouts themselves are simultaneous for rules purposes.
4. After Production, the Solar phase checks game end before World Government Terraforming, Colony
   production, or Turmoil. If the game ends, skip those later Solar steps.
5. Colony production returns fleets and advances colony tracks after World Government Terraforming.
   Canon currently advances tracks during `ProductionPhase` and returns fleets at `Generation`; this
   is a documented approximation and a **known workflow gap**.
6. In solo play, test the configured victory condition before final greeneries; final greeneries
   cannot rescue a failed terraforming objective.
7. Final greeneries occur in the final generation's player order. One player completely exhausts
   their legal conversions—including resources gained from placements and triggers—before the next
   player begins. Scoring follows only after all final greeneries and their effects resolve.

Keep phase topology and delegated-turn completion in [WORKFLOW.md](WORKFLOW.md). This file should
record only the domain precedence that topology must satisfy.

## Sequencing is not immediacy

Sequencing says only that B cannot happen before A. `A THEN B` provides that guarantee by replacing
the completed A task with B; unrelated pending work may still happen between them. A barrier is
similarly non-immediate: it makes B illegal until A's required work is done without giving B the
next turn. Mars University's two activations therefore demonstrate both the value and limitation
of `THEN`: each draw is linked to its own discard, but Solarnet currently permits both discards
before either draw.

Immediacy is stronger: the change is not offered as player work and cannot be postponed behind
another task. That is what `::` expresses for tags and hidden bookkeeping. It should not be inferred
from `THEN`, a barrier, natural unavailability, or the fact that players normally move cubes without
pausing in a physical game.

## Immediacy is not atomicity

Atomicity is stronger again: one operation cannot be split, reordered internally, or interleaved
with observations of an intermediate state. An immediate `::` effect is not automatically atomic.
The engine splits its instruction into concrete changes, and each change may itself fire effects
before the next change. Conversely, `Timeline.atomic` provides rollback on failure; that
failure-atomicity alone does not make an operation indivisible under the game rules.

The unanswered BGG post proposes these as likely atomic units:

- Paying for and putting a card into play, including its tags and effects.
- Direct transfers and exchanges: steal/pay-to instructions, Recruitment, and Vote of No
  Confidence.
- One stated amount of a resource or production change, and probably one multi-card draw or
  discard.
- Fleet movement, trade income, and resetting that colony's track, while treating colony bonuses
  as separate triggered effects.
- Possibly each individual right-hand side of a triggered effect. That would make each Mars
  University or Pluto discard/draw, and each Olympus Conference or Recyclon reaction, one unit.

It proposes these as deliberately non-atomic:

- A multi-step global-parameter change: each step remains separately observable, so track bonuses
  cannot be skipped.
- The ocean placement and 6 M€ loss printed together on one Hellas space.
- Philares reactions to multiple new adjacencies: each adjacency triggers separately rather than
  tile placement creating one combined reaction.

The post's linked designer exchange adds an important qualification: even gaining 5 M€ or raising
temperature two steps was described as separate changes, while an effect phrased “if one or more”
still observes the original instruction as one effect. A player cannot repartition a 7 M€ loss
into seven effects to multiply Mons Insurance reactions. “Atomic” therefore risks conflating three
different guarantees: no player interleaving, visibility of intermediate component changes, and
the identity/multiplicity seen by triggers.

Use the post's list as a reasonable audit hypothesis, not an authority. For every claimed atomic
operation, decide and test all three guarantees separately. In particular, investigate triggered
right-hand sides, trade settlement, and card entry rather than assuming that immediate execution or
rollback already supplies game-rule atomicity.

## Audit discipline

For each newly discovered A-before-B rule:

1. Record the authoritative wording and the smallest observable counterexample.
2. Ask whether A can trigger B, either from A itself or from a component C that should know about
   the relationship, including an `IF` that distinguishes the legitimate A events.
3. If not, and only some authored A usages need B, ask whether each exceptional source conceptually
   owns both stages and should author `A THEN B`.
4. If neither is honest, introduce the narrowest barrier or workflow completion scope that makes B
   illegal until A's required work drains.
5. Add both a precedence test and, where relevant, a freedom test proving unrelated tasks may still
   be reordered.
6. Move the corresponding audit item to the correct verdict bucket.
