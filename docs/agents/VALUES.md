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

## Product aspiration

All else being equal, Solarnet aims to support every official Terraforming Mars card and rule,
including expansions, exactly as the designer intends. Compromises on fidelity should be rare,
small, and explicit.

Completeness does not override conceptual integrity. A feature that works only by adding an
incoherent exception, parallel mechanism, or disproportionate framework is still a design failure.

## Model the game, not the implementation

- Correct behavior is necessary but not sufficient. Prefer a small set of coherent rules from which
  card behavior follows naturally.
- Keep effects with the game component that owns the rule. Use a cross-cutting system component only
  when the rule is genuinely switchable or ambient.
- Greenery-to-oxygen lives intrinsically on `GreeneryTile`, conditioned on the ambient
  `Photosynthesis` state. `PharmacyUnion` is the opposite kind of exception: its published rule
  genuinely needs special treatment.
- Prefer hand-authored Pets plus general runtime semantics. A custom instruction is honest when it
  bridges metadata or a capability Pets does not have; Kotlin-generated Pets is not automatically
  simpler.
- When the user explicitly asks for Terraforming Mars rule research, verify disputed rulings against
  a post by Jacob Fryxelius; rulebooks and physical components remain primary evidence for their
  printed content. Do not initiate rule research during routine implementation work.

## Minimize permanent concepts

- First ask what can be removed, then whether existing Pets and domain mechanisms compose cleanly.
- Prefer one source of truth and one systemic rule over wrappers, mirrored state, parallel APIs, or
  per-component exceptions.
- Do not build flexibility for hypothetical games, clients, hostile callers, or performance needs.
  The known project is allowed to constrain the design.
- A hardcoded narrow fact can be cheaper than a framework. Conversely, repeated implementation-shaped
  workarounds are evidence that the general model is missing something.
- Stop when a small request starts requiring new vocabulary across several modules. Explain the
  pressure instead of normalizing a disproportionate design.

## Respect layer responsibility

- Evaluate each layer against the contract it owns. Do not make a lower layer responsible for the
  quality, strategy, or product behavior chosen by a caller above it.
- State and engine layers preserve game facts, validate legal mutations, and calculate their atomic
  consequences. They do not decide which legal mutation a client ought to make.
- An Actor-access layer is the place where caller authority and visible state may eventually be
  restricted. Its first design is intentionally maximally permissive; do not invent granular policy
  before a real caller requires it. An Agent sits above access and uses the same mutation path for
  explicitly requested and Driver-chosen actions.
- An autoexecution policy may be cautious, adversarial, whimsical, or game-playing. Lower layers do
  not care which legal option it selects; named policy guarantees belong to that policy and its
  application.
- Do not push application preferences downward merely to guarantee a pleasant default. Conversely,
  do not omit a lower-layer invariant because an upper layer currently behaves well.

## Keep Pets central

- Pets should read like the physical game: compact, composable, and precise about ownership,
  identity, timing, and choice.
- Components have types and multiplicity, not fields or incidental object identity. Add another
  representation only when the rules truly distinguish it.
- A Catalog supplies coherent data. Modules select ambient rules. A GamePremise describes one
  exact game. Do not blur these roles.
- Load only the vocabulary a game needs. Mentioning an inactive optional type in a safe query must
  not activate its expansion.

Source precedents: search for `CLASS GreeneryTile` and `CLASS Photosynthesis` in
[Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
for an intrinsic rule with ambient conditioning. Search for `PharmacyUnion` in
[Promo `cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets)
only when evaluating a genuinely exceptional published rule.

## Keep interfaces and evidence honest

- Use small, typed APIs and the narrowest visibility. There are no compatibility clients to protect.
- Preserve engine invariants even for trusted, rules-bypassing operations.
- Domain input must fail with domain errors. Programmer-error exceptions indicate invalid Kotlin or
  an impossible engine state.
- Prefer scenario and integration tests that prove observable rules. Do not duplicate production
  catalogs in tests or assert incidental task text and ordering.
- A passing narrow test proves only its assertion. Review the final diff and state what was not
  verified.

## Spend effort deliberately

After fidelity, prioritize completeness, simplicity, and composability. Official material matters;
fan material, unrelated games, Turmoil, polished UX, compatibility, and performance are lower
priorities unless explicitly selected. Keep reports concise, ranked, and useful for the next
decision.
