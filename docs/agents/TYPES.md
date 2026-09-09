# Pets types: what the specification does not own

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** you need Pets type behavior that reaches outside `dev.martianzoo.pets.types`.
>
> **Read first:** [`docs/type-system-spec.md`](../type-system-spec.md) is the authority for the type
> system itself — classes, dependencies, class literals, types, subtyping, bounds, refinements, class
> properties, defaults, enumeration, inhabitance, and Type variables. Every rule there is numbered
> `n-m` and checked by the identically numbered test in
> `test/common/dev/martianzoo/pets/types/Spec*.kt`. Do not restate those rules here.
>
> **Status:** the residue — the parts of the Pets type story that other modules own.

## Where each concern lives

| Concern | Owner |
| --- | --- |
| Classes, Types, dependencies, refinements, Type variables, uninhabited Types | [`type-system-spec.md`](../type-system-spec.md) |
| Known-wrong type behavior | `test/common/dev/martianzoo/pets/types/BugsTest.kt` |
| Which Classes a premise activates | [`OPTIONS.md`](OPTIONS.md#projection-closure); tests in `ActivationTest.kt` |
| Master Class identity versus game-filtered enumeration | [`CLASS_TABLES.md`](CLASS_TABLES.md) |
| Class-property cardinality, groups, and direction | [`PROPERTIES.md`](PROPERTIES.md) |
| Contextual `Owner`, Actor attribution, delegated narrowing | [`IDENTITY.md`](IDENTITY.md) |
| `EACH` fanout | [`EACH.md`](EACH.md) |
| Gain/removal counts, AMAP, abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) |
| Trigger and Actor specialization at runtime | [`ENGINE.md`](ENGINE.md) |

## 1. Owner-local derived Classes

*Card-definition syntax, lowered before the Class Table is built. The type system sees only the
generated declarations.*

A card definition can declare a component Class at its point of use without choosing its canonical
name explicitly. For example, a card instruction can gain `RequiredAction { -> 3 ProjectCard }`, or use
`CityTile<RemoteArea {}>`. Card-definition construction lowers these to declarations with
stable owner-derived names such as `Inventrix_RequiredAction` and
`PhobosSpaceHaven_RemoteArea` before building the Class Table. They have exactly the existing Class
and component semantics; there is no runtime anonymous identity.

The body follows the complete expression. For example,
`SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {}` becomes the use-site expression
`MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>` and declares its superclass as
`SpecialTile<LandArea>`. Arguments therefore specialize both the occurrence and the generated
Class's superclass. Refinements constrain only the occurrence and are removed recursively from the
declared superclass because refinement types cannot be supertypes.

The local body may contain invariants, properties, effects, and actions. It may not contain
`DEFAULT` clauses or nested Class declarations. The generated Class inherits applicable defaults
from its supertypes.

Use this for a Class local to one definition, especially required actions, temporary effects,
special tiles, and remote areas. A shared Class, a Custom implementation, or a component with
several semantic roles should remain explicit. Multiple local Classes with the same natural suffix
must be declared explicitly rather than distinguished by an ordinal or hash.

Another expression that needs the exact derived Class may use its assigned canonical name. Writing
the superclass without a local body still means the whole abstract family; it does not implicitly
resolve to the local subtype. Existing implicit Type-variable rules continue to interpret repeated
abstract dependencies inside the derived Type as uses of one declaration.

This syntax is available only in card-definition expressions. Ordinary Class declarations reject
it. Manually submitted instructions are still parsed and validated, then rejected with
`NoNewClassDeclarationsException` because the live game's Class Table is frozen.

## 2. Inserting defaults into instructions

*The type system decides what a class's defaults **are** (spec section 10). These rules decide when
an authored instruction **receives** them.*

Inside a refinement, an implicit default is deferred when its dependency is a direct use of a
Class-header Type variable; candidate substitution can then bind it through that occurrence.
Writing `<>` still explicitly accepts the default.

A bare dependent expression in a `HAS` refinement likewise reserves its first dependency position
that can accept the refined domain. Candidate substitution binds that position before defaults are
considered, so `Player(HAS StartToken)` tests `StartToken<p>` for each candidate `p` even though
`StartToken` inherits the contextual `Owned<Owner>` default. The explicit forms remain available:
`StartToken<Owner>` requests the contextual owner, and `StartToken<>` explicitly accepts the
default.

A gain or removal that would receive dependency bounds from its use-specific default cannot leave
its argument list implicit. It must supply at least one argument or write an empty list such as
`OceanTile<>` to explicitly accept those bounds. The gain and removal halves of `A FROM B` are
checked independently. This rule does not apply to all-use dependency defaults or to Quantifier
defaults. An explicit empty list is invalid when the dependency-default set for that use is empty;
it cannot serve only to give an expression a different authored spelling.

## 3. Type-variable lifetime outside resolution

*Spec section 13 defines what a Type variable is, where one is declared, and what binding does.
These are the engine-facing consequences.*

Class-scoped variables survive inheritance and enumeration. Component specialization substitutes
only their recorded uses in Effects; an unrelated occurrence of the same Class remains an ordinary
bound. Effect-local variables stay open while the effect is installed, then a matching event
specializes their trigger, condition, Actor selector, and instruction together. This is trigger
specialization, not global replacement of every occurrence of the same abstract Class.

Action and `THEN` variables survive lowering and queuing. An open variable prevents the relevant
stages from splitting into independent tasks until an earlier choice supplies its value. Within one
atomic transmutation, `Foo<Same, Here, To FROM From>` is compact syntax for
`Foo<Same, Here, To> FROM Foo<Same, Here, From>`; each unchanged argument occupies both roles and
therefore uses one atomic variable.

The [`EACH`](EACH.md) fanout makes its selector a declaration whose scope is its body. Each
enumerated concrete selector Type substitutes through the recorded use paths. Inside the body, an
Owner selection supplies contextual `Owner`; a non-Owner selection retains the enclosing contextual
owner. `This` is the effect-bearing component. The construct rejects a body with no use of the
selector.

## 4. Implementation direction for Type-variable identity

Use one authored-occurrence model and shared matching primitives with the scope rules above. Do not
assume that Class-header specialization, trigger capture, and `THEN` continuation need one
recognition algorithm: they receive values from different events and may cleanly retain separate
policies. Consolidate a policy only when its declaration, scope, and binding rules are actually the
same.

Stable authored-occurrence paths may eventually replace the current fallbacks to expression
identity or equality after transformations. That is a possible mechanism, not an accepted next
step. No known card, rule, or normal engine operation currently demonstrates that those fallbacks
produce wrong behavior.

Do not add occurrence tokens, `Expression.Linkage`, or cross-pipeline provenance propagation from
this design description alone. First demonstrate a focused failure through normal Pets elaboration
or game execution. A synthetic test whose only contract is preserving a proposed identity
representation is not sufficient evidence. Any solution must also show why a smaller correction to
the affected construct's existing recognition policy cannot preserve the real behavior.

A rejected implementation is preserved locally as stash
`codex/type-variable-linkage-review-2026-09-02` (stash commit
`8f2c9617401d3d630097fa52209e46a586930194`). Inspect it before revisiting this mechanism. It added
217 net lines across the expression model, preprocessing, scope analysis, engine resolution, and
construct-specific lowering without establishing an observable failure. The stash records the
cost and explored failure modes, not a design to restore wholesale; because Git stashes are local,
the evidence gate above remains authoritative when the object is unavailable.

The card-owned `Splicer<SpliceTacticalGenomics>` component is a working content mechanism, not
unfinished Type-variable infrastructure. Further changes to its ownership or task assignment would
be optional content cleanup.
