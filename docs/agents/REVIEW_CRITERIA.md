# Final review criteria

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** reviewing a code change, reporting an implementation complete, or preparing to
> stage a commit.
>
> **Status:** mandatory final-diff rejection criteria. The owning documents linked below explain the
> underlying rules.

Apply these criteria to the entire final diff, not only the last revision. A violation makes the
change unacceptable to commit. Remove it, or explain why the criterion does not apply and obtain the
user's explicit acceptance before committing.

## Do not mistake dead ends for defects

Apply the [decision-tree principle](../../AGENTS.md#decision-tree-pruning-belongs-to-the-players-agent-or-client)
before raising findings: assume the player's agent or client presents only choices known not to
dead-end the turn, or accept that dead ends are exposed. No implemented pruner is required for this design
assumption. Protected Habitats allowing an attempted theft that later dead-ends and requires
rollback is valid modeling.

Reject findings, TODOs, and proposed engine/model changes justified only by an exposed dead end,
late failure, or caller rollback. Require a separate rule failure: for example, a prohibited theft
surviving successful turn completion, or a required legal outcome being excluded. This does not
prevent work explicitly requested on agent/client pruning.

## Scope and conceptual cost

Reject the change if:

- a narrow request introduced an unapproved gameplay helper, representation, processing stage,
  cross-module API, or vocabulary spanning several modules;
- the diff contains unrelated cleanup, restores adjacent behavior, or implements a speculative
  client, game, compatibility promise, or misuse defense;
- two representations or sources of truth now describe the same fact; or
- the permanent conceptual cost or diff size is disproportionate to the requested behavior.

Revisit [`VALUES.md`](VALUES.md#keep-concepts-few-and-ownership-precise) when any item is close.

## Rules and ownership

Reject the change if:

- card- or rule-specific Kotlin, a custom instruction, or component-specific gameplay machinery was
  used without first exhausting removal and ordinary Pets composition;
- `TfmGameplay` or test support creates or relocates rule components, supplies missing ordering, or
  identifies work through rendered text or cause;
- behavior originates from an inert identity or duplicated state rather than the live component
  that offers and rescinds it;
- a layer gained knowledge or policy outside its responsibility, including game-specific
  enumeration below the game view;
- engine task attribution was expanded into caller authentication, secret custody, or deciding
  which history players accept, responsibilities assigned outside the calculator by
  [ADVERSARIAL.md](ADVERSARIAL.md); or
- autoexecution, incidental iteration order, `THEN`, `::`, priority, or pre-pruning was used to hide
  missing choice, task identity, completion, or engine semantics. Exposing a path that must later
  be rolled back is not itself missing semantics; apply the dead-end principle above.

Use [`VALUES.md`](VALUES.md#keep-pets-central), [`TESTING.md`](TESTING.md#test-design),
and [`SEQUENCING.md`](SEQUENCING.md#before-adding-order) for the detailed tests. Inspect the current
Agent policy source before relying on automatic execution.

## Tests and evidence

Reject the change if:

- a card or rule test inspects task text, causes, incidental queue order, or mirrored Canon data
  instead of exercising player-facing operations and observable results;
- `.expect()` was replaced by broad absolute assertions around an action, or records costs, setup,
  literal task execution, or the correction itself rather than the action's interesting net result;
- a replay fact comes from generated output, an existing replay, or agent inference when original
  evidence is available;
- a replay correction is hidden inside an unrelated action, or a generated recording contains
  evidence, commentary, or assertions owned by its replay test; or
- known incorrect behavior is presented as an ordinary rule or accepted hack instead of an explicit
  passing characterization (`BugsTest`, or a card-test pair following `TESTING.md`).

Use [`TESTING.md`](TESTING.md#test-design) and the replay guide selected by
[`README.md`](README.md#verification-and-reconstruction).

## API specifications and KDoc

Apply the [API specifications and KDoc guidance](SPEC_FIDELITY.md), including
its nine-module priority scope. Reject documentation work if:

- it omits a public API contract merely because the declaration serves another repository module
  or is not a principal entry point;
- class KDoc neither explains applicable shared rules nor links directly to their common
  explanation;
- an API description leaves relevant caller obligations, observable outcomes, or failure effects
  ambiguous, or merely paraphrases the signature;
- duplicated explanations create competing sources of truth;
- documentation turns genuinely unresolved intent into a promise, or weakens a clearly intended
  contract merely to match an implementation defect; or
- documentation work silently changes behavior or visibility to fit the contract.

A clearly intended contract may be specified before the implementation satisfies it. Track known
mismatches separately; a defect notice in the specification or KDoc is not required. Assume readers
already understand Pets and Solarnet.

## Final coherence

Reject the change if:

- it undoes any requirement already accepted during the task;
- tests pass only for a narrow assertion while the affected behavior, composition, or final diff
  remains unverified;
- public behavior, commands, setup, configuration, or APIs changed without updating the smallest
  owning document; or
- the staged diff differs from the complete working-tree state that was reviewed and tested, or
  includes unrelated work.

Passing this review means no rejection criterion applies. It does not turn a suspicious design into
an accepted exception; only the user's explicit acceptance does that.
