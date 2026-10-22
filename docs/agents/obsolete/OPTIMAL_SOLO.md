> **Archaeology only.** This document preserves a mix of how things were and how we wanted
> them to be around **October 22, 2026**. It is not current guidance, an implementation
> contract, or a list of work to resume. Earlier status labels and instructions below are
> historical quotations, not present requirements.

Original source: `docs/agents/OPTIMAL_SOLO.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

Full historical document.

# Monotonicity in optimal solo play

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** working on TR63 exact/conservative optimization, standard-resource monotonicity, or
> the associated report tool.
>
> **Skip when:** implementing solo rules or evaluating general AI-player architecture.
>
> **Status:** research note; no optimizer is implemented.

## Source map

- [`StandardResourceMonotonicityReport.kt`](../../../pets/src/jvm/dev/martianzoo/tfm/petstools/StandardResourceMonotonicityReport.kt)
  — search for `fun main` to inspect the implemented catalog report.
- [`SoloGenerationCountdownTest.kt`](../../../test/common/dev/martianzoo/tfm/tests/rules/SoloGenerationCountdownTest.kt)
  — read only when a proposed optimization depends on committed TR63 timing.
- [Terraforming Mars `cards.json5`](../../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/cards.json5)
  — search for a named counterexample such as `Factorum` before relying on the analysis.

This note develops one facet of exact or conservative optimization for the TR63 solo variant. It
does not describe an implemented optimizer. The current goal is to recognize when an additional
component is a pure benefit, when loaded game material disproves that claim, and when facts about the
current state restore it conditionally.

## Adopted problem

For one fixed [`GamePremise`](../../../pets/src/common/dev/martianzoo/catalog/GamePremise.kt), play is a finite sequence of player choices with
deterministic consequences. A completed game scores normally if the player has at least 63 TR after
the last production phase and scores zero otherwise. A game in which a deck runs out and would be
reshuffled is outside the problem and is discarded rather than evaluated.

Open-deck and hidden-deck play are different problems:

- In open-deck play, deck contents and order are part of the known state. “Best attainable score”
  has an exact meaning.
- In hidden-deck play, the player knows only a set or distribution of possible deck orders. We must
  eventually choose whether “best” means best expected score, best guaranteed score, or something
  else. Structural conclusions that hold for every possible deck do not depend on that later choice.

Deck order and knowledge here belong to the optimizer's external search problem, not to hidden
state that Solarnet must maintain or protect. Hypothetical draws and rewinds are freely calculable;
access to actual undisclosed draws and recognition of a played score are external responsibilities
described in [ADVERSARIAL.md](../ADVERSARIAL.md).

At the client interface, every method call both begins and ends with a nonempty player task
queue. Starting and finishing the game are the exceptions. The engine may pass through an idle world
internally, but that state is not observable through this interface. Each client-visible decision is
therefore one of:

1. selecting one queued task; or
2. narrowing the selected task, possibly one sub-Specification at a time.

Resolution, execution, triggered effects, and other forced processing after a choice can be viewed
as its automatic consequences. The optimizer branches only where the client can choose.

## Safe simplification of the decision tree

Monotonicity is useful because it can justify reductions before expensive search. A move can be
executed eagerly only when its full automatic consequences preserve at least one optimal
continuation. Useful sufficient evidence includes:

- it adds only components already certified as pure benefits in the current state;
- it changes no blocker requirement, timing opportunity, deck knowledge, cost, or trigger;
- it can be swapped with every still-relevant alternative without changing either result; or
- every branch it removes has been proved no better than a surviving branch.

These tests are intentionally stronger than “the move looks good.” Factorum shows why: gaining a
normally valuable resource can simplify the option list by deleting exactly the option that should
have been taken first.

## Static analysis

The [Pets monotonicity note](pets/MONOTONICITY.md) owns the report,
its evidence levels, blocker analysis, and Factorum examples.
