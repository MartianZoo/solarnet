# The Pets language: a specification

This document defines the Pets language as implemented in `dev.martianzoo.pets`. It is meant to be
readable start to finish, but it is organized so you can look one rule up and stop.

## How to read this

The specification is divided into **sections**, numbered 1 to 12. Each section states a series of
numbered **rules**. A rule is written `L4-2` — `L` for the language, then "section 4, rule 2" — and
it is the unit you cite. Its peer document, [the type system specification](type-system-spec.md),
numbers its rules `T4-2` the same way, so one rule id belongs to exactly one document.

Every rule is checked by tests whose names begin with the same id, in
`test/common/dev/martianzoo/pets/`:

| Section | Test file |
| --- | --- |
| 1. Source and declarations | `Lang01SourceTest.kt` |
| 2. Names | `Lang02NamesTest.kt` |
| 3. Expressions | `Lang03ExpressionsTest.kt` |
| 4. Requirements | `Lang04RequirementsTest.kt` |
| 5. Metrics | `Lang05MetricsTest.kt` |
| 6. Instructions | `Lang06InstructionsTest.kt` |
| 7. Narrowing | `Lang07NarrowingTest.kt` |
| 8. Effects | `Lang08EffectsTest.kt` |
| 9. Actions | `Lang09ActionsTest.kt` |
| 10. Transform blocks | `Lang10TransformsTest.kt` |
| 11. Owner-local classes | `Lang11OwnerLocalClassesTest.kt` |
| 12. Elaboration | `Lang12ElaborationTest.kt` |

So `grep -rn "L6-9" docs/pets-language-spec.md test/common/dev/martianzoo/pets/` finds a rule and
everything that proves it. Known departures from these rules have passing characterizations in
`LangBugsTest.kt`; each is flagged where it belongs and listed again in the appendix.

Examples use real Terraforming Mars component names — `GreeneryTile`, `Plant`, `OceanTile` — but the
declarations shown are simplified. They illustrate a rule; they are not a transcript of `tfm-canon`.

### What Pets is

Pets is a **declarative specification language**. A `.pets` source declares what components may
exist and what rules hold of them. There are no programs, no statements and no entry point — only
specifications.

Its subject matter is game states. Everything the language can say is one of two things:

- a **query** over a single state. A requirement asks a yes-or-no question of one state; a metric
  computes a number from one state.
- a **relation between two states**, a before and an after. An instruction says how the after should
  differ from the before: `2 Plant<Player1>` denotes the relation "the after-state holds two more
  Player1 plants than the before-state".

Temporality is part of that subject matter rather than an artifact of any machine. `A THEN B` says
that the change A denotes happens before the change B denotes; `EACH Player { Plant }` quantifies
over the players present in one state, exactly as a metric counts them.

### What this document does not cover

Two neighbours are deliberately out of scope.

**Types.** This document says how a type expression is *written* and where one may appear. What one
*means* — which components it admits, when one narrows another, how arguments match dependencies —
is the [type system specification](type-system-spec.md), cited here as `T5-2` and never restated.

**Realization.** An instruction denotes a relation between states. Bringing an after-state about is
a separate job, and every question about *how* that is done belongs to some other document:

| Question | Owner |
| --- | --- |
| How pending work is scheduled, identified and completed | [`ENGINE.md`](agents/ENGINE.md), [`SEQUENCING.md`](agents/SEQUENCING.md) |
| Who is asked to make an open choice, and how | [`ENGINE.md`](agents/ENGINE.md), [`AUTOEXEC.md`](agents/AUTOEXEC.md) |
| What count actually gets executed, and how `.` resolves | [`QUANTIFIERS.md`](agents/QUANTIFIERS.md) |
| Who acted, how a change is attributed, who may narrow a choice | [`IDENTITY.md`](agents/IDENTITY.md) |
| How `EACH` enumerates components of a live world, and when | [`EACH.md`](agents/EACH.md) |
| Action availability, costs, invoices and action identity | [`ACTIONS.md`](agents/ACTIONS.md) |
| The order in which independent effects fire | [`SEQUENCING.md`](agents/SEQUENCING.md) |
| The event log, causes and traces | [`ENGINE.md`](agents/ENGINE.md), [`DIAGNOSTICS.md`](agents/DIAGNOSTICS.md) |
| Which classes a particular game contains | [`OPTIONS.md`](agents/OPTIONS.md) |
| Which name a concept gets, and its localized display names | [`NAMING.md`](agents/NAMING.md) |
| Class-property cardinality, groups and printed tags | [`PROPERTIES.md`](agents/PROPERTIES.md) |

A rule here is a rule only if `modules/pets` can check it. Where a rule states a meaning that
reaches past what this module can observe, its test pins the part that is observable here, and the
row above says who owns the rest.

### Notation

