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

Completeness is not a project goal. Design cleanup matters more than forcing every official rule
into the model.

When exact fidelity would require disproportionate or poorly understood machinery, select the
clearest coherent variant the model can support and document the difference from the official
rule. A variant is a deliberate rule, not a label for accidental behavior. Do not misrepresent it
as exact.

Cards and expansions are valuable primarily because varied rules test the model. A component may
show that existing concepts compose, reveal a missing general rule, or identify an honest special
case. Machinery serving only a few marginal components deserves explicit investigation, including
whether dropping those components materially simplifies the model.

Keep rules with the game component that owns them. Use a cross-cutting system component only for a
genuinely ambient or switchable rule. When the user explicitly requests Terraforming Mars rule
research, use rulebooks and physical components as primary evidence and verify disputed rulings
against a post by Jacob Fryxelius.

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

> **Recurring failure warning:** If a card or rule appears to need custom Kotlin, a custom
> instruction, or a component-specific gameplay helper, stop before implementing it. Name the
> general capability ordinary Pets lacks, and first try removal or composition of existing
> mechanisms.

Pets should read like the physical game: compact, composable, and precise about ownership,
identity, timing, and choice. Authored source data may lower into Pets, but there must not be a
parallel declaration of the same content. Once lowered, execution proceeds through Pets semantics.

Make a genuine attempt to express behavior in ordinary Pets. Bounded custom instructions, metrics,
and classes are acceptable when they keep the general language and engine smaller. They must state
an honest exception at the semantic seam, not smuggle card knowledge into orchestration.

Ordinary production Kotlin outside those extension points must not branch on particular cards,
components, expansions, or setting vocabulary. `TfmGameplay`, initializers, phase drivers, and
similar integration code must not repair or coordinate individual content. If behavior cannot be
expressed in raw Pets or a bounded custom semantic extension, reconsider the behavior, the
component's inclusion, or the model before adding orchestration.

Prototype code is not exempt. Mirrored semantic state, a privileged runtime path, or domain
knowledge outside declared lowering and custom-extension points is evidence against the model.

Components have types and multiplicity, not fields or incidental object identity. A Catalog
supplies coherent data, Modules select ambient rules, and a GamePremise describes an exact game.
Do not blur those roles or activate optional vocabulary merely by mentioning it in a safe query.

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
