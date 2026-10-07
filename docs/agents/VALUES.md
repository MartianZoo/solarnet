# Project values

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** designing, implementing, or reviewing a behavior or architecture change, especially
> when fidelity, generality, completeness, and conceptual cost compete.
>
> **Skip when:** performing a mechanical, behavior-preserving edit whose design ownership is already
> settled.
>
> **Status:** durable working rules, plus a register of complexity findings already dispositioned.

These are the durable criteria for design and review. Repository-level instructions in
[`AGENTS.md`](../../AGENTS.md) remain authoritative for how to work.

## What Solarnet is for

Solarnet is a playground for exceptional software and language design. Terraforming Mars is the
demanding subject through which the project can discover and demonstrate a small executable algebra
of rules. The owner has not settled how to trade further core-design work against enabling
outsiders, or which audience to serve first.

The project should attest that a complicated real system can arise from concepts that are few,
precise, composable, and honestly owned. The result should feel discovered rather than patched
together. Correct behavior is necessary, but an implementation that relies on incoherent
exceptions, mirrored models, privileged integration paths, or a disproportionate framework is
still a design failure.

An especially valued outcome is an independent builder arriving with an unexpected idea they are
passionate about, building it themselves, and being proud of the result, with Solarnet helping them
succeed. Skilled UI developers building real user interfaces would also be welcome. These are
reasons to make the libraries useful to others, not a selected audience or a commitment for the owner
to build a particular application.

## Priority tiers

These tiers describe enduring design values, not a settled ranking of audiences or an implementation
schedule. Some are different faces of the same design, and evidence is a proof obligation rather
than a competing feature.