| Written | Means |
| --- | --- |
| `CLASS Foo` | Pets source for a class declaration |
| `Foo<Bar>` | Pets source for an expression |
| `T5-2` | rule 2 of section 5 of the [type system specification](type-system-spec.md) |

A few terms are used precisely throughout:

- A **component** is one occurrence of a concrete type. A **state** is a multiset of components.
- A **world** is whatever can answer a query about a state — the `TypeInfo` and `GameReader`
  interfaces. Where a rule needs one, it says so.
- A **node** is any piece of Pets syntax. An **element** is one of the six major kinds an author
  writes: expression, requirement, metric, instruction, effect, action.
- **Authored** Pets is what a source contains. **Elaborated** Pets is what authored Pets becomes
  once a class table is in hand (section 12). Sections 4 to 9 describe both; section 7 applies only
  to elaborated instructions, and says so.
- **Rendering** is normalized, never verbatim. Whitespace is discarded, each node is written in its
  own canonical form, and enough parentheses are inserted that re-parsing the result yields the same
  node. "Enough" is not "the fewest": a renderer may add a parenthesis precedence does not require,
  and does — `Plant OR Heat THEN Steel` comes back as `(Plant OR Heat) THEN Steel`. Every rule that
  says an element *round-trips* means exactly this: rendering then re-parsing is the identity.

---

## 1. Source and declarations

A `.pets` source is a sequence of class declarations and nothing else. There is no top-level
instruction, no import, and no entry point: a source says which components may exist and what rules
hold of them, and stops.

**L1-1. A source is a sequence of declarations, in order.** `Parsing.parseClasses` returns one
`ClassDeclaration` per declared class, in source order. A source containing only whitespace and
comments declares nothing. A source whose final declaration is incomplete is rejected with
`PetSyntaxException`; there is no partial success.

**L1-2. A signature is a name, an optional dependency list, and an optional supertype list.**
`CLASS Foo` declares a concrete class and `ABSTRACT CLASS Foo` an abstract one (T2-1).

```pets
ABSTRACT CLASS Tile<Area> : Occupant, Owned<Owner>
```

**L1-3. One `CLASS` keyword may introduce several classes**, `CLASS Alpha, Beta`, but only when
there is no body. Each gets the same kind and docstring and its own signature. A comma inside a
supertype list continues that list rather than starting a new signature, so `CLASS Alpha : Root,
Beta` declares one class with two supertypes, and only the last signature of a group can name
supertypes.

**L1-4. A body is brace-delimited, and its elements are separated by newlines or by semicolons.** A
body element is an invariant (`HAS r`), a `DEFAULT` clause, a property assignment (`name = value`),
an effect, or an action. A newline-separated body may also contain nested declarations; a
semicolon-separated one may not.

```pets
CLASS GreeneryTile : Tile { HAS MAX 1 This; This: OxygenStep }
```

**L1-5. A nested declaration becomes a sibling that names its container as a supertype** (T2-2).
The container is returned first, then its nested declarations in source order, recursively.

**L1-6. A docstring is a quoted string on the line before `CLASS`.** It is retained on the
declaration (T2-1) and re-emitted when the declaration is rendered.

**L1-7. `DEFAULT` clauses name the class that declares them** (T10-3) and are merged into one set
per use kind (T10-1). A clause naming another class is rejected.

**L1-8. A property is assigned at most once per body.** `name = value` binds one class property;
the same name twice in one body is rejected. The right-hand side is one of the bound words `Number`,
`Metric`, `Requirement` and `Requirement?`, a literal non-negative number, a metric quoted after
`COUNT`, or a requirement quoted after `HAS`. Quotes may not appear inside the quoted text. What
these bounds and values mean is T9-1 and T9-2.

```pets
CLASS Ants : CardFront {
  cost = 9
  score = COUNT "Microbe<This>"
  requirement = HAS "4 OxygenStep"
}
```

**L1-9. Signature expressions carry no refinements**, at any depth. A dependency bound or supertype
written `Foo(HAS Bar)` or `Foo(NOT Bar)` is rejected, because a refined type cannot be a bound.

**L1-10. Whitespace and comments.** Horizontal whitespace is insignificant. `//` begins a comment
that runs to the end of the line. A backslash immediately before a line ending continues the line,
so one element may span several source lines. Newlines are significant only as separators (L1-1,
L1-4).

**L1-11. A declaration renders as parseable source, and round-trips.** `toString()` produces a
multi-line declaration and `toString(oneLine = true)` a semicolon-separated one; parsing either
yields an equal declaration.

**L1-12. A declaration can also be parsed on its own.** `Parsing.parseOneLinerClass` accepts exactly
one declaration, with an optional semicolon-separated body, and rejects owner-local class syntax
(L11-6). This is how a declaration embedded in structured card data is read.

