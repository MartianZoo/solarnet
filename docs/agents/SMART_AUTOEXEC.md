# Safe and smart autoexecution

> **Agent information:** This note records the implemented Player policy, its Canon evidence, and
> the remaining research. Source and tests establish current behavior.
>
> **Read when:** changing Agent policy, judging whether a Player task can be claimed automatically,
> investigating task order, or changing Admin settlement.
>
> **Status (2026-10-21):** SAFE is available as an opt-in Player policy in the Agent API; the
> Playground enables it by default. Agents still default Players to EAGER. `TfmAutoExec` supplies
> the Canon register below to every Terraforming Mars Agent, including the Playground. `SafeTasks`
> lives in test sources as a development-time row proposer. Admin keeps
> the current Agent behavior; the owner's separate full-autoexecution direction remains open.

## The decision we are trying to make

A Player may have several tasks in the queue after initiating an action. A task is safe to do
eagerly when claiming it now cannot remove any post-action game state that the Player could have
reached by claiming that **same mandatory task later**. Compare continuations of the same
initiating action from the same position. All its ordinary queued consequences must finish; a
different turn or a separately initiated action cannot be inserted into the comparison.

This is directional. The task need not commute perfectly with every other task. Gaining Steel
early, for example, may leave an additional spending option available without forcing its use.
The question is whether doing the task early makes a later state unreachable. Compare complete
states, including components, dependencies, pending work, and any distinction later Canon play
can observe. A different event-log order alone is not a lost game state unless later rules use it.

A Player Agent must not resolve a genuine Player choice merely because an outcome seems better or
a queue order is convenient. It must not pick among card discards, payment splits, targets, OR
alternatives, or amounts unless a separate safe rule establishes that no choice is lost. The
owner has stipulated that card discards should remain manual. Explicit Player commands may, of
course, choose any legal option.

For now, the owner permits a narrower exception to the no-lost-outcome rule: perform concrete,
mandatory card draws early, even beside other card work, because the Playground can undo back
through the deal. This includes a `PER` draw whose count is resolved from the current World when
claimed. A future mode that forbids undo before a draw must disable this exception. Choosing cards
to discard or to move from Selecting remains manual.

The Player policy can use EAGER, SAFE, CONCRETE, or NONE at this stage. EAGER remains available
while SAFE is being evaluated; it can make genuine Player choices. SAFE uses the game's live
predicate when several tasks compete. CONCRETE considers an actor's sole selectable task, but
other actors' work can still prevent it from running; NONE takes nothing automatically.

Terraforming Mars SAFE uses the Canon register below when several ordinary tasks compete. Its
shared loop also claims the only ordinary task in the World when its instruction is concrete and
the claim finishes without Player input. The register is checked again after every task. Card
departures that ask which named card leaves Hand or Selecting remain manual in the Playground.
When two or three ordinary Player tasks remain and the first is concrete, the Playground also
tries the short queue through `DecisionSearch`. It takes that first task only if every searched
continuation finishes in the same complete state within 100 search steps. The search applies
the SAFE predicate after every step, so it does not try orders that postpone an eagerly safe task.
An unlisted choice, failed path, different outcome, or exhausted limit leaves the tasks for manual
selection. Giant Solar Collector's Energy production and Venus step exercise this case.
The four play phases are also where the Playground's checked autoexec option filters visible
choices through a speculative completion search. The search uses the end of the current
`ActionScope` as its witness,
or `PassBaton` when the player declines an action before a scope opens. It never asks for an
action-completion witness during setup, research, production, or solar work. Only an exhaustive
search that establishes no completion path hides a choice. Exhausting the node budget or reaching
an unsupported branch leaves the choice visible and reports that the search could not decide.
The same applies when a numeric choice has more options than the search lists: failure at a smaller
amount does not prove every larger amount fails.
Each Playground search for a prompt or card shortcut gets 2,000 steps and 500 milliseconds shared
across its verdict calls, including steps spent listing unfinished form choices. Once either limit
is reached, the remaining choices stay visible as undecided. The browser lists numeric choices and
alternatives that open a numeric choice directly from the TaskForm, without completion searches or
uncertainty marks. It leaves those choices to the Player instead of making a provisional amount
selection on the browser thread. The Playground also checks a one-second limit between steps of an
automatic settlement walk, because each step can create a fresh completion search; remaining tasks
stay available for manual selection. A single engine step still runs synchronously, so this limit
cannot interrupt it.
The Playground also saves completed live positions to browser storage. After a stalled click, a
reload and RECOVER GAME reconstruct the named cards and deck positions and open with Player
autoexec off. This resumes at the last saved position; it does not interrupt work in the old tab.
Moving search off the browser thread remains open.
With the checkbox off, hand-card shortcuts require a held card and an offered PlayProject; printed
card-action shortcuts check their local form choices. Neither runs a completion search. A clicked
choice may still fail during actual play and be undone or changed manually.
The Playground passes the exact unfinished TaskForm instruction into speculation; the search follows
its remaining choices before committing the form. Rebuilding it from rendered text can lose an open
choice such as TradeAction's payment action.
Speculation never names cards: the World models unplayed cards as anonymous backs, and card
tracking records names only for real moves. A narrowing that names a card face offers the faces
the Player holds when the search begins; a face drawn during the explored line is unknowable, so
such a line is undecided rather than dead. Merger is exempt by name, because its rules let a
Player start it without proving the turn can finish.

