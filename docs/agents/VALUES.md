# Project values

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** designing, implementing, or reviewing a behavior or architecture change,
> especially when fidelity, generality, completeness, and conceptual cost compete.
>
> **Skip when:** performing a mechanical, behavior-preserving edit whose ownership is settled.
>
> **Status:** durable working rules. Project priorities and future programs belong in
> [`SOLARNET_ROADMAP.md`](../../SOLARNET_ROADMAP.md) and
> [`PETS_ROADMAP.md`](../../PETS_ROADMAP.md).

Repository-level instructions in [`AGENTS.md`](../../AGENTS.md) remain authoritative.

## What Solarnet is for

Solarnet is a playground for exceptional software and language design. Terraforming Mars is the
demanding subject through which the project can discover and demonstrate a small executable
algebra of rules. Correct behavior is necessary, but an implementation that relies on incoherent
exceptions, mirrored models, privileged integration paths, or disproportionate machinery is still
a design failure.

Solarnet is a calculator, including during adversarial play. For Actor attribution, a request to
select, narrow, or perform FooPlayer's task through FooPlayer's Agent is sufficient; the engine
still validates task assignment and the modeled game rules. It does not establish who operates
that Agent, whether FooPlayer approved the decision, or whether anybody accepts the resulting
history. Local exploration may use every Actor's Agent and correction facilities freely.
[ADVERSARIAL.md](ADVERSARIAL.md) assigns authentication, hidden-information custody, and acceptance
of shared play to the surrounding arrangement. Those concerns do not justify engine restrictions.

The project should make its libraries useful to independent builders without designing for
hypothetical clients. A real second use can reveal a better responsibility split; an imagined use
does not justify flexibility, compatibility, or a framework.

## Let libraries attest to the design

Use library divisions when they make responsibilities and semantics clearer.

- Give each library an intelligible responsibility and a small, expressive contract.
- A caller should depend only on capabilities it uses. Every module dependency must follow from
  the depending module's responsibility.
- Prefer directional dependencies. Avoid ambient initialization, shared global state, and
  assumptions that unrelated application layers are present.
- Test modules independently and test meaningful compositions across them.
- Change an interface when doing so improves the design; there are no compatibility clients to
  preserve.
- Keep cohesive behavior together. Module count is not a quality measure.

Composition does not require broad abstraction. Separate the capabilities Solarnet actually has,
then connect their honest contracts. Do not add flexibility for unrelated games, hostile callers,
or imagined performance needs.

## Model the game honestly

Apply the [Pets modeling values](../../pets/docs/agents/VALUES.md#model-the-game-honestly) to
engine changes as well as declarations. Runtime code must not compensate for a poor model.

## Keep concepts few and ownership precise

- First ask what can be removed, then whether existing Pets and domain mechanisms compose cleanly.
- Prefer a single source of truth and a systemic rule over wrappers, duplicated representations,
  parallel APIs, and component-specific exceptions.
- A hardcoded narrow fact can cost less than a framework. Repeated implementation-shaped
  exceptions can instead indicate that a general concept is missing.
- Stop when a small request starts creating vocabulary across several modules. Explain the design
  pressure instead of normalizing disproportionate complexity.
- Evaluate each layer against the contract it owns. Lower layers preserve facts, validate legal
  mutations, and calculate consequences; caller policy and strategy belong above them.
- Do not push application preferences downward to guarantee a pleasant default, and do not omit a
  lower-layer invariant because an upper layer currently behaves well.
- Treat broad cross-module pressure as evidence about the model. Several awkward local mechanisms
  may be symptoms of the same missing rule.

## Keep Pets central

Apply the [Pets modeling values](../../pets/docs/agents/VALUES.md#keep-pets-central) to
engine changes as well as declarations. Runtime code must not compensate for a poor model.

## Keep interfaces and evidence honest

- Use small, typed APIs and the narrowest visibility consistent with their responsibility.
- Preserve engine invariants even for trusted or rules-bypassing operations.
- Domain input must fail with domain errors. Programmer-error exceptions indicate invalid Kotlin
  or an impossible engine state.
- Prefer readable scenario and integration tests that demonstrate observable behavior and library
  composition. Do not duplicate production catalogs or assert incidental task text and ordering.
- Focused scenarios and source-backed replays are complementary. Scenarios explain rules; replays
  prove that the model survives their interaction.
- Preserve original replay evidence and make corrections visible. The observed execution must
  genuinely follow from the declared model.
- A passing narrow test proves only its assertion. Review the final diff and state what was not
  verified.