**L1-13. Every catalog also receives the system declarations.** `systemClassDeclarations` supplies
the classes this specification and the type system depend on — `Component` and `Class` (T1-4, T1-5),
the ownership vocabulary `Anyone`, `Owner` and `Owned`, the actor root `Actor`, and the signals `Ok`
(L6-4) and `Die` (L12-14) — plus `Atomized` (L12-11) and `Custom` (T2-11). A catalog's own source is
loaded alongside them. Which of these a *game* then contains is `OPTIONS.md`'s question, not this
document's.

---

## 2. Names

**L2-1. A class name is UpperCamelCase, or an all-caps abbreviation.** After the first letter,
digits and underscores are allowed; a leading letter-plus-digits segment must be followed by another
capital. All-caps names are at most six characters. Formally:

```text
[A-Z]( [a-z_][A-Za-z0-9_]*
     | [0-9]+[A-Z][a-z_][A-Za-z0-9_]*
     | [A-Z0-9]{0,5} )
```

so `GreeneryTile`, `Tharsis_2_2`, `A_foo`, `L1TradeTerminal`, `MC` and `TR` are names, and `greenery`
and `Terraforming Mars` are not.

**L2-2. Keywords are reserved and case-sensitive.** `ABSTRACT`, `BY`, `CLASS`, `COUNT`, `DEFAULT`,
`EACH`, `EVAL`, `FROM`, `HAS`, `IF`, `MAX`, `NOT`, `OR`, `RANK`, `THEN` and `X`, together with the
property-value words `Metric`, `Number` and `Requirement`, are the words the grammar itself uses, and
none of them may be a class name. Because the reserved spellings are exact, `Max`, `By` and `Has` are
perfectly good class names.

**L2-3. A property name is lowerCamelCase**: a lowercase letter followed by letters and digits.

**L2-4. A transform-kind name is an all-caps word** (section 10).

**L2-5. There is one namespace and no scoping.** A name is not declared, bound or shadowed by any
construct in this document; it means whatever class the class table says it means (T1-1, T1-7).
Which name a concept should get, and how names are displayed to a person, is `NAMING.md`'s subject.

---

## 3. Expressions

An expression is the noun of the language. It appears in every other element, and everywhere it
appears it identifies a type.

**L3-1. An expression is a class name, an optional argument list, and an optional refinement.** Each
argument is itself an expression. What the resulting type is, and how arguments match dependencies,
is T5-1 and T3-5.

**L3-2. Writing an empty argument list is not the same as writing none.** `GreeneryTile<>` and
`GreeneryTile` denote the same type (T5-2), but the two spellings are distinguishable, and section
12 gives the difference its meaning: `<>` says "I accept this use's defaults on purpose" (L12-5,
L12-7).

**L3-3. There are two refinements, and an expression carries at most one.** `(HAS r)` refines by a
requirement and `(NOT x)` by a structural difference; T8-1 through T8-12 say what each means.

**L3-4. A class literal is written with one bare class name**, `Class<Steel>` (T4-1, T4-6).

**L3-5. `This` names the component the enclosing declaration is about.** It is an expression like
any other and may take arguments: `This<Foo>` keeps the arguments and adopts the context's class.
Elaboration replaces it (L12-2). `This` is a placeholder rather than a class, so it has no defaults
of its own and an empty argument list on it accepts nothing: `This<>` *is* the bare placeholder.
Every construct that recognizes the placeholder recognizes both spellings, even though the two are
different expressions (L3-2).

**L3-6. `Owner` in an authored expression is contextual.** It stands for whoever supplies the
context, and elaboration replaces it (L12-3). `Anyone` is an ordinary class and stands for itself
(T3-4).

**L3-7. An expression renders as the class name, the argument list if one was written, and the
refinement.** Whitespace is not preserved, but nothing else is normalized away: an authored
expression is not rewritten into its type's minimal form, so `Tile` and `Tile<Area>` remain distinct
expressions even though they resolve to one type (T1-3, T5-5).

**L3-8. Two expressions are equal when their spellings agree.** Argument order is part of the
spelling, so `Microbe<Player1, Ants>` and `Microbe<Ants, Player1>` are different expressions for one
type. This is why the type system, not the syntax, is the authority on identity (T5-1).

---

## 4. Requirements

A requirement denotes a yes-or-no query over one state: `MAX 4 OxygenStep` asks whether the oxygen
track is at 4 or below.

**L4-1. A requirement is evaluated from the values of the metrics it names.** `isMetBy` takes a
function from metric to count and returns the answer; nothing else about the state is consulted.
That function is the only place a world enters.

**L4-2. There are three counting forms.** `n M` holds when M's value is at least n, `MAX n M` when
it is at most n, and `= n M` when it is exactly n. An omitted count is 1, so `Plant` means "at least
one plant" and `MAX 0 Tile` means "no tiles at all".