## Owner direction (2026-10-14)

- **D1. Admin is greedy and fixed.** Admin executes its work in the order received. A task that
  fails is retried later in the same request. A full pass over the queue that executes nothing is
  an error. Whether Admin's Agent and the Players' Agents are the same kind of object is open.
- **D2. Policies compose.** Autoexecution is not an enum. An Agent attaches any set of small
  policies, in any order, from a library of specialized policies.
- **D3. Autoexecution is walking what pruning leaves.** Pruning removes edges that lead only to dead
  ends, and collapses edges whose outcomes are functionally identical. A next step that pruning
  leaves as the only edge is safe to take. An automatic pick among several live edges with possibly
  different outcomes is itself pruning, and usually poor pruning.
- **D4. Pruning is expensive** in this game. Expect complicated analysis.
- **D5. Never close a choice by default.** Autoexecution should preferably never remove a choice
  from its user. Some players will still want convenience that does.
- **D6. The model comes first** (2026-10-13). Do not argue for a Pets or engine design from
  autoexecution behavior. Policy adapts to the model.
- **D7. Dead ends are acceptable** (2026-10-14). Their exposure, late discovery, and need for caller
  rollback are not engine/model defects. Pruning them is the agent or client's responsibility;
  its absence does not justify changes to the model.

Admin is different. Its intended full autoexecution has no safe-predicate gate. It attempts every
supplied task in receipt order and retries work that is temporarily unavailable; a full pass with
no progress is an error. Admin is not a Player choosing an order. Keep its greedy settlement
separate from the Player policy and its safety predicates.

## What a task predicate means

The starting research scope is a **pending, mandatory, fully concrete Player gain or removal**:
the exact Type, holder or dependency, direction, and fixed amount are known; the task is not
optional, AMAP, an unresolved PER, or an unchosen branch. The policy may later gain rules for
other task shapes, but calling a target concrete does not make its amount or continuation fixed.
See [QUANTIFIERS.md](QUANTIFIERS.md) for the resolution-time differences.

A task predicate is a claim of this form: **when this exact task shape occurs under these live
conditions, the Player can select or execute it now without losing a post-action state.** There
will be separate collections of currently eligible gain and removal predicates. They are more
specific than lists of component names: a Type can be safe for a direct gain but unsafe for an
AMAP gain, a particular holder, or a task with a continuation.

SAFE applies the game's predicate to every optional change or removal, even when it is the only
actionable task. Being alone in the queue does not establish that an amount or card identity can
be chosen automatically.

The effective collections change as the action unfolds:

- **Start narrowly.** Enable a researched predicate only for the Canon configurations and task
  shapes covered by its record. Unknown content or task structure does not match it.
- **Remove the affected predicate early.** An initiating card play or action use can identify a
  rare dangerous route before the interacting card exists. If New Partner can leave its MC
  production gain pending while its chosen Prelude eventually brings in Manutech, the
  MC-production-gain predicate must be disabled when that route begins, not when Manutech appears.
  A coarse action-long suspension is acceptable for a rare route; it need not suspend unrelated
  types or ordinary actions.
- **Check live work as well as components.** Pharmacy Union's queued MC loss can remain after the
  corporation flips. A guard based only on whether Pharmacy Union is currently in play would
  restore MC-gain eligibility too soon. Industrial Complex and other production readers may be
  relevant before their cards enter play if pending work can play them.
