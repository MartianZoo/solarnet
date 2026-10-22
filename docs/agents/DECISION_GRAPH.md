# Decision graph notes

> **Raw material — October 22, 2026.** Notes from discussion, not a finished design or an
> implementation specification. Keep open questions open. The engine interaction contract lives
> in [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md).

## Recorded direction

- **Single-Action Decision Graph** names the decisions and resulting states involved in completing
  an action. It is implicit: we can reason as though the graph exists without constructing or
  storing a graph structure. No new graph representation is selected.
- Include task selection and individual narrowing choices, even if a view also treats a completed
  task as a single step. An action can involve multiple tasks, requests, and Actors; another
  Player's response belongs to it too. An empty queue for the initiating Player is insufficient.
- Exploration and pruning belong to the agent. The engine module does not and will not decide
  whether an entire action can finish. It executes submitted instructions and their consequences.
- Explore pruning choices for which no continuation can complete the action. The goal is to reject
  such a choice when it is made, rather than reveal the dead end several decisions later. This
  removes impossible continuations, not strategically undesirable choices.
- The choice catalogue is task selection, `OR` alternatives, abstract types and their dependencies,
  amounts (`?` and `X`), and card identities. A deterministic prompt order should avoid different
  paths that merely ask the same questions in different orders. Choosing a class can reveal
  dependencies and therefore questions that were not visible beforehand.
- Current evidence: [TaskForm](../../src/common/dev/martianzoo/agent/TaskForm.kt) has
  `ALTERNATIVE`, `TARGET`, and `AMOUNT` decisions. It chooses an `OR` arm first, then an abstract
  class before its dependencies, then amount. The caller picks a task ID before creating its form.
  Naming a played card's class can be a target choice; naming anonymous card movements belongs to
  [CardTracking](../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt), not a fourth form kind.
  Arbitrary `TaskForm.narrow` calls need not follow the guided prompt order. The requested evolution
  to **TASK, OR, TYPE, NUMBER, CARD**, in that order, is recorded in [TODO.md](../../TODO.md).

## Proposed endpoint: action completion

Use **action complete** for the proposed endpoint below; its exact recognition remains open.
An individual task's instruction being resolved and concrete does not establish action completion.

- **Proposal for review:** an action is **complete** when all its obligations and consequences have
  finished, including removal of its `ActionScope`, dependent cleanup, and work caused by that
  removal. Continue automatic game progression to the next independent decision or game end.
  A new action offer may already be pending, including the same Player's second action; executing
  that new action is outside this graph.
- `ActionScope` is the promising Canon anchor, not an engine completion concept. Its disappearance
  alone is insufficient: removal can enqueue more work. Merely activating `FINALLY` is earlier
  still, because the scope-removal task has not necessarily executed.
- Example: Stan's greenery can require Kim's Philares choice. Include that response, scope removal,
  and resulting cleanup before calling Stan's action complete. A subsequent action offer does not
  make Stan's previous action incomplete.
- Canon's [scope rule](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/game.pets)
  removes it through `FINALLY`; `Baton` reacts by deferring another `OfferAction`. Thus “all
  recursively caused tasks” without a stopping criterion would absorb later actions indefinitely.
  Include the handoff and automatic consequences, but stop before an independent new decision;
  a remaining response to the old action is not such a decision.
- Open: how the agent recognizes that distinction using existing facts, and what anchor applies
  to production or other interactions without an `ActionScope`. Do not introduce engine task
  ancestry or completion machinery merely to make detection convenient.
- [Sequencing's current completion check](SEQUENCING.md#operation-completion) can accept before
  scope removal and assumes removal cannot require further action choices. It does not establish
  this stronger endpoint. Reconcile the older authoring restriction during future design work.
- Every fully explored successful path must reach action completion; a known unsuccessful path
  dead-ends. A path stopped because card play cannot be predicted remains unknown, not a proved
  success or failure.

## Generic cards and unknowable completion

- **Selected direction:** this graph, and all speculative state exploration, must be happy with
  generic faceless cards. Speculation must not demand invented names for anonymous draws, moves,
  purchases, or discards. Actual execution supplies the required identities.
- Preserve identities already assigned before speculation begins; known card plays can be
  analyzed normally. Card handling should look like ordinary narrowing to the pruning layer
  as far as possible.
- Label a card-play edge **“completability unknowable”** whenever that play could use a card
  without an assigned name before this speculative exploration began. Do not invent an identity
  and claim to have proved completion, or continue analysis past that play.
- The criterion is knowledge at the start of speculation, not the card's current location.
  Playing from `Selecting` is the expected Canon correspondence, still to verify; a card drawn
  after speculation began does not become predictable just because it later enters `Hand`.
- Merger motivates this stopping rule. A fizzle alternative, discussed using WG Project, may
  independently establish a successful continuation without analyzing the unknown card play.
  Preserve that distinction: an unknown branch is not a dead end, and an available successful
  alternative can make further search unnecessary. These examples still need focused verification;
  they do not select card-specific engine exceptions.