**L4-3. A minimum of zero is rejected.** `0 Plant` is not a requirement — it would ask nothing —
while `MAX 0 Plant` and `= 0 Plant` are the useful ways to say "none".

**L4-4. The target is independent of the metric's own scaling.** `MAX 2 (3 Plant)` compares 2
against the value of `3 Plant`, which counts complete groups of three (L5-3). Six plants meet it and
nine do not.

**L4-5. A counting requirement takes one metric atom.** A metric union or subtraction must therefore
be parenthesized where a requirement counts it: `9 (Plant - Steel)`.

**L4-6. `,` is conjunction and `OR` is disjunction, and `OR` binds tighter.** So `a, b OR c` requires
`a`, and one of `b` or `c`. Parentheses group.

**L4-7. Alternatives are a set; conjuncts are a sequence.** `Plant OR Plant` collapses to `Plant`,
while `Plant, Plant` keeps both conjuncts as written. A collapsed single alternative is no longer an
`OR` at all.

**L4-8. `EVAL name` reads a class property as a requirement** (L12-12). Until it is expanded it has
no value of its own, and asking for one is a programming error.

**L4-9. A requirement observes; it never chooses.** Nothing inside a requirement is an open choice
for a player to settle, which is also why an abstract expression repeated only inside requirements
declares no type variable (T13-8).

**L4-10. Requirements round-trip.** Grouping is re-inserted wherever re-parsing would otherwise read the tree differently.

---

## 5. Metrics

A metric denotes a non-negative integer computed from one state. Metrics appear after the `/` of an
instruction, inside counting requirements, and as class-property values.

**L5-1. Counting is the only world-dependent part.** `evaluate` is supplied a count for each
component count, a value for each property read, a count for each union and a value for each rank;
scaling, capping and subtraction are computed from the syntax itself. A metric is therefore
non-negative by construction.

**L5-2. An expression counts the components matching it; a bare number is a constant.** `Plant`
counts plants; `5` is five.

**L5-3. `n M` counts complete groups of n.** Its value is M's value divided by n, rounded down, so
`3 Plant` is 2 when there are 7 plants and also 2 when there are 8. A unit of one is meaningless and
is dropped: `1 Plant` *is* `Plant`. A unit of zero is rejected.

**L5-4. `M MAX N` is the smaller of the two values.** A cap may not be directly capped again.

**L5-5. `M - N` subtracts, saturating at zero, and is left-associative.** `A - B - C` is `(A - B) -
C`, and a metric never goes negative, so `Plant - 20` is 0 rather than a debt.

**L5-6. `A OR B` counts the union of its alternatives without double-counting.** Its arms must be
plain component counts: subtraction discards the component identity a union needs, so
`Plant - Steel OR Heat` is rejected. Duplicate alternatives are rejected.

**L5-7. Precedence, tightest first: scaling and `MAX`, then subtraction, then `OR`.** So
`A MAX 5 - B` caps `A` before subtracting, while `(A - B) MAX 5` caps the difference. Where a metric
is nested, its container decides how much grouping is needed (L4-5), and after the `/` of an
instruction a top-level `OR` must be grouped because a bare `OR` there begins an instruction
alternative (L6-7).

**L5-8. `receiver.name` reads a class property**, and `EVAL` includes a property's own syntax
(L12-12). A property metric with no receiver takes one from the enclosing refinement candidate or
context.

**L5-9. `RANK Selector { m1, m2, ... }` is a competition rank.** It denotes the highest-first
position of one candidate among the components matching `Selector` in one state, comparing the
listed metrics lexicographically. Authored syntax leaves the candidate open; a refinement supplies
it. At least one metric is required, and the selector's refinement filters the field without
becoming part of the name the metrics use. This module pins the syntax and that scoping; ranking a
live field is realized where a world is available, and pinned by `engine/RankMetricTest.kt`.

**L5-10. Metrics round-trip.** Grouping is re-inserted wherever re-parsing would otherwise read the tree differently.

---

## 6. Instructions

An instruction denotes a relation between a before-state and an after-state. It is the only kind of
element that does.

**L6-1. The elementary instructions are gain, removal and transmutation.** `n Foo` says the after
state holds n more components of type `Foo`; `-n Foo` that it holds n fewer; `n Foo FROM Bar` that
n components of `Bar` have become n of `Foo`.

**L6-2. A count is a positive integer or `X`.** `X` denotes an amount left open, and may carry a
coefficient: `2X Plant` is an even number of plants. A count of zero is rejected.

**L6-3. A quantifier says how much of the count must happen.** `!` means the whole amount, `.` as
much of it as possible, and `?` any part of it including none. An authored change may omit the
quantifier; elaboration then supplies the class's default (T10-2, L12-5). What "as much as possible"
resolves to against a real state is `QUANTIFIERS.md`'s subject.

