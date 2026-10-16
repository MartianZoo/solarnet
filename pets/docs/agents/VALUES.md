# Pets modeling values

> **Status:** durable modeling rules. [AGENTS.md](../../AGENTS.md) applies to this repository.
> Keep the static model usable without Solarnet runtime dependencies.

## Model the game honestly

**Assume client-side pruning, or accept dead ends.** Every Pets and engine design must follow
the [decision-tree principle in AGENTS.md](../../AGENTS.md).
The player's agent or client either presents only choices known to permit turn completion, or
exposing unsuccessful paths is acceptable. Missing pruning does not create a model requirement.
Protected Habitats may let an opponent attempt a plant removal and then dead-end the turn,
requiring rollback. That is valid rule enforcement, not a modeling defect or a fidelity compromise.

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

## Keep Pets central

Pets needs only the expressive power required by concrete Canon behavior. Unused combinations may
be rejected explicitly; do not add representations or processing machinery merely to make the
language general, robust against arbitrary inputs, or closed under every syntactic composition.

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
