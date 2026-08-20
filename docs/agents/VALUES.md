# Project values

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
- Greenery-to-oxygen lives intrinsically on `GreeneryTile`; solo setup owns its narrow cancellation
  after each neutral greenery. `PharmacyUnion` is the opposite kind of exception: its published rule
  genuinely needs special treatment.
- Prefer hand-authored Pets plus general runtime semantics. A custom instruction is honest when it
  bridges metadata or a capability Pets does not have; Kotlin-generated Pets is not automatically
  simpler.
- Verify disputed data and rules against primary evidence. For a Terraforming Mars ruling, only a
  post by Jacob Fryxelius is authoritative; rulebooks and physical components remain primary evidence
  for their printed content.

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

## Keep Pets central

- Pets should read like the physical game: compact, composable, and precise about ownership,
  identity, timing, and choice.
- Components have types and multiplicity, not fields or incidental object identity. Add another
  representation only when the rules truly distinguish it.
- An Authority supplies coherent data. Modules select ambient rules. A GamePremise describes one
  exact game. Do not blur these roles.
- Load only the vocabulary a game needs. Mentioning an inactive optional type in a safe query must
  not activate its expansion.

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