**L6-4. `Ok` is the instruction that relates a state to itself.** Gaining `Ok` denotes no change at
all; it vanishes from a group (L6-8) rather than appearing as an empty member, and a group with
nothing left in it is `Ok`.

**L6-5. `I / M` scales a change by a metric's value.** `Titanium / 3 EarthTag` grants one titanium
per three complete Earth tags. Only an elementary change may be scaled this way.

**L6-6. `r: I` gates an instruction on a requirement.** The gate is not a choice (L7-5); when its
requirement fails, the instruction cannot be carried out. `OR` binds tighter than a gate, so
`3 PlantTag: Plant OR 4 Plant` gates both alternatives, and a gate on one alternative alone must be
parenthesized. A gate does not directly contain another gate.

**L6-7. `I OR J` is a choice among alternatives.** Duplicate alternatives are rejected rather than
collapsed. An `OR` is always open (L7-1), because the choice is the point.

**L6-8. `,` separates independent instructions and has the lowest precedence.** The result is a
*group*, not one instruction: nothing in this language relates the members of a group to each other,
which is exactly what makes them independent. Groups flatten, and a group of one renders as that
one.

**L6-9. `A THEN B` says A happens before B.** The relation is stated between the two changes
themselves and is right-associative, so `A THEN B THEN C` is one sequence of three stages rather than
nested pairs. Every stage before the last must be a single instruction: a group or another sequence
on the left is rejected, because "before" needs one identifiable change to be before. What waiting
means for pending work is `SEQUENCING.md`'s subject.

**L6-10. `EACH Selector { body }` quantifies over one state.** It denotes one independent branch of
`body` for each distinct concrete type matching `Selector` present in the state, with the selector's
spelling in the body denoting that type. A refinement on the selector filters which components take
part without becoming part of the name the body uses. The body may not be empty, and fanouts do not
nest. A concrete selector and a body that never names its selection are both meaningless — every
branch would be the same instruction — and are rejected where the fanout is resolved against a
world, which is `EACH.md`'s subject, along with how that world is enumerated and when. This module
pins the syntax and that scoping; `engine/EachSelectorOwnerTest.kt` and
`engine/InstructionResolutionTest.kt` pin the rest.

**L6-11. `I BY Actor` names who performs the change.** It distributes over a group, so
`(A, B) BY Player1` is `A BY Player1, B BY Player1`. Attribution itself is `IDENTITY.md`'s subject.

**L6-12. A transmutation may be written compactly when both sides share a class.**
`Foo<Same, Here, To FROM From>` is `Foo<Same, Here, To> FROM Foo<Same, Here, From>`. Exactly one
argument may change; the unchanged ones occupy both roles.

**L6-13. Precedence, tightest first:** a scaled expression and its quantifier, `/`, `BY`, `OR`, the
gate `:`, `THEN`, `,`. Parentheses group, and rendering re-inserts grouping wherever re-parsing
would otherwise read the tree differently — including around a transmutation written in full inside
an `OR`, whose bare `FROM` would be ambiguous.

**L6-14. `X` is one open amount per instruction, and may span a sequence.** Two independent
instructions may not share an `X` (there is nothing to make the two amounts agree), while the stages
of a `THEN` may, and then must agree (L7-7).

---

## 7. Narrowing: what remains open

An authored instruction usually leaves something open — an abstract type, an unfixed count, a choice
between alternatives. **Narrowing** is the relation "this more specific instruction is an acceptable
way of carrying out that more general one".

This section is about *elaborated* instructions. An authored change carries no quantifier until
elaboration supplies one (L6-3), and narrowing has nothing to compare until it does.

**L7-1. An instruction is abstract when something is still open.** `isAbstract` reports an unfixed
`X`, an absent or optional quantifier, an abstract expression (T5-3), or an `OR`. `2 Plant<Player1>!`
is not abstract; `2 Plant<Player1>?` is, because doing less is permitted and nothing has said how
much less.

**L7-2. A proposal must have the same shape.** A narrowing preserves the kind of node, the number of
`THEN` stages and the size of a group. Two exceptions, and only two: any instruction may narrow an
`OR` by narrowing one of its arms, and `Ok` may narrow an optional change.

**L7-3. A change may narrow its count, its quantifier and its types.** The count may not grow, and
may shrink only under `?`. The quantifier may narrow: `?` may become anything, while `!` and `.` are
incompatible with each other. Each written expression must narrow the authored one (T6-2).

**L7-4. `Ok` narrows an optional change and nothing else.** Declining `2 Plant?` entirely is a
narrowing; declining `2 Plant!` or `2 Plant.` is not.

**L7-5. A gate, a `PER` metric, a `BY` actor and an `EACH` selector are not choices.** A proposal
must reproduce each of them exactly; only what they contain may narrow.