- **Add predicates back when the reason ends.** At a completed action, or sooner if the relevant
  work and lasting effects have disappeared, restore each predicate whose normal conditions hold.
  Re-evaluate after committed commands; a cached list must not disagree with the World or queue.

The implementation computes eligibility from the current World and queue each time it considers a
task. No second mutable list needs synchronization. A guard therefore takes effect as soon as its
condition appears and ends when that condition clears.

## Current Canon predicate register

The stock, production, and Terraform Rating rows require a pending Player task consisting of a
direct `Gain` or `Remove`, with a positive fixed count, mandatory quantifier, fully concrete target,
no target refinement, no `THEN`, and no deferral. The target must have exactly the task assignee as
its owner. The card-draw row also accepts `PER` around such a gain. The same-track step row instead accepts a single
`AMAP` step on a public track. A transmutation, optional change, open `X`, held card resource,
or unresolved choice does not match these rows. These are **investigated or argued safe within
current Canon**, not mathematical proofs covering future declarations.

| Eligible task | Normal enabling conditions | Remove from eligibility when | Return to eligibility |
| --- | --- | --- | --- |
| Gain MC, Steel, Titanium, Plant, Energy, or Heat stock | No SolarPhase; no pending nested play/action route, unfinished card placement, or order-sensitive same-stock loss | Enter SolarPhase; queue a variable, clipped, optional, transferred, cross-actor, or otherwise complex same-stock loss; queue PlayCard, PlayPrelude, PlayProject, CopyPrelude, CopyProductionBox, UseAction, UseCardAction, UseStandardProject, UseTurmoilPolicyAction, DoRequiredActions, or StandardAction; or leave a card placement pending after such a signal is claimed | Those live conditions end |
| Gain MC stock, additionally | Conditions above | PharmacyUnion or MonsInsurance is in play; any pending task or its `THEN` contains an AMAP MC removal or transfer | The card leaves play and all related clipped-loss work clears |
| Gain TerraformRating | This Player's ActionScope exists; no pending nested play/action route; TerraformingDeal absent | Scope ends, a nested route appears, or TerraformingDeal enters play | A new qualifying scope or the route/card leaves |
| Gain Production of MC, Steel, Titanium, Plant, Energy, or Heat | This Player's ActionScope exists; no SoloMode or SolarPhase; no pending nested play/action route; every other ordinary task assigned to the same Player is a direct, positive, fixed, mandatory stock/TR change or another standard-resource Production gain of that Player, without target refinements or `THEN` | A competing task leaves that simple shape | Competing work clears while normal conditions hold |
| Gain CardBack into this Player's possession | Concrete mandatory draw, including a `PER` count evaluated when claimed; undo remains available | A future mode forbids undo back through the deal | Undo becomes available again. The current Playground deals opening corporation, project, and Prelude cards automatically while leaving their discards manual. |
| Gain one OxygenStep, TemperatureStep, or VenusStep with `AMAP` | This Player's ActionScope exists; every other ordinary task has the same Actor, cause, resolved track, amount one, and `AMAP` quantifier; no pending nested play route | Any other ordinary task differs or a nested play route appears | The different work clears. Venus L1 Shade's three Venus steps are the Canon scenario. |
| Remove TerraformRating | This Player's ActionScope exists; no pending nested play/action route; every other ordinary task assigned to the same Player is a direct, positive, fixed, mandatory stock/TR change of a different Type belonging to that Player, without target refinements or `THEN` | Scope ends, a nested route appears, or a competing task leaves that simple shape | A new qualifying scope with only independent work |
| Remove MC, Steel, Titanium, Plant, Energy, or Heat stock | This Player's ActionScope exists; no SoloMode, SolarPhase, or pending nested route; every other ordinary task assigned to the same Player is a direct, positive, fixed, mandatory stock/TR change of a different component type belonging to that Player, without target refinements or `THEN` | Any competing ordinary task falls outside that shape, or the phase/mode/route changes | Competing work clears and the normal conditions hold |
| Remove MC stock, additionally | Conditions above | PharmacyUnion or MonsInsurance is in play; pending work contains an AMAP MC loss/transfer; or RedsPolicy is in play while an ordinary TerraformRating gain remains pending | Those conditions end |

Evidence for the changed and added rows is in
[SafeTfmAutoExecCanonTest](../../test/common/dev/martianzoo/tfm/tests/rules/SafeTfmAutoExecCanonTest.kt):

