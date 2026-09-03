# Class properties

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing class-property syntax, storage, inheritance/narrowing, cardinality,
> defaults, `RequirementGroup`, printed tags, or a property-backed scalar.
>
> **Skip when:** changing component state or Type dependencies; class properties are immutable
> Class facts.
>
> **Status:** core mechanism implemented. Later sections distinguish settled rules from open
> extensions; proposed syntax is illustrative unless marked current.

## Read only the relevant sections

| Task | Read |
| --- | --- |
| Parse/store/read a property | Current model through Reading and evaluating |
| Decide where a property applies | Applicability is primarily structural |
| Cost, optional values, or groups | Total values; Cardinality types; Requirement versus RequirementGroup |
| Inheritance/default behavior | Narrowing and inheritance; Abstract defaults are not overrides |
| Printed tags or instruction collections | `Instruction*` and printed tags |
| Add a new property capability | Design constraints for future extensions |
| Judge whether properties are worth their cost | Why class properties earn their cost |

## Source map

- [`Property.kt`](../../src/common/dev/martianzoo/pets/ast/Property.kt) and
  [`PropertyValue.kt`](../../src/common/dev/martianzoo/pets/ast/PropertyValue.kt) —
  inspect AST forms.
- [`ClassDeclaration.kt`](../../src/common/dev/martianzoo/pets/data/ClassDeclaration.kt)
  — search for `properties` for stored declarations and defaults.
- [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt) — search for
  `private fun resolveProperties` for validation and inheritance behavior.