**L7-6. An `OR` is satisfied by any arm.** A proposal narrows an `OR` when it narrows at least one
alternative; an `OR` narrows an `OR` when every one of its alternatives does.

**L7-7. `X` takes one value everywhere it appears.** Each occurrence receives that value multiplied
by its own coefficient, so `X Plant THEN 2X Heat` may become `3 Plant THEN 6 Heat` but not
`3 Plant THEN 5 Heat`. A proposed count that is not a multiple of the coefficient is rejected.

**L7-8. A shared type variable takes one value everywhere it appears.** Narrowing a sequence or a
transmutation that repeats an abstract expression must supply one consistent value for it (T13-6,
T13-7); two different values are rejected.

> **A known gap.** When the repeated expression is written with an empty argument list, the variable
> is declared but never binds, and the stages may diverge after all. Characterized in `LangBugsTest`.

**L7-9. `narrows` is the boolean form of `ensureNarrows`.** The former answers, the latter throws
`NarrowingException` and says why. A failure caused by something other than narrowing — an unknown
class, a malformed proposal — is not converted into a "no".

**L7-10. Groups narrow elementwise.** Members are matched by position, and the sizes must agree.

---

## 8. Effects

An effect is a rule attached to a class: `CityTile: 2 MC` says that whenever a city tile appears,
this component's owner gains 2 MC. Every component of that class carries the rule for as long as it
exists.

**L8-1. An effect is a trigger, a colon, and an instruction.** The trigger says which event the rule
is about; the instruction says how the state after that event relates to the state before it.

**L8-2. `::` marks an automatic effect** — a consequence carrying no choice, which the rule intends
to be inseparable from the event that caused it. When that distinction matters is `SEQUENCING.md`'s
subject.

**L8-3. There are two kinds of trigger.** `This` and `-This` are about this very component being
gained or removed. Any other expression is a *subscription* to gains, or with a leading `-` removals,
of components matching it.

**L8-4. A self trigger is not a subscription to its own type.** There is no way to spell one as the
other: writing the bare `This` placeholder as a subscription target *is* the self trigger, however
its empty argument list was written (L3-5). The two say
different things — `This` is about changes to this very component, and scales its instruction by the
number of copies changed, while a subscription is about changes anywhere that match an expression,
and is carried once per copy of the effect-bearing component. How many times each actually fires is
`ENGINE.md`'s subject.

**L8-5. `X` before a trigger's expression binds the size of the change.** `X Plant: X Heat` reacts to
a gain of any number of plants with the same number of heat. A removal is written `-X Plant`.

**L8-6. `OR` joins triggers, and self and subscribed triggers may not mix.** `This OR -This` is fine;
`This OR Plant` is not, because one is about this component and the other about the world.

**L8-7. `BY` restricts a trigger by actor and `IF` by state.** Precedence, tightest first: `OR`,
`BY`, `IF`. Parentheses give one alternative its own qualifier. A `BY` selector is an expression, so
`BY Player(NOT Owner)` is a filter and `BY Player` may declare an actor variable (T13-9).

**L8-8. A class literal may not be a trigger.** `Class<Foo>: Bar` is rejected: the one component per
concrete class is fixed before any effect runs (T4-6), so nothing ever gains one.

**L8-9. A bare `Component` subscription must be qualified.** `Component: Bar` subscribes to
everything and is rejected; `Component IF Foo: Bar` and `Component BY Anyone: Bar` are accepted,
because each states what the rule is actually watching for.

**L8-10. Effects round-trip, and a gated instruction is parenthesized after the colon** so that the
effect's own colon stays unambiguous.



---

## 9. Actions

An action is a rule a player may invoke: `Steel -> 5 MC` offers to turn one steel into 5 MC.

**L9-1. An action is an optional cost, an arrow, and an instruction.** The cost is written without a
minus sign; it is understood to be given up.

**L9-2. An action means: spend the cost, then do the result.** `cost -> I` denotes `-cost! THEN I`,
and a costless action denotes just `I`. That is the whole of what the arrow means; the `THEN` is
L6-9's, with nothing added.

**L9-3. A cost is a scaled expression, optionally scaled by a metric, optionally inside a transform
block.** A comma-separated or gated cost is rejected — alternative costs are written as separate
actions, so that each is one thing a player can choose to do.

**L9-4. An action becomes an effect keyed to the action's position on its class.** The nth action of
a class lowers to an effect triggered by `UseAction<This, ActionN>`, and a class may offer at most
three. Action identity, availability and payment are `ACTIONS.md`'s subject; the standard-resource
cost rewrite that currently rides along in this module belongs there too, and `Transforming.kt`
carries a TODO to move it into `tfm-canon`.

**L9-5. A class's effects are its authored effects followed by its lowered actions.**

