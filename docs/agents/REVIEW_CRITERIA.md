# Final review criteria

> **Read when:** reviewing a code change, reporting an implementation complete, or preparing to
> stage a commit.
>
> **Status:** mandatory final-diff rejection criteria. The owning documents linked below explain the
> underlying rules.

Apply these criteria to the entire final diff, not only the last revision. A violation makes the
change unacceptable to commit. Remove it, or explain why the criterion does not apply and obtain the
user's explicit acceptance before committing.

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
  enumeration below the game view; or
- autoexecution, incidental iteration order, `THEN`, `::`, priority, or pre-pruning was used to hide
  missing choice, task identity, completion, or engine semantics.

Use [`VALUES.md`](VALUES.md#keep-pets-central), [`TESTING.md`](TESTING.md#test-design),
[`SEQUENCING.md`](SEQUENCING.md#before-adding-order), and
[`AUTOEXEC.md`](AUTOEXEC.md#choice-safety-check) for the detailed tests.

## Tests and evidence

Reject the change if:

- a card or rule test inspects task text, causes, incidental queue order, or mirrored Canon data
  instead of exercising player-facing operations and observable results;
- `.expect()` was replaced by broad absolute assertions around an action, or records costs, setup,
  literal task execution, or the correction itself rather than the action's interesting net result;
- a replay fact comes from generated output, an existing replay, or agent inference when original
  evidence is available;
- a replay correction is hidden inside an unrelated action, or a gameviewer recording contains
  evidence, commentary, or assertions owned by its replay test; or
- known incorrect behavior is presented as an ordinary rule or accepted hack instead of a passing
  observable `BugsTest` characterization.

Use [`TESTING.md`](TESTING.md#test-design) and the replay guide selected by
[`README.md`](README.md#reconstruct-a-game).

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
