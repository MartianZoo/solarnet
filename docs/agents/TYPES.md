# Pets types: what the specifications do not own

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** you need Pets type behavior that reaches outside `dev.martianzoo.pets.types`.
>
> **Read first:** two specifications already own most of this subject, and neither is restated here.
> [`docs/type-system-spec.md`](../type-system-spec.md) is the authority for the type system itself —
> classes, dependencies, class literals, types, subtyping, bounds, refinements, class properties,
> defaults, enumeration, inhabitance, and Type variables. [`docs/pets-language-spec.md`](../pets-language-spec.md)
> is the authority for the language — declarations, expressions, requirements, metrics, instructions,
> narrowing, effects, actions, transform blocks, owner-local Classes, and elaboration, including how
> an authored instruction receives its defaults. Rules are numbered `T4-2` and `L5-2` and each is
> checked by the identically named test under `test/common/dev/martianzoo/pets/`.
>
> **Status:** the residue — the parts of the Pets type story that other modules own.

## Where each concern lives

| Concern | Source of truth |
| --- | --- |
| Classes, Types, dependencies, refinements, Type variables, uninhabited Types | [`type-system-spec.md`](../type-system-spec.md) |
| Inline derived Classes, and default insertion into instructions | [`pets-language-spec.md`](../pets-language-spec.md) sections 11 and 12 |
| Which Classes a premise selects | [`GamePremise.kt`](../../src/common/dev/martianzoo/state/GamePremise.kt) and tests in `PremiseSelectionTest.kt` |
| Master Class identity versus game-filtered enumeration | [`CLASS_TABLES.md`](CLASS_TABLES.md) |
| Class-property cardinality, groups, and direction | [`PROPERTIES.md`](PROPERTIES.md) |
| Lexical ownership, Actor attribution, delegated narrowing | [`IDENTITY.md`](IDENTITY.md) |
| `EACH` fanout | [`EACH.md`](EACH.md) |
| Gain/removal counts, AMAP, abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) |
| Trigger and Actor specialization at runtime | [`ENGINE.md`](ENGINE.md) |

## 1. Type-variable lifetime outside resolution

*Type-system-spec section 13 defines what a Type variable is, where one is scoped, and what
binding does, and language-spec L3-8 says what narrowing one requires. These are the engine-facing
consequences.*

Class-scoped variables survive inheritance and enumeration. Component specialization substitutes
only their recorded uses in Effects; an unrelated occurrence of the same Class remains an ordinary
bound. Effect-local variables stay open while the effect is installed, then a matching event
specializes their trigger, condition, Actor selector, and instruction together. This is trigger
specialization, not global replacement of every occurrence of the same abstract Class.

Attaching a class-header scope copies the source Effect before recording its resolved variables.
Catalogs can share authored declarations; those declarations must not retain a compiled universe
through a variable's bound Type or let interpretation in another Catalog overwrite an earlier scope.

Explicitly marked Action and `THEN` variables survive lowering and queuing. A sequence remains
joint while its first stage uses an unresolved shared choice, including in an observer; unrelated
earlier stages can run. Its continuation retains the authored scope. Concrete binding removes the
variable; partial binding restricts its domain and retains its identity. A represented-class choice
is settled when its root Class is concrete; its uses keep their own component arguments, and
class-literal uses project to the selected root Class. Whole-sequence partial
proposals preserve the corresponding markers at every surviving unresolved occurrence. Captures
follow dependency paths. Full-sequence proposals check `OR` arms jointly with the other stages;
first-stage-only selection requires compatible arms to agree on their bindings. Substitution is
simultaneous, followed by predicate checks. Optional quantities and
independent Types may remain open after shared values are selected.

Instruction selection uses `GroundType.ensureSelectionNarrows` through `TypeInfo`: an abstract
candidate retains its `HAS` predicates, while a concrete candidate can discharge them in the
selection world. Ordinary contextual Type narrowing retains the aggregate-query meaning in T8-2.
First-stage selection uses existing forced concrete-type resolution before binding a uniquely
determined Type. Resolution leaves a stage's shared observers intact until their choices are bound. It may reject
an impossible first change using the existing limiter over its domain with predicates removed;
that broader domain gives an upper bound without querying an unresolved observer.
The engine owns neither a blanket concreteness guard nor a card-specific exception.

A full transmutation likewise names a destination choice used by its source. Within a compact
atomic transmutation,
`Foo<Same, Here, To FROM From>` is compact syntax for
`Foo<Same, Here, To> FROM Foo<Same, Here, From>`; each unchanged argument occupies both roles and
is stored once. The gained and removed Types remain projections of that compact tree until
execution; no Type variable is involved.

The [`EACH`](EACH.md) fanout enumerates its selector. A marker on the selector explicitly makes each
selected concrete Type available through the same marker on its root in the body; other body
expressions retain their ordinary meanings. An explicitly marked `EACH Me@Player` rebinds the
lexical owner in its body; an unmarked selection retains the enclosing `Me`. `This` is the effect-bearing component.
The body need not use the selection.

## 2. Implementation direction for Type-variable identity

Use one authored-occurrence model and shared matching primitives with the scope rules above. Do not
assume that Class-header specialization, trigger capture, and `THEN` continuation need one
recognition algorithm: they receive values from different events and may cleanly retain separate
policies. Consolidate a policy only when its declaration, scope, and binding rules are actually the
same.

When use-specific defaults expand a recorded occurrence, narrowing recognizes the expanded form by
its unchanged dependency-key assignments. This preserves the authored variable through elaboration
without adding occurrence tokens or provenance to `Expression`.

For binding's predicate-consumption rule, see
[T13-10](../type-system-spec.md#13-type-variables).

The card-owned `Splicer<SpliceTacticalGenomics>` component is a working content mechanism, not
unfinished Type-variable infrastructure. Further changes to its ownership or task assignment would
be optional content cleanup.

## 3. `glb` reports disjointness and inexpressibility the same way

T7-1 now distinguishes them: `Tharsis_2_2 ⊓ Tharsis_2_3` is absent because nothing could be both,
while `Tile ⊓ Owned` with rival subclasses is absent only because no declared class names the
overlap. `ClassTable.glb` returns null for both, and callers treat both as errors.

Changing the return type to tell them apart would touch every caller, for an error message. Revisit
only when a real diagnosis needs it — the honest first step would be a separate query, not a new
result type for `glb`.