**L9-6. An instruction that happens on gain is the effect `This: I`.** This is how a card's "do this
now" section becomes an ordinary rule; an immediate `Ok` produces no effect at all.

---

## 10. Transform blocks

A transform block marks a subtree for rewriting by a named handler. `PROD[...]` is the one every
card uses: inside it, `Plant` means plant *production*.

**L10-1. A block is an all-caps kind name, square brackets, and one node.** The kinds of node that
accept a block are instruction, action cost, metric, requirement and trigger.

**L10-2. A block whose kind has no handler is preserved verbatim**, so a source may carry marks that
a later stage will interpret.

**L10-3. A handler rewrites only inside its own block**, and what it returns must be the same kind of
Pets it was given. A block that expands into several independent instructions splices into the
surrounding group (L6-8).

**L10-4. A trigger block wraps only a gain or removal**, never `OR`, `BY` or `IF` — the mark applies
to the event being watched, not to the restrictions on it.

**L10-5. Nesting a block inside a block of the same kind is representable but not processable.** The
syntax admits `PROD[PROD[Plant]]`; any handler for that kind rejects it, because the second mark
could only mean what the first already means.

---

## 11. Owner-local classes

A card often needs a class of its own — one required action, one special tile, one remote area —
that no other card will ever mention. Rather than force a name, Pets lets the definition declare the
class where it is used, and derives the name.

This is source-level lowering: it happens while the declaration file is parsed, so the type system
never sees anything but ordinary declarations.

**L11-1. An expression followed by a body declares a class at its point of use.**

```pets
CLASS Inventrix { This: RequiredAction { -> 3 ProjectCard } }
```

**L11-2. The generated name is the owner's name, an underscore, and the base class name.** The
occurrence becomes that name, so the effect above is `This: Inventrix_RequiredAction`, and the
generated declaration is `CLASS Inventrix_RequiredAction : RequiredAction`.

**L11-3. The body follows the complete expression.** Arguments specialize both the occurrence and
the generated class's declared supertype; refinements constrain only the occurrence and are removed
recursively from the supertype, because a refined type cannot be a supertype (L1-9).

```pets
SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {}
```

becomes the occurrence `MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>` and declares
`CLASS MiningArea_SpecialTile : SpecialTile<LandArea>`.

**L11-4. The local body may contain invariants, properties, effects and actions**, and may not
contain `DEFAULT` clauses or nested declarations. The generated class inherits applicable defaults
from its supertypes like any other.

**L11-5. Owner-local classes do not nest.** Neither a local body nor an argument of the occurrence
may declare another one.

**L11-6. One owner declares at most one unnamed local class per base name.** A second is rejected
rather than distinguished by an ordinal, so the derived name stays stable and meaningful. Two local
classes with the same natural suffix must be declared explicitly.

**L11-7. The syntax is available only where a declaration file is being read.** `parseOneLinerClass`
rejects it, and `Parsing.parse` parses and validates it before rejecting it with
`NoNewClassDeclarationsException`, because a submitted instruction has no definition owner and a
live game's class table is frozen (T1-6).

**L11-8. Naming the base class alone still means the base class.** An occurrence with no local body
is an ordinary expression; it does not resolve to some nearby derived class.

---

## 12. Elaboration

Authored Pets is not yet the form that sections 4 to 9 describe. **Elaboration** is the
class-table-dependent rewriting that fills in what a physical game leaves implicit — that a tile goes
on a land area, that a resource belongs to the player doing the thing, that "gain 3 cards" means
three separate cards. It changes how a source *reads*; it never changes which types exist, which is
what section 10 of the type system specification means by a default not being a bound.

**L12-1. Elaboration is a fixed set of stages, applied differently depending on where the Pets came
from.** The stages are: canonicalize a session's input names (which names a session accepts is
`NAMING.md`'s subject); expand every name to its canonical spelling; infer type variables (T13-6
through T13-9); split atomized gains (L12-11); insert defaults (L12-4 through L12-10); bind the
contextual owner (L12-3); dispatch transform blocks (section 10); expand property evaluations
(L12-12).

Two entry points apply different subsets, in different orders:

| | An element a player submits | A class's own effects |
| --- | --- | --- |
| Session input names | canonicalized | not applicable — a source writes canonical names |
| Defaults are inserted against | `This` | the class's own context |
| Order of defaults and atomizing | atomize, then default | default, then atomize |
| Contextual owner | bound to the submitting player | left open, and `BY Owner` added where the result needs one (L12-13) |
| Property evaluations | rejected, except in a metric (L12-12) | expanded once the receiver is concrete |

The shared core — expanding names, inferring variables, atomizing, defaulting and dispatching — is
the same rewriting in both.

**L12-2. `This` is replaced by the context expression.** `Class<This>` becomes the class literal for
the context's class, and `This<Foo>` keeps its own arguments while adopting the context's class.