- The stock-gain correction uses Mining Expedition in solo play and the
  [SoloStandardResourceReserve source](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/solo.pets).
- The Terraform Rating correction uses Shipment to Earth with Reds Policy and its
  [queued MC charge](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/turmoil/classes.pets).
- The production correction uses Anti-Desertification Techniques with Manutech and with Suitable
  Infrastructure already in play, while `OrderingMattersTest` checks Manutech arriving mid-action.
- The card-draw row uses Biolab's three ProjectCard draw beside its Plant production gain and the
  opening corporation, project, and Prelude draws. [PlaySessionTest](../../test/wasmJs/dev/martianzoo/tfm/web/playground/PlaySessionTest.kt)
  compares SAFE's opening deal with manual clicks using the same seed and verifies the discard
  tasks remain pending.
- The same-track row uses Venus L1 Shade's three `AMAP` VenusStep tasks from the same card;
  its [card source](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/prelude2/cards.json5)
  supplies that batch.

Deferred `LATER` and `FINALLY` work cannot overtake an ordinary task, so the removal predicate
checks competing **ordinary** tasks. `ActionScope` itself schedules its cleanup under `FINALLY`;
counting that cleanup as an ordinary competitor would disable every action-scope removal.
The same-Player assignee, same-Player target, and unrefined sibling requirements prevent a target
choice or a cross-player listener from hiding inside an otherwise simple-looking change. An
ActionScope owned by a different Player does not enable these predicates.

The shared nested-route check also sees a pending change that places a `CardFront`, including
one inside a task's `THEN` continuation. It suspends the stock, TR, production, and removal
predicates while that placement remains pending; claiming the original `PlayCard` or `UseAction`
signal alone does not restore eligibility. Later consequences are checked against the live queue.

The six direct stock gains have no current Canon gain listener that changes the Player's stock
according to its order. `SoloStandardResourceReserve` reacts to the SoloOpponent's own stock and
production and queues an Admin refill; it does not clip a Player's gain. The Mining Expedition
solo scenario checks the Player's Steel gain while other work remains. In an ordinary action, the
non-MC stock losses found in Canon are exact mandatory costs or optional attacks. An
early gain cannot force the Player to spend it, and an optional attack can still take the same
amount as after a delayed gain. Turmoil's Plant, Heat, and Titanium losses are computed or clipped
in SolarPhase; the stock predicates are suspended there. Pending nested play/action routes are
suspended before their new cards or actions are chosen. For additional caution, any currently
ordinary task or `THEN` that can remove the same stock suspends the gain predicate unless it is
an exact, fixed, mandatory, direct removal assigned to that Player, of that Player's stock, with
no continuation. Such a removal commutes with the gain even if the gain must fund the removal;
the resulting balance is the same. This exception lets production income run beside the five-MC
ProdOffset removal. A pending attack still suspends eligibility until that work clears.

MC differs. PharmacyUnion's microbe-tag consequence loses up to four MC, so gaining MC first can
increase the amount lost. MonsInsurance can transfer up to three of its owner's MC after an attack;
a MediaGroup reward can be gained before or after that compensation in the same HiredRaiders
action. Recession can queue a clipped MC loss for another Player during a nested Prelude play. The
MC guard checks both the live cards and unfinished clipped-loss instructions. After PharmacyUnion
flips to a PlayedEvent, the outstanding `-4* MC` work still disables MC autoexecution; when that
work clears, MC becomes eligible again.

The pending placement matters in a reachable WG Project action. Ecology Experts' microbe tag
queues Splice Tactical Genomics' mandatory two-MC reward for Kim. Stan chooses Project Inspection
as Ecology Experts' project. Even after Stan claims `PlayCard<ProjectInspection>`, its zero-cost
card placement is still pending; placing it reuses Board of Directors' action to play Recession.
Recession takes up to five MC from Kim. Taking the Splice reward before the placement leaves Kim
with zero MC; taking the same reward afterward leaves her with two. SAFE must hold the reward
through that placement, not merely through the original `PlayCard` task.

