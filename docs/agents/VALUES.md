# Project values

> **Read when:** designing, implementing, or reviewing a behavior or architecture change, especially
> when fidelity, generality, completeness, and conceptual cost compete.
>
> **Skip when:** performing a mechanical, behavior-preserving edit whose design ownership is already
> settled.
>
> **Status:** durable working rules.

These are the durable criteria for design and review. Repository-level instructions in
[`AGENTS.md`](../../AGENTS.md) remain authoritative for how to work.

## Aim for exceptional library design

Solarnet is meant to be a showpiece of library design, not merely a complete application or a rules
implementation that passes its tests. Its concepts, contracts, dependencies, and composition should
be unusually clear. The whole should feel like the natural assembly of understandable parts.

Design quality is a product requirement. Correctness against the project's declared behavior is
necessary, but an implementation that depends on incoherent exceptions, mirrored models,
privileged integration paths, or a disproportionate framework is still a failure. Prefer the
smallest coherent set of rules from which the desired behavior follows.

When choosing investments, prefer, in order:

1. A smaller, clearer, and more coherent model.
2. Better library responsibilities, contracts, and composition.
3. Better fidelity, usability, diagnostics, or performance where a demonstrated need selects them.
4. Broader coverage of official Terraforming Mars material.
5. Fan material, unrelated games, compatibility, and speculative flexibility only when explicitly
   selected.

This order chooses what to improve; it does not excuse defects. Preserve invariants, make only
claims the implementation satisfies, and hold lower-priority work to the same design standard.

## Build libraries that compose

Treat Solarnet as a collection of libraries, even when one application is their only current
consumer. Good separation directly improves that application: responsibilities become easier to
explain, dependencies easier to control, behavior easier to test, and parts easier to replace and
combine. Usefulness in unforeseen contexts should emerge from that discipline rather than from
designing for unusual hypothetical consumers.

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
required. Do not split cohesive behavior merely to increase the module count, and remain willing to
combine modules again when experience shows that a division is artificial. Module structure is a
design tool, not a ratchet.

Composition does not require broad abstraction. Separate the real capabilities Solarnet has, then
connect their honest contracts. Do not add flexibility for arbitrary games, hypothetical clients,
hostile callers, or imagined performance needs.

## Model the game honestly

Solarnet ultimately aspires to support every official Terraforming Mars card and rule exactly as the
designer intends, but completeness is deliberately a low priority. At this stage, design cleanup is
more important than forcing every official rule into the current model.

When exact fidelity would require disproportionate or poorly understood machinery, select the
clearest coherent variant the model can support and document the difference from the official rule.
A variant is a deliberate rule, not a new label for accidental behavior. Do not misrepresent it as
exact, and do not preserve a bad design merely because it happens to cover one more card. Revisit
documented variants as the model improves.

Adding cards is valuable primarily because varied and difficult rules test the model. A card may
reveal that existing concepts compose well, expose a missing general rule, or identify an honest
special case. Select card work for that design evidence, not to maximize a coverage count. Repeated
card-shaped workarounds indicate that the model is missing something.

Keep rules with the game component that owns them. Use a cross-cutting system component only when a
rule is genuinely ambient or switchable. `GreeneryTile` conditioned on `Photosynthesis` in
[Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
is the precedent for intrinsic behavior under an ambient rule; `PharmacyUnion` in
[Promo `cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets) is the
precedent for a published rule that genuinely needs exceptional treatment.

When the user explicitly requests Terraforming Mars rule research, use rulebooks and physical
components as primary evidence for printed content and verify disputed rulings against a post by
Jacob Fryxelius. Do not initiate rule research during routine implementation work.

## Keep concepts few and ownership precise

- First ask what can be removed, then whether existing Pets and domain mechanisms compose cleanly.
- Prefer one source of truth and one systemic rule over wrappers, duplicated representations,
  parallel APIs, and per-component exceptions.
- A hardcoded narrow fact can cost less than a framework. Repeated implementation-shaped exceptions
  can instead be evidence that a general concept is missing.
- Stop when a small request starts creating vocabulary across several modules. Explain the design
  pressure rather than normalizing disproportionate complexity.
- Evaluate each layer against the contract it owns. Lower layers preserve facts, validate legal
  mutations, and calculate consequences; caller policy and strategy belong above them.
- Do not push application preferences downward to guarantee a pleasant default, and do not omit a
  lower-layer invariant merely because an upper layer currently behaves well.

## Keep Pets central

Pets should read like the physical game: compact, composable, and precise about ownership, identity,
timing, and choice. Prefer hand-authored Pets plus general runtime semantics. A custom instruction is
honest when it bridges metadata or a capability Pets does not have; Kotlin-generated Pets is not
automatically simpler.

Components have types and multiplicity, not fields or incidental object identity. A Catalog
supplies coherent data, Modules select ambient rules, and a GamePremise describes one exact game.
Do not blur these roles or activate optional vocabulary merely by mentioning it in a safe query.

## Keep interfaces and evidence honest

- Use small, typed APIs and the narrowest visibility consistent with their responsibility.
- Preserve engine invariants even for trusted or rules-bypassing operations.
- Domain input must fail with domain errors. Programmer-error exceptions indicate invalid Kotlin or
  an impossible engine state.
- Prefer readable scenario and integration tests that prove observable behavior and library
  composition. Do not duplicate production catalogs or assert incidental task text and ordering.
- A passing narrow test proves only its assertion. Review the final diff and state what was not
  verified.