- [`PropertyTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/PropertyTest.kt)
  — read when behavior crosses Pets declarations and Terraforming Mars content.

Class properties record immutable facts about a Class. They are not fields on component
occurrences: every component of one concrete Type sees the same class-property facts. A class
property may also be read through `Class<X>`, so the fact does not require an X component to exist.

## Current model

A class body declares lowerCamelCase class-property names in a namespace separate from Class Names:

```pets
ABSTRACT CLASS MarsArea {
  row = Number
  column = Number
}
CLASS Tharsis_2_2 : MarsArea {
  row = 2
  column = 2
}
```

The implemented value families are:

| Declaration | Meaning | Concrete form |
| --- | --- | --- |
| `Number` | non-negative, world-independent integer | `cost = 10` |
| `Metric` | world-dependent numeric expression; `Number` narrows it | `score = COUNT "TemperatureStep"` |
| `Requirement` | one required game condition | `requirement = HAS "3 ScienceTag"` |
| `Requirement?` | currently, an absent or present Requirement | the declaration may be omitted by a concrete descendant |

The Kotlin AST names are `Property`, `PropertyName`, and `PropertyValue`.
`PropertyValue` is a `Specification`, so inheritance uses the same narrowing relation as types,
quantities, and instructions.

### Narrowing and inheritance

An ordinary class-property declaration narrows an inherited bound; it does not override a value:

```pets
ABSTRACT CLASS Scored { score = Metric }
ABSTRACT CLASS FixedScore : Scored { score = Number }
CLASS EightPoints : FixedScore { score = 8 }
```

Once a concrete value has been supplied, descendants cannot redeclare it. The same fact
coalesces when inherited through multiple paths. Distinct origins and divergent narrowing paths
conflict even when their printed values happen to match. Every non-cardinality bound on a
concrete Class must have a concrete value.

This rule is intentionally stronger than conventional object-oriented property overriding. A class
property is a fact about the Class, and inheritance accumulates and narrows facts.

### Reading and evaluating

A numeric class-property read is a Metric:

```text
ColonizerTrainingCamp.cost
Class<ColonizerTrainingCamp>.cost
CardFront(HAS 20 cost)
```

The first two forms have the same lookup meaning. An unqualified class property inside a refinement
receives the candidate Type as its receiver.

A stored Metric or Requirement is quoted to distinguish its inert syntax from the surrounding class
body; the quotes do not make it text. A class effect expands the parsed syntax explicitly:

```pets
This:: Result / EVAL This.score
This:: (EVAL This.requirement: Ok)
```

Expansion substitutes the concrete receiver for `This`, supplies the effect's contextual Owner,
and then applies the normal defaults and lowering. Expansion may wait until trigger matching has
specialized an abstract receiver. `EVAL` is invalid in an arbitrary count query.

## Why class properties earn their cost

**Disposition: at peace with it.** Do not report the property mechanism as removable complexity.

A count of declaring classes makes properties look lightly used: `Number` has three declarations,
`Requirement?` three, `Requirement` only `Milestone`, and `Metric` only `Award`. That measurement is
misleading. Without class properties the same immutable Class facts had to be obtained by major
cheats, which made the project's claims about Pets farcical — the language could not state a fact
the game plainly prints on a card.

Declaring-class count is therefore the wrong measure for this mechanism. The right questions are
whether a property kind expresses a printed fact honestly and whether reading it composes with
ordinary Pets. Both hold today.

Growth is expected rather than suspicious: [TURMOIL.md](TURMOIL.md#open-language-and-modeling-questions)
wants instruction-valued properties for party ruling bonuses. Judge a proposed new kind by
“Design constraints for future extensions” below, not by how many classes will declare it.

## Applicability is primarily structural

A class property should be declared at the highest Class for which asking the question makes sense,
not at a broader Class with a dummy value.

`row` and `column` belong to `MarsArea`, not `Area`. Phobos Space Haven's derived
`PhobosSpaceHaven_RemoteArea : RemoteArea` therefore has no such class properties. Asking for
`PhobosSpaceHaven_RemoteArea.row` is a nonsense question and fails
because the class property does not exist; it does not return zero or an absent value.

Use a cardinality type only when the question applies to every member of the declaring Class but a
value is legitimately optional. Do not use cardinality to compensate for a property declared too
high in the hierarchy.

## Total values and the ruling on card cost

Absence and zero are distinct in general. They coincide only when the domain concept itself is
total and zero completely describes the behavior.

`cost` is total across `CardFront`. Corporation and Prelude fronts cost zero: the player acquires
them without paying anything. A hypothetical `NonProjectCardFront` would naturally narrow its cost
all the way to zero. Project cards still state their cost explicitly, including the four current
zero-cost project cards; there is no useful project-card default of zero.

Therefore `CardFront.cost` remains `Number`, not `Number?`. This also keeps payment and numeric cost
filters within one numeric model.

There is no global rule that an absent numeric class property means zero. If `Number?` is eventually
introduced, its empty case supplies no Number and hence no Metric: it cannot count anything. A
present zero must remain observably different from absence. In particular, a future bare
`HAS optionalNumber` must test cardinality, not numeric positivity.

## Cardinality types, not null values

The preferred model has no null class-property value. A suffix describes how many values a class
property holds:

- `T?` means zero or one T values.
- A possible `T*` means zero or more T values.

The empty case is an empty cardinality, not a distinguished value inhabiting T. This matters for
lookup, narrowing, defaults, queries, and evaluation. The current specialized implementation uses
`OptionalRequirementType` as the bound and `AbsentRequirementValue` as the effective value on a
concrete Class; those are implementation vocabulary, not the intended general domain model.

Cardinality queries should observe the number of held values. Bare `HAS property` means at least one
value. If a cardinality can exceed one, `HAS 2 property` naturally means at least two. These are
presence/cardinality queries, not numeric reads of the held value.

### `Requirement?` and directional valence

Requirement was deliberately named for its game meaning rather than the neutral mathematical word
Predicate. Requirements combine directionally: comma-separated requirements all apply. The empty
conjunction is satisfied, so evaluating zero held Requirements naturally produces `Ok`:

```text
EVAL zero requirements       => Ok
EVAL one requirement         => that requirement
EVAL several requirements    => all of them
```

That identity does not make cardinality irrelevant. Cutting Edge Technology discounts a card
because a printed requirement is present; milestones and awards likewise count cards that have
printed requirements. Replacing absence with a stored `Ok` would erase a fact those rules observe.
A present requirement that happens to be satisfied—or even logically trivial—remains present.

The current implementation captures the important behavior:

- an absent `Requirement?` may remain on a concrete Class;
- `EVAL` of the empty case lowers to the always-satisfied `Component` requirement;
- bare `HAS requirement` distinguishes empty from present.

It does so with a specialized absent/present representation and a 0/1 Metric interpretation. That
is not yet the general cardinality model described here and must not be generalized by treating
empty values as numeric zero.

## Requirement versus RequirementGroup

**Open design direction.** Requirements may deserve the same distinction now made between
`Instruction` and `InstructionGroup`. Today `Requirement.And` makes a comma-separated source such as

```pets
EarthTag, JovianTag, VenusTag
```

one Requirement syntax tree containing three conjuncts. A future `RequirementGroup` could instead
hold three independent Requirements, with empty and singleton groups collapsing in source just as
instruction groups do.

That would make the cardinality interpretation literal. A card could hold zero or more printed
Requirements, `HAS requirement` would ask whether it has any, and `HAS 2 requirement` would ask
whether it has at least two. It would also make conjunction's empty identity structural instead of
encoding it as a special always-satisfied Requirement.

Questions to settle before changing the AST include:

- whether card fronts should declare `requirement = Requirement*` rather than `Requirement?`;
- whether cardinality counts top-level comma-separated requirements, including `(A OR B), C` as two;
- whether order and duplicates are significant in a RequirementGroup;
- how transforms, linking, rendering, and narrowing act on a group;
- whether `Requirement` remains the type of one atomic or possibly `OR`-composed condition.

Do not document this group model as implemented until those questions are resolved.

## Abstract defaults are not overrides

**Settled principle, unimplemented mechanism.** Because concrete Classes are final, an abstract
Class may safely offer descendants a default class-property value. A concrete descendant may accept
that default or state another permitted value explicitly. Once the concrete Class's effective value
is chosen, it remains final.

This is not conventional overriding. A default is a fallback used only when a descendant makes no
choice; it is not an inherited concrete fact that is later replaced. The declaration model must
distinguish:

- a bound that requires every concrete descendant to choose explicitly;
- a default that supplies the choice when a descendant is silent;
- a final fact that every descendant inherits unchanged.

Project-card cost illustrates the first case: each project card should state its cost, and the four
zero-cost cards should explicitly state zero. A family whose members share a normal value
might use a default. A family whose value is definitionally fixed should narrow to a final fact.

Syntax and multiple-inheritance rules remain open. In particular, competing defaults, nearer
defaults, and an abstract descendant replacing an ancestor's default need one systemic rule before
the feature is implemented.

## `Instruction*` and printed tags

**Exploratory direction.** A zero-or-more Instruction class property could store the recipe for
materializing a card's printed facts:

```pets
tags = Instruction*
```

A concrete card might then hold instructions that gain its printed Tag components when the front
comes into existence. Because the instructions belong to the card Class, they would also be
available through `Class<CardFront>` before a live CardFront component exists. This could subsume the
current Kotlin metadata bridge that handles card tags during play.

The direction is promising but not yet a design. It must answer:

- whether an instruction-valued property represents behavior or immutable printed data;
- how a query asks for a particular tag without executing the instructions;
- how duplicate printed tags are represented and counted;
- whether order matters, given that `InstructionGroup` is ordered while tags are not;
- how `This`, Owner, defaults, and trigger-time specialization are contextualized;
- whether `Instruction*` is a group value, a cardinality-bearing property, or both.

The goal is not merely to move `HandleCardTags` into generated Pets. The result should provide one
source of printed tag facts that supports both pre-existence queries and live materialization.

## Design constraints for future extensions

Any extension should preserve these rules:

1. Class properties are immutable facts about Classes, never occurrence state.
2. Put a property only where the question applies; prefer hierarchy over sentinel values.
3. Totalize a domain concept only when no actionable distinction is lost, as with card cost.
4. Empty cardinality is not a null value and is never globally coerced to zero or `Ok`.
5. Type-specific identities may govern evaluation without erasing observable cardinality.
6. Bounds, defaults, and final facts are distinct declaration concepts.
7. Stored syntax is evaluated explicitly and in the concrete Class context.
8. New group or cardinality machinery must replace special cases rather than coexist as a parallel
   representation of the same fact.