The TR-gain entry is deliberately confined to an action scope. The current gain listeners are
PreservationProgram's first-increment cancellation, RedsPolicy's queued three-MC cost,
UNMI/Pristar's once-per-generation marker, and TerraformingDeal's two-MC reward.
PreservationProgram cancels a single increment, and the UNMI/Pristar marker records whether an
increment occurred. RedsPolicy queues its MC charge whether the TR gain is claimed first or after
the other fixed work; Shipment to Earth exercises that order. TerraformingDeal's reward could be clipped by a
later MC loss, so its presence also disables the entry. TR-dependent Turmoil global
effects occur in SolarPhase, outside the entry. Direct fixed TR losses in ordinary ActionScopes
come from Pristar and TerraLabsResearch; the SolarPhase TR losses are AMAP and excluded. The
stock-removal entry is narrower: with only simple different-type changes competing, the surveyed
Canon listeners and quantities have no same-resource read that can distinguish doing that removal
first. The MC-removal entry adds the clipped-loss guards. With RedsPolicy in play, a queued
TerraformRating gain can cause a three-MC removal. The predicate conservatively waits until the
TerraformRating work clears before taking a competing fixed MC removal. Fixed costs alone have
not established a harmful early order; further same-action listeners or conditional income have
not been exhaustively ruled out. RedsPolicy alone does not disable a later unrelated MC removal.

The production-gain entry allows other fixed standard-resource production gains in the queue;
two such gains add independently even when they name the same resource. A pending
IndustrialComplex production box or Cyberia copy cannot pass the check, and a pending nested play
is suspended before that box appears. IndustrialComplex already in play has no ongoing production
listener. Manutech and Suitable Infrastructure already in play observe the same increase whether it
is claimed first or second; the Anti-Desertification Techniques scenarios check each live listener.
The nested-play guard still applies if a card could bring either listener into play between gains.
A pending RobinsonIndustries action or other quantity reader cannot pass the simple-competitor check. The
solo reserve and Turmoil global effects account for the phase/mode exclusions. This is an
investigated Canon predicate, not a claim that all production gains commute with other work.

## Why a few actions deserve special attention

The current Solarnet model puts all work from a card play and its nested plays into the same
eligible task queue. New Partner can therefore expose its MC-production gain beside a pending
Prelude draw and play; the chosen Prelude may be Merger, which plays a corporation. Ecology
Experts, WG Project, Excentric Sponsor, Board of Directors, and Double Down provide other
nested-play routes. Valley Trust plays a Prelude through a later required action; its starting
money is not in that same queue. Action-use routes, including Established Methods, Project
Inspection, Viron, Neptunian Power Consultants, and St. Joseph of Cupertino Mission, also deserve
a same-queue check. Merely naming a card with a play or action instruction is not evidence that it
can coexist with a particular concrete gain.

The reported physical-game ruling treats a card played by another card as a nested action context;
for example, a wild tag could be assigned differently in each context. Solarnet does not model
those protected contexts, and there is no current plan to add them for this policy. Analyze the
queue the engine actually offers, mark any resulting physical-rules uncertainty, and guard the
affected predicate. [ActionScopeTest](../../test/common/dev/martianzoo/tfm/tests/rules/ActionScopeTest.kt)
shows the present shared scope; [UnknownRulesTest](../../test/common/dev/martianzoo/tfm/tests/rules/UnknownRulesTest.kt)
records the unresolved New Partner ordering question.

## Research standard

Only current Canon in supported, compatible configurations is in scope. Explicit Unsafe
configurations are outside the safety claim. FakeHeadStart, invented cards, and tasks inserted
directly into a test queue do not establish a Canon interaction. A candidate counterexample must
start at a reachable decision point in a real Canon action. The same concrete task must be
claimed in both runs of that same action; only its claim time changes. Account for tasks generated
while the queue clears.

A mathematical proof for every predicate is not required. A highly determined search that finds
no counterexample is acceptable evidence for enabling a predicate. Record that result honestly
as **investigated safe**, not proven safe. A direct argument covering the relevant Canon cases can
be recorded as **argued safe**. In either case, name what was searched, the possible competing
instructions and listeners, the exact task shape, and when the predicate is disabled. If a
counterexample emerges, disable or narrow the predicate and add its same-action witness.
Unresearched cases remain manual. The policy aims to make no known Player choice; an empirical
record is not an absolute theorem about future Canon edits.

Maintain a record for each candidate predicate:

| Field | Question to answer |
| --- | --- |
| Task | What exact gain or removal, amount, holder, and quantifier does it match? |
| Normal eligibility | In which compatible Canon configurations and queue positions is it enabled? |
| Removal from active set | What initiating action, component, pending task, or continuation disables it, and how early? |
| Return to active set | What observable condition makes it eligible again? |
| Same-action evidence | Which reachable competing instructions, spawned work, listeners, limits, dependencies, and choices were checked? |
| Confidence | Argued safe, investigated safe, known counterexample, or unresearched; which tests and sources support it? |