[PLANS.md](PLANS.md) indexes the concrete programs that pursue these outcomes.
[Current major-plan priority](#current-major-plan-priority) records the owner's present emphasis and
the comparisons that remain unsettled.

### Tier 1: defining and worth active investment

1. An exceptionally small, clear, regular, and formally precise semantic model. Profound software
   design matters more than uncovering a grammar peculiar to Terraforming Mars.
2. One semantic source driving execution, natural-language explanation, iconography, and analysis.
   The concrete **trifecta** milestone is around 300 cards that work perfectly, have good English
   instructions, and have good iconographic depictions, all derived from that same source.
   Single-sourced FAQ entries would be a major bonus, not a condition for reaching the trifecta.
3. Ordinary game meaning in authored Pets and general semantics, not Kotlin orchestration.
   Production code such as `TfmGameplay`, initializers, and workflow glue should not know particular
   cards, components, expansions, or science-fiction concepts.
4. Exact source-backed replay tests proving that the model works across whole games. Focused
   scenarios and language tests matter, but nothing provides comparable evidence that the pieces
   work together.
5. Runtime consequences that can be understood without reconstructing incidental machinery. The
   fragmented action and payment lifecycle is an important example: task-pool searches,
   cause-based choreography, and enormous traces should not be needed to understand an action.
6. Compact authored Pets that evokes the physical icon grammar without compromising precise
   semantics. Judge genuine notation-versus-semantics conflicts case by case.

Current work should preserve the replay evidence while improving these foundations; there is no
expected need to choose between clean orchestration and working replays. Complexity serving only
one to three minor cards deserves special scrutiny, including consideration of dropping the cards.

### Tier 2: valued, but not a current program of work

1. An independent executable conformance suite that can falsify the engine rather than trusting a
   transparent implementation to audit itself.
2. Material performance improvements that enable qualitatively different work. Small percentage
   gains do not justify attention; large gains can.
3. Excellent parser and typechecker diagnostics, especially for mistakes authors hit commonly.
4. Generative and property-based exploration of interactions not represented by curated examples.
5. A polished explanation for outsiders and, later, an educational reconstruction of how the design
   developed. This is distinct from the internal agent handbook, whose accuracy currently protects
   Tier 1 design work. The clean resulting model matters more than preserving its history during
   development.
6. Richer causal presentation when it can be derived or post-processed cheaply. Existing event logs
   already provide substantial traceability, so perfect attribution does not merit design cost.

These may be improved opportunistically or when they block Tier 1, but they do not currently earn a
major initiative of their own.

### Tier 3: desirable, but given little weight

- Broad coverage of official material and exact support for every awkward card or expansion.
- General reuse, unrelated games, and module purity pursued for their own sake rather than as
  evidence of a coherent design.
- Fan material and an owner-built polished player product.
- Large-scale strategic analysis and a separate optimized engine.
- A comprehensive causal-analytics product built over exported histories.
- Making raw Pets immediately understandable to a typical Terraforming Mars player.

Things the project does not regard as desirable are omitted rather than assigned a tier. These
tiers do not excuse defects or authorize claims the implementation cannot support.

## Current major-plan priority

There is **no definitive implementation sequence** among card tracking, export/import, autoexec,
payment simplification, and the trifecta. Export/import, then autoexec improvement, then payment
simplification was an expressed inclination; it is not a binding schedule or a ranking of all
programs. Restoring a coherent middle-ground card-tracking game-playing API may be especially
important, but its precedence over the other work has not been settled.

[CARD_HANDLING.md](CARD_HANDLING.md) owns that selected external-tracking direction;
[EXPORT.md](EXPORT.md) owns the requirement for a faithful, clean record of player decisions.
Saving and resuming unfinished games is not a priority. Do not promote it into an export acceptance
requirement.

End-to-end solo play, including autonomous play, is a desirable goal. The owner's working view is
that human and computer play are the same goal: both need the same game-playing capabilities. Do not
split those capabilities into separate gameplay architectures. This does not select an AI research
program or a work sequence.
Hidden information, player-specific universes, and drafting are outside the selected card-handling
goals.

The existing action/payment, workflow, Agent-policy, handbook, public-contract, and semantic programs
remain indexed in [PLANS.md](PLANS.md). Their position in that index must not be interpreted as an
authoritative execution order. Preserve source-backed replay evidence while improving the design;
evidence is a proof obligation, not a competing feature. The bounded Class-universe finish is
complete; [CLASS_TABLES.md](CLASS_TABLES.md) records its stable ownership and authority rules.

## Let libraries attest to the design

Treat Solarnet as a collection of libraries where that division makes responsibilities and
semantics clearer. Composition is a test of the model, not an end-user requirement for hypothetical
consumers. Good separation can make ownership easier to explain, dependencies easier to control,
behavior easier to test, and parts easier to replace and combine.

- Give each library one intelligible responsibility and a small, expressive contract.
- A caller should depend only on the capabilities it uses. Every module dependency must be
  logically justified by the responsibility of the depending module.
- Prefer one-way dependencies. Avoid cycles, ambient initialization, shared global state, and
  assumptions that unrelated application layers are present.
- Test modules independently and test meaningful compositions across them.
- Do not promise API stability yet. Change an interface when doing so produces a better design;
  there are no compatibility clients to preserve.
- Use KDoc to make contracts and their intent increasingly self-explanatory. Add separate module
  documentation when the subject warrants it.

There is no predetermined correct module size. If one group of classes can be explained as doing X
and another as doing Y, consider separate fine-grained Gradle modules. A second consumer is not
required. Do not split cohesive behavior simply to increase the module count, and remain willing to
combine modules again when experience shows that a division is artificial. Module structure is a
design tool, not a ratchet.

Composition does not require broad abstraction. Separate the real capabilities Solarnet has, then
connect their honest contracts. Do not add flexibility for arbitrary games, hypothetical clients,
hostile callers, or imagined performance needs. A less reusable structure can be correct when it
keeps the model smaller and its ownership more honest.

## Model the game honestly

Solarnet may continue to acquire official Terraforming Mars cards and rules indefinitely, but
completeness is not a project goal. Design cleanup is more important than forcing every official
rule into the model.

When exact fidelity would require disproportionate or poorly understood machinery, select the
clearest coherent variant the model can support and document the difference from the official rule.
A variant is a deliberate rule, not a new label for accidental behavior. Do not misrepresent it as
exact, and do not preserve a bad design just because it happens to cover one more card. Revisit
documented variants as the model improves.

Adding cards is valuable primarily because varied rules test the model. A card may reveal that
existing concepts compose well, expose a missing general rule, or identify an honest special case.
Select card work for that evidence, not to maximize a coverage count. Do not romanticize ugly edge
cases merely because they are difficult.

Any engine behavior or semantic machinery used by only one to three minor cards deserves explicit
investigation. That count is a suspicion trigger, not an automatic deletion rule: ask what general
truth the mechanism captures and what becomes simpler if the cards or behavior are dropped. Be
willing to retire cards and nominal support when doing so materially simplifies the model.

Keep rules with the game component that owns them. Use a cross-cutting system component only when a
rule is genuinely ambient or switchable. `GreeneryTile` conditioned on `Photosynthesis` in
[Terraforming Mars `board.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets)
is the precedent for intrinsic behavior under an ambient rule; `PharmacyUnion` in
[Promo `cards.json5`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) is the
precedent for a published rule that genuinely needs exceptional treatment.

When the user explicitly requests Terraforming Mars rule research, use rulebooks and physical
components as primary evidence for printed content and verify disputed rulings against a post by
Jacob Fryxelius. Do not initiate rule research during routine implementation work.

## Keep concepts few and ownership precise

- First ask what can be removed, then whether existing Pets and domain mechanisms compose cleanly.
- Prefer one source of truth and one systemic rule over wrappers, duplicated representations,
  parallel APIs, and per-component exceptions.
- A hardcoded narrow fact can cost less than a framework. Repeated implementation-shaped exceptions
  can instead indicate that a general concept is missing.
- Stop when a small request starts creating vocabulary across several modules. Explain the design
  pressure rather than normalizing disproportionate complexity.
- Evaluate each layer against the contract it owns. Lower layers preserve facts, validate legal
  mutations, and calculate consequences; caller policy and strategy belong above them.
- Do not push application preferences downward to guarantee a pleasant default, and do not omit a
  lower-layer invariant just because an upper layer currently behaves well.
- Treat broad cross-module pressure as evidence about the model. Removing domain-specific
  orchestration, making scopes self-running, and simplifying actions and payments may be parts of
  the same correction rather than projects to optimize independently.

## Dispositioned complexity findings

Reviews that sweep for removable complexity keep rediscovering the same machinery. Record what was
decided about each finding so the next sweep spends its effort on something new.

Use one of these dispositions:

| Disposition | Meaning |
| --- | --- |
| **At peace with it** | Deliberate and expected to last. Do not propose removing it; the reasoning says why the apparent cost is worth it, or why the measurement that flagged it was misleading. |
| **Accepted for now** | The cost is real and a better option would be taken, but none is known or an external constraint holds. The entry names what would change the answer. |
| **Under design** | The problem is recorded, but its replacement remains open. Keep alternatives and evidence in the owning document. |
| **Will be obsolete** | It disappears as a consequence of other selected work. Do not spend design effort on it directly. |

An entry records a decision that was made, not a rule that cannot change. Overturn one by showing
its reasoning wrong — new evidence, a changed constraint, or a second client that shifts the
balance. Rediscovering the same cost is not enough to overturn one.

Keep the substantive reasoning in the owning document and keep this table to one line per finding.

### At peace with it

- **`Die` as a concrete zero-capacity Type** —
  [CLASS_TABLES.md](CLASS_TABLES.md#die-and-ok). Concrete accurately makes `Die` final;
  `HAS MAX 0 This` states why it cannot occur without making abstractness falsely advertise an
  implementation choice. Structurally uninhabited Types remain a distinct general case even where
  the engine can derive the same result.
- **Class properties as a mechanism** — [PROPERTIES.md](PROPERTIES.md#why-class-properties-earn-their-cost).
  Few property kinds, one or two declaring classes each; without them the same facts needed major
  cheats. Declaring-class count is not the measure.
- **Pets `Action` cost sugar and the parallel `Cost` AST** —
  [ACTIONS.md](ACTIONS.md#why-the-action-cost-form-is-a-product-requirement). `cost ->` is a product
  requirement used by many cards.
- **The metric operator set** — [ENGINE.md](ENGINE.md#the-metric-operators-are-intentional). `Max`,
  `Subtract`, and `Or` have few authored uses, but the algebra is under-built rather than
  over-built.
- **Handwritten `*.cards.pets` fragments** —
  [`StandardFormBundle`](../../src/common/dev/martianzoo/tfm/canon/StandardFormBundle.kt). The
  suffix is the smallest way to keep handwritten cards in the existing card-resource selection
  model without another Bundle.
- **Explicit exclusion in normal-corporation offers** — [WORKFLOW.md](WORKFLOW.md#current-foundation).
  Standard back typing already rejects beginner faces, while `NOT BeginnerCorporation` deliberately
  states the normal-path rule at every normal-corporation offer.
- **Turmoil's `TurmoilPlayer` and `ApplyRulingBonus`** —
  A player's delegate cap needs the bearer's owner available inside an effect, unlike Neutral's
  direct invariant. The ruling bonus cannot trigger on `Ruling`, because setup places Greens without
  applying its bonus. Event positions depend on live events; `EACH` binds the event across position
  transmutation, and removing that event removes its position.
- **Turmoil's two party-arrow supertypes and two reveal requests** —
  Pets needs distinct spellings for the two otherwise indistinguishable party dependencies. The
  Coming request bootstraps setup by becoming the Distant request after its choice; later Distant
  requests simply finish, and a shared position parameter would also admit unsupported Current.

### Accepted for now

- **Claims through shared occupancy** — [PLANS.md](PLANS.md#retain-claims-through-shared-occupancy).
- **Refinements as Types** — [type-system-spec.md](../type-system-spec.md#refinements-are-types).
  One recursive Type model preserves refinements in dependency positions and Type variables without
  a parallel resolved-expression representation. A separate structural Type model would be more
  ontologically precise, but its aggregate complexity is not currently justified. Revisit if
  world-dependent Type operations cause concrete API or correctness problems.
- **`BigInt`** — a bespoke immutable bit mask serving one field, `Class.abstractSupertypeBits`.
  Common code has no `java.util.BitSet`, so the alternative is a slower supertype test on a hot
  path. Revisit if a multiplatform bitset becomes available or if the test stops being hot.
- **Remaining address-only `System` classes** — [EACH.md](EACH.md). Remaining listeners and task
  holders have lifetime or routing roles. Revisit where fanout can preserve recipient decision
  authority without the extra component.

### Under design

- **Delegated control and idle cleanup** —
  [SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options) compares nested
  priorities with exclusive operation scopes. Neither replacement has been selected.
- **Action identity and permission** — [ACTIONS.md](ACTIONS.md) records the distinct roles of
  action identity, turn permission, usage markers, and completion latches; their simplification
  remains open.

## Keep Pets central

> **Recurring failure warning:** If one card or rule appears to need custom Kotlin, a custom
> instruction, or a component-specific gameplay helper, stop before implementing it. Name the
> general capability ordinary Pets lacks, and first try removal or composition of existing
> mechanisms.

Pets should read like the physical game: compact, composable, and precise about ownership, identity,
timing, and choice. Authored source data may lower into Pets, but there must not be a second parallel
declaration of the same content. Once lowered, execution must proceed through Pets semantics.

First make a genuine attempt to express behavior in ordinary Pets. Custom instructions, custom
metrics, and other deliberately bounded custom classes are acceptable when they keep the general
language and engine smaller. They should state an honest exception at the semantic boundary, not
smuggle card knowledge into orchestration. A few explicit custom classes can cost less than a
general feature, especially when that feature would exist for only a few cards.

The hard boundary is ordinary production Kotlin outside those extension points. It must not branch
on particular cards, components, expansions, or setting vocabulary. `TfmGameplay`, initializers,
phase drivers, and similar integration code must not repair or coordinate individual content. If a
behavior cannot be expressed in raw Pets or a bounded custom semantic extension, reconsider the
behavior, the card's inclusion, or the model before adding orchestration.

Keep approved custom Kotlin minimal and plainly owned. Kotlin-generated Pets is not automatically
simpler, and a proliferation of custom classes is still a reason to look for a missing general
concept; neither observation makes every custom class a design failure.

Prototype code is not exempt from these boundaries. If demonstrating a proposed model requires
mirrored semantic state, a privileged runtime path, or domain-specific knowledge outside the
declared source-lowering and custom-extension boundaries, stop: that requirement is evidence
against the model, not scaffolding to implement it.

Components have types and multiplicity, not fields or incidental object identity. A Catalog
supplies coherent data, Modules select ambient rules, and a GamePremise describes one exact game.
Do not blur these roles or activate optional vocabulary simply by mentioning it in a safe query.

## Keep interfaces and evidence honest

- Use small, typed APIs and the narrowest visibility consistent with their responsibility.
- Preserve engine invariants even for trusted or rules-bypassing operations.
- Domain input must fail with domain errors. Programmer-error exceptions indicate invalid Kotlin or
  an impossible engine state.
- Prefer readable scenario and integration tests that demonstrate observable behavior and library
  composition. Do not duplicate production catalogs or assert incidental task text and ordering.
- Focused scenarios and full source-backed replays are complementary. Scenarios explain individual
  rules; replays prove that the model survives their interaction. Do not trade either away merely
  to improve a coverage count.
- Exact replay evidence is non-negotiable wherever the project claims modeled behavior. Preserve
  original sources, make corrections visible, and ensure the observed execution genuinely follows
  from the declared model. Rich exported provenance and causal analytics can wait.
- A passing narrow test proves only its assertion. Review the final diff and state what was not
  verified.