**L12-3. `Owner` is replaced by the context owner**, everywhere except inside the body of an `EACH`
whose selector is itself an owner — there the selection supplies the owner instead, so an ordinary
owned body reads on a card exactly as it does anywhere else. The selector itself is not shielded: it
names components in the enclosing context, so `EACH ProjectCard<Owner> { ... }` means the cards the
enclosing owner holds. A `RANK` selector shields nothing.

**L12-4. Every expression receives its class's all-use dependency defaults** (T10-1), recursively.

**L12-5. A gain or removal also receives the defaults for its use kind**, and a gain must opt in.
When a class has gain dependency defaults, a gain may not leave its argument list implicit: write
`OceanTile<>` to accept them, or supply at least one argument. This keeps a defaulted placement
visible at the point of use.

**L12-6. A removal declines its use-specific defaults by writing nothing.** An implicit argument list
on a removal is not an error: it simply does not receive the removal-only dependency defaults, though
all-use defaults (L12-4) still apply. `-Marker<>` accepts them.

> **The asymmetry is deliberate.** The symmetric rule — a gain *or* removal must opt in — is the
> tempting one, and it is wrong. Requiring it of removals rejects `-Owed`, written bare in the
> action payment lowering, and with it `CryoSleepTest`, `AridorTest`, `DistantPressureMassTest`,
> three whole-game replays and two integration suites. A removal names a component that already
> exists; there is no placement left to default.

**L12-7. `Foo<>` is invalid where that use has no dependency defaults to accept.** An empty list is
an acceptance, not merely a second spelling of the same expression.

**L12-8. The two halves of `A FROM B` are defaulted independently**, and where the transmutation
writes no quantifier of its own, the gained half's gain default and the removed half's removal
default are intersected, the stricter winning: mandatory beats as-much-as-possible, which beats
optional.

**L12-9. Inside a `HAS` refinement, a bare dependent expression reserves a slot for the candidate.**
It keeps the first dependency position that could accept the refined domain free, so that candidate
substitution (T8-3) binds it: `Player(HAS StartToken)` asks `StartToken<p>` of each candidate `p`,
even though `StartToken` inherits a contextual owner default. The explicit spellings remain
available: `StartToken<Owner>` requests the contextual owner and `StartToken<>` accepts the default.

**L12-10. Inside a refinement, a default is deferred when its dependency is a direct use of a
class-header type variable** (T13-2), so that candidate substitution can bind it through that
occurrence. Writing `<>` still accepts the default explicitly.

**L12-11. A gain of several `Atomized` components becomes several gains of one.** `3 ProjectCard`
becomes three independent gains, because three cards are three separate things to choose.

**L12-12. `EVAL` includes a class property's own syntax where it is written.** `EVAL Goal.score`
expands to the metric that class's `score` property holds, with `This` inside it bound to the
property's class. It needs a receiver context, so it is expanded in a class effect and in a
submitted *metric*, which is given one, and rejected in an ordinary submitted instruction, which is
not. An evaluation whose receiver is still abstract stays unexpanded until it is not, and a property
that would expand into itself is rejected.

**L12-13. A class's effects are elaborated against that class's own context.** They are gathered from
every superclass, and an effect on a class that is neither an owner nor owned, whose instruction
needs an owner, has `BY Owner` added to its trigger — that is how an unowned rule learns whose event
it is reacting to.

**L12-14. A change to a type this game cannot hold becomes `Die` or `Ok`.** When a specialized
instruction names a type that is not active (T12-1), a mandatory change becomes a gain of `Die` — an
instruction that can never be carried out — and an optional one becomes `Ok`. This keeps a rule that
mentions absent content from silently succeeding.

**L12-15. Specializing an effect closes it over one exact component.** Given a component's type,
`specializeEffect` binds the class's type variables (T13-5), the `This` context and the contextual
owner together, in one step.

---

## Appendix A: known departures

Each of these has a passing characterization in `test/common/dev/martianzoo/pets/LangBugsTest.kt`;
the rule states the intent.

| Rule | Departure |
| --- | --- |
| L7-8 | A repeated abstract expression written with an empty argument list (`Tile<> THEN Tile<>`) declares a shared variable that never binds, because the recorded variable keeps the spelling it had before use-specific defaults were inserted. The stages may then be narrowed to different types. |

## Appendix B: deliberately unspecified

- **Which parentheses a renderer adds beyond the ones round-tripping requires.** Rendering is
  required to round-trip (L4-10, L5-10, L6-13, L8-10), not to be minimal, and today it is not
  minimal.
- **Exception messages.** Rules name exception *types* where the type is part of the contract.
- **The order in which elaboration visits nodes.** Only the order of the stages (L12-1) is
  specified.
- **Where a transform handler comes from**, and what any particular kind such as `PROD` rewrites.
  Section 10 specifies the mark, not its meaning.