Audit gains and removals separately. For removal, test whether early consumption affects later
payment, quantity resolution, minimum counts, a holder's existence, or loss listeners. For gain,
test later consumption or loss, caps, quantity readers, production or TR reactions, and a listener
that could be installed before a delayed gain. Check whether a named card's instructions can
actually mingle with the task in the same queue; the mere existence of a relevant action on that
card is insufficient.

[OrderingMattersTest](../../test/common/dev/martianzoo/tfm/tests/rules/OrderingMattersTest.kt)
already supplies harmful-early gain examples for MC and several production Types, with explicit
physical-rules qualifications for some Cyberia orders. Its comments naming Types without a
witness are **not** safe-predicate entries. The stock, TerraformRating, and restricted production
rows above are seeded under their stated live conditions.

## Implementation and remaining research

[AutoExecLoop.kt](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) claims the only ordinary
task in the World if its instruction is concrete, or a competing task approved by the game's
predicate. A task that needs Player input stays pending; SAFE does not choose or leave a task
selected. It reevaluates after every committed command. The factory currently defaults Players
and Admin to EAGER; Players opt into SAFE explicitly. [TfmAutoExec.kt](../../src/common/dev/martianzoo/tfm/engine/TfmAutoExec.kt)
owns the Canon predicates, supplied by [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt)
to all production Terraforming Mars Agent entry points. Game-neutral Agents have no researched
game predicate, so SAFE takes only an eligible sole task whose choices resolve without input there.

Within a turn-based play, production gains outside the restricted row remain manual because their
timing or amount can interact with pending work. Card-held resources, variable quantities, special
holders, and nonstandard changes remain manual there. A fixed stock gain computed by `PER` can
match the ordinary stock row after the World resolves its amount; Shipment to Earth's Earth-tag MC
gain is a Canon example. The register still declines a competing MC payment step or card placement
after payment: payment can have remaining resource choices, and placing a card can install
same-action listeners. A sole concrete task may be claimed by the shared loop.
The current predicates should be revisited when Canon adds a direct stock
gain listener, a new source of clipped loss, a quantity reader, or a new nested-play route.

The policy has focused tests for unsafe early-gain witnesses and selected SAFE predicates. Its
acceptance claim is limited to the predicate register and actual Canon queue shapes.

`SafeTasks` is test-only development analysis. It can propose additional rows, but its runtime
summary is not the predicate. Check any proposal against `DecisionSearch` with policy NONE, which
enumerates completed outcomes without SAFE's order reduction. A scratch harness compared the old
analyzer with this oracle on 806 claimable tasks at 240 positions from three random solo games with
at least two pending Player tasks: 692 were production income, 43 action-phase tasks, and the rest
setup, Prelude, and research work. All but one agreed; the exception was a payment whose own gate
kept it unavailable. The search found no claim that lost an outcome. Since production income
dominated the sample, those numbers are evidence that the analyzer was often conservative, not
evidence that its accepted tasks are sound across Canon. `OrderingMattersTest` supplies the rare
same-action witnesses for Pharmacy Union, Mons Insurance, nested Recession, Manutech arriving
through Merger, and Cyberia copying Industrial Complex. The register refuses those claims.

`DecisionSearch` also supports the Playground's dead-end pruning and forced-choice walk. A choice
is hidden only when an exhaustive search proves it cannot finish; an exhausted budget leaves it
visible. The Playground deals cards for draws the register accepts, but does not auto-claim a task
that asks which named card leaves Hand or Selecting. The search consults no deck and branches over
no card names, so its verdicts use only what the Player knows. Invention Contest, for example, is
proved completable with its cost alone (`DecisionSearchTest`).

The search reads a choice's options lazily, since an amount can have an unbounded domain: Tycho
Magnetics' owed Energy is offered up to `Int.MAX_VALUE`. It lists at most 100 amounts and tries
the middle and endpoints first, then the others. This can find a completion quickly without
assuming that completion is monotonic or forms a single range. A dead-end verdict still requires
checking every offered amount; if more than 100 amounts exist, failed listed amounts leave the
answer undecided. Paying more Titanium can make Nitrogen-Rich Asteroid playable when paying less
does not (`DecisionSearchTest`). Declining an optional amount is its own choice.
