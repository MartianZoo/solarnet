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
everything that proves it. Where a rule and the implementation are known to disagree, the rule says
so; there is no such departure today.

Examples use real Terraforming Mars component names — `GreeneryTile`, `Plant`, `OceanTile` — but the
declarations shown are simplified. They illustrate a rule; they are not a transcript of `tfm-canon`.

Blockquoted **Non-normative example** and **Non-normative implementation note** insets explain why
an otherwise surprising provision exists. They are evidence and orientation, not additional rules.

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

> **Non-normative design note — Pets follows the game’s icon grammar.** Pets is not trying to make
> Terraforming Mars look like a conventional programming language. Its primary notation is the
> game’s own: nouns are component types, juxtaposed counts scale them, explicit names carry one
> choice between its uses, and omitted context can mean what the physical component leaves implicit.
> Explicit machinery is added where the game needs a distinction, not merely because a
> general-purpose language would normally spell it. The standard to apply is therefore whether the
> compact notation has one coherent elaborated meaning and remains faithful to the game, not whether
> it resembles a familiar term language.

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
| Action availability, costs, billing and action identity | [`ACTIONS.md`](agents/ACTIONS.md) |
| The order in which independent effects fire | [`SEQUENCING.md`](agents/SEQUENCING.md) |
| The event log, causes and traces | [`ENGINE.md`](agents/ENGINE.md), [`DIAGNOSTICS.md`](agents/DIAGNOSTICS.md) |
| Which classes a particular game contains | [`GamePremise`](../src/common/dev/martianzoo/pets/data/GamePremise.kt) and [`PremiseSelectionTest`](../test/common/dev/martianzoo/pets/types/PremiseSelectionTest.kt) |
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
Each declaration introduces exactly one class. `CLASS Foo` is concrete; `ABSTRACT CLASS Foo` is
abstract (T2-1).

```pets
ABSTRACT CLASS Tile<Area> : Occupant, Owned<Owner>
```

**L1-3. A body is brace-delimited, and its elements are separated by newlines or by semicolons.** A
body element is an invariant (`HAS r`), a `DEFAULT` clause, a property assignment (`name = value`),
an effect, or an action. A newline-separated body may also contain nested declarations; a
semicolon-separated one may not.

```pets
CLASS GreeneryTile : Tile { HAS MAX 1 This; This: OxygenStep }
```

> **Non-normative example — map spaces.** Generated map declarations such as `Hellas_1_4` keep row,
> column, and placement bonus in one semicolon-separated body. Allowing nested declarations in that
> form would make a “one physical space per line” record expand into invisible sibling classes.

**L1-4. A nested declaration becomes a sibling that names its container as a supertype** (T2-2).
The container is returned first, then its nested declarations in source order, recursively.

> **Non-normative example — cards and locations.** The `CorporationCard` hierarchy and `ProjectCard`
> are written inside `CardBack`, itself inside `Card`, but the type table needs ordinary globally
> named classes. Lowering nesting to sibling inheritance preserves the readable taxonomy without
> creating a namespace the rest of Pets does not have.

**L1-5. A docstring is a quoted string on the line before `CLASS`.** It is retained on the
declaration and re-emitted when the declaration is rendered.

**L1-6. `DEFAULT` clauses name the class that declares them** (T10-3) and are merged into one set
per use kind (T10-1). Separate compatible clauses may supply the dependency arguments and quantifier
of one use-kind default. Clauses that disagree about their class, dependency arguments or quantifier
are rejected; declaration order never selects a winner. A clause naming another class is rejected.

> **Non-normative implementation note — defaults have one source.** No card needs to install a
> remote class's default. Permitting it would let an unrelated expansion silently change what bare
> `OceanTile<>` or `CardBack` means merely by being loaded later.

**L1-7. A property is assigned at most once per body.** `name = value` binds one class property;
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

> **Non-normative implementation note — reject conflicting print data early.** A card cannot have
> two printed costs or two milestone requirements. Rejecting the second assignment at parse time
> prevents declaration order from becoming an accidental override rule.

**L1-8. Signature expressions carry no refinements**, at any depth. A dependency bound or supertype
written `Foo(HAS Bar)` or `Foo(NOT Bar)` is rejected, because a refined type cannot be a bound.

> **Non-normative example — Mining Area.** The card declares a local special tile at
> `LandArea(HAS Neighbor<OwnedTile>)`. The live refinement constrains this occurrence, while the
> generated class extends plain `SpecialTile<LandArea>`; putting the board query in its signature
> would make a state-dependent predicate part of permanent class identity.

**L1-9. Whitespace and comments.** Horizontal whitespace separates tokens and carries no other
meaning: it may appear between any two of them, and no construct depends on how much of it there is.
It is required only where two tokens would otherwise run together into one, so `2 MC` is a scalar and
a name while `2MC` is neither. `//` begins a comment that runs to the end of the line. A backslash
immediately before a line ending continues the line, so one element may span several source lines.
Newlines are significant only as separators (L1-1, L1-3).

> **Non-normative example — Mars Nomads.** Its action moves a marker and then pays every marked
> area's placement bonus. A backslash lets that one action span source lines without a newline being
> mistaken for the end of the body element.

**L1-10. A declaration renders as parseable source, and round-trips.** `toString()` produces a
multi-line declaration and `toString(oneLine = true)` a semicolon-separated one; parsing either
yields an equal declaration.

> **Non-normative implementation note — generated declarations are real source.** Owner-local
> classes and diagnostic output are rendered and parsed again in tests and tooling. Round-tripping
> prevents a formatter from changing an action's grouping or turning a nested class into a body
> element with different ownership.

**L1-11. A declaration can also be parsed on its own.** `Parsing.parseOneLinerClass` accepts exactly
one declaration, with an optional semicolon-separated body, and rejects owner-local class syntax
(L11-7). This is how a declaration embedded in structured card data is read.

> **Non-normative implementation note — one record, one declaration.** Catalog composition accepts
> standalone declarations supplied by structured data. Rejecting trailing source or a local class
> keeps that API from smuggling additional globally named types through one record.

**L1-12. Every catalog also receives the system declarations.** `systemClassDeclarations` supplies
the universal audit signal `Audit` plus the classes this specification and the type system depend
on — `Component` and `Class` (T1-4, T1-5), the ownership vocabulary `Anyone`, `Owner` and `Owned`,
the actor root `Actor`, the identity signal `Ok` (L6-4), and the impossible type `Die` (L12-14) —
plus `Atomized` (L12-11) and `Custom` (T2-9).
A catalog's own source is loaded alongside them. Premise construction always includes `Audit` and
decides which of the remaining system declarations a *game* contains.

> **Non-normative example — impossible and empty outcomes.** Specialization uses the built-in `Die`
> and `Ok` terminal instructions when selected content makes a mandatory result impossible or an
> optional result empty. Supplying their Classes in every Catalog lets that rule work before any
> Terraforming Mars module contributes its own classes.

---

## 2. Names

**L2-1. A class name is an uppercase-leading identifier.** After the first ASCII uppercase letter,
ASCII letters, digits, and underscores are allowed. Formally:

```text
[A-Z][A-Za-z0-9_]*
```

Thus `GreeneryTile`, `Tharsis_2_2`, `A_foo`, `L1TradeTerminal`, `MC`, and `TOOLONG` are names, while
`greenery` and `Terraforming Mars` are not.

> **Non-normative example — coordinates and currencies.** Digits and underscores are admitted for
> board spaces like `Tharsis_2_2`, and all-caps spellings for abbreviations like `MC` and `TR`.
> Covering both costs only the requirement that a name begin with a capital, which is what keeps
> lowercase identifiers and prose labels out.

**L2-2. Keywords are reserved and case-sensitive.** `ABSTRACT`, `BY`, `CLASS`, `COUNT`,
`DEFAULT`, `EACH`, `EVAL`, `FROM`, `HAS`, `IF`, `MAX`, `NOT`, `OR`, `RANK`, `THEN` and `X`, together
with the property-value words `Metric`, `Number` and `Requirement`, are the words the grammar itself
uses, and none of them may be a class name. Because the reserved spellings are exact, `Max`, `By`
and `Has` are perfectly good class names.

**L2-3. A property name is lowerCamelCase**: a lowercase letter followed by letters and digits.

**L2-4. A transform-kind name is an all-caps identifier** (section 10).

**L2-5. Class names are global.** A class name means whatever class the class table says it means
(T1-1, T1-7); no construct declares a class name, and no construct gives one a different class in
part of a source. Which name a concept should get, and how names are displayed to a person, is
`NAMING.md`'s subject.

This is not the same as saying that nothing is bound. Several constructs do bind, and two of them
shadow:

| Construct | What it binds | Where |
| --- | --- | --- |
| `This` | the component the declaration is about | the whole declaration (L3-5) |
| `Owner` | the context's owner | the whole declaration, except inside an `EACH` whose selector is an owner (L12-3) |
| `X` | one open amount | one instruction, across the stages of a `THEN` (L6-14) |
| `@Type` or `Name@Type` in an `EACH` or `RANK` selector | each selected component | that construct's body or metrics (L6-10, L5-9) |
| a refinement's domain | the candidate | that refinement (T8-3) |
| `@Type` or `Name@Type` in an Effect trigger, `THEN`, Action cost, or transmutation destination | one shared choice | that Effect, sequence, Action, or transmutation (L6-15, L6-16, L8-12, L9-8, T13-6) |

> **Non-normative example — generated special tiles.** `MiningRights_SpecialTile` must be referable
> later by that exact global name when its placement bonus is inspected. Lexical scoping would make
> the inline declaration convenient locally but invisible to the card's later production rule.

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

> **Non-normative example — Aquifer.** `OceanTile` and `OceanTile<>` denote the same type, but the
> latter explicitly accepts the empty-water-area placement default. Erasing the spelling difference
> would either hide a consequential default or force every harmless type reference to accept it.

**L3-3. A refinement is a non-empty set of conjoined clauses.** Each comma-separated clause repeats
its keyword: `(HAS r)` refines by a requirement and `(NOT x)` by a structural difference. A
top-level comma separates clauses, so a conjunction inside one `HAS` must be grouped, as in
`(HAS (Foo, Bar) OR Baz, NOT Qux)`. Duplicate clauses collapse and order does not affect equality.
T8-1 through T8-11 say what each clause means.

**L3-4. A class literal is written with one bare class name**, `Class<Steel>` (T4-1, T4-6).

**L3-5. `This` names the component the enclosing declaration is about.** It is an expression like
any other and may take arguments: `This<Foo>` keeps the arguments and adopts the context's class.
Elaboration replaces it (L12-2). `This` is a placeholder rather than a class, so it has no defaults
of its own and an empty argument list on it accepts nothing: `This<>` *is* the bare placeholder.
Every construct that recognizes the placeholder recognizes both spellings, even though the two are
different expressions (L3-2).

> **Non-normative example — self cleanup.** Card locations use `-This:: This!` to remove and restore
> that exact location component. Recognizing both `This` and `This<>` as the placeholder prevents an
> empty list from accidentally turning self cleanup into a subscription to a broad card-location
> type.

**L3-6. `Owner` in an authored expression is contextual.** It stands for whoever supplies the
context, and elaboration replaces it (L12-3). `Anyone` is an ordinary class and stands for itself
(T3-4).

> **Non-normative example — CrediCor.** Its setup says `This: 57 MC` without naming a player.
> Contextual `Owner` lets that bare money gain belong to the player who received CrediCor, while
> `Anyone` remains available for genuinely unrestricted theft or payment.

**L3-7. An expression renders as its Type-variable marker if present, the class name, the argument
list if one was written, and the refinement.** Whitespace is not preserved and duplicate refinement
clauses collapse, but an authored expression is not rewritten into its type's canonical form:
`Tile` and `Tile<Area>` remain distinct expressions even though they resolve to one type (T1-3,
T5-5).

> **Non-normative implementation note — normalization preserves variable identity.** Type-variable
> scope recording precedes normalization, and later rewrites retain each marked occurrence's
> marker. Ordinary expressions remain outside that scope even when they resolve to the same Type.

**L3-8. Two expressions are equal when their structural spellings agree.** Argument order is part of
the spelling, while refinement-clause order and duplication are not (L3-3). Thus
`Microbe<Player1, Ants>` and `Microbe<Ants, Player1>` are different expressions for one type. This is
why the type system, not the syntax, is the authority on identity (T5-1).

> **Non-normative implementation note — syntax is not component identity.** No card distinguishes
> `Microbe<Player1, Ants>` from the reversed argument spelling once resolved. Keeping the syntax
> unequal preserves faithful rendering and exact variable-occurrence tracking without changing the
> game type.

**L3-9. An `@` marker before an expression marks a Type variable where the enclosing construct
permits one.** `@Type` is anonymous; `Name@Type` gives it a class-name-shaped local discriminator
under L2-1. The complete bound expression follows the marker, so its argument list and refinement
remain on the right: `Chosen@Tile<Area>(HAS Marker)`. Matching anonymous occurrences share by bound
Class and lexical scope. Matching named occurrences share by `(BoundClass, Name)` and scope, so one
name may be reused for different bound Classes.

A scope may contain an anonymous variable only when it has no named variable with the same bound
Class. If several variables share one bound Class, every one must be named. The syntax does not
distinguish a declaration from a use or require the occurrence that supplies the choice to come
first; the enclosing construct determines which occurrence supplies the value and where the marker
is visible (L6-15, L6-16, L8-12, L9-8).

An occurrence that reuses a supplied value cannot add a refinement. There is one argument-list
form: `Class<@Type>` or `Class<Name@Type>` marks the Class represented by that literal, and
`@Type<dependencies>` denotes the selected Class with those dependency arguments.
`@Type<>` deliberately accepts that occurrence's defaults under L3-2. This is
represented-Class application, not an argument list on an arbitrary Type variable; T4-1 and T13-1
define it.

A construct-local Type variable exists only through two or more matching marked occurrences.
Every other expression retains its ordinary meaning, except that the represented root inside a
refined `Class<T>` literal follows T8-10 automatically. Compact `FROM` is an instruction form, not
a variable declaration: it stores each unchanged argument once and derives both projections from it
(L6-12).

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

> **Non-normative example — Arcadian Communities.** Its community must begin on a land area with
> `MAX 0 Occupant`. Ordinary minimum syntax cannot express “empty”; exact zero would also work for
> this non-negative metric, but the authored maximum form states the absence test directly.

**L4-3. A minimum of zero is rejected.** `0 Plant` is not a requirement — it would ask nothing —
while `MAX 0 Plant` and `= 0 Plant` are the useful ways to say "none".

> **Non-normative implementation note — no vacuous gates.** No canonical card needs “at least zero.”
> Rejecting it catches a likely mistaken `MAX 0` before a condition silently becomes always true.

**L4-4. The target is independent of the metric's own scaling.** `MAX 2 (3 Plant)` compares 2
against the value of `3 Plant`, which counts complete groups of three (L5-3). Six plants meet it and
nine do not.

> **Non-normative example — Producer.** Producer asks for 16 net production units, where its metric
> first subtracts production offsets. The outer 16 is the milestone threshold; it must not become a
> multiplier inside the net-production calculation.

**L4-5. A counting requirement takes one metric atom.** A metric union or subtraction must therefore
be parenthesized where a requirement counts it: `9 (Plant - Steel)`.

> **Non-normative example — Tycoon.** `10 (ActiveCard OR AutomatedCard)` counts ten cards from a
> union. Without the required grouping, `OR AutomatedCard` could be parsed as a requirement
> alternative, changing “ten total cards” into “ten active cards or one automated card.”

**L4-6. `,` is conjunction and `OR` is disjunction, and `OR` binds tighter.** So `a, b OR c` requires
`a`, and one of `b` or `c`. Parentheses group.

> **Non-normative example — Colonies setup.** Its premise is an `OR` of player-count/colony-count
> conjunctions. Parenthesized pairs ensure a one-player game needs four colony tiles and a two-player
> game needs five; reversing precedence would cross-wire counts between alternatives.

**L4-7. Alternatives are a set; conjuncts are a sequence.** `Plant OR Plant` collapses to `Plant`,
while `Plant, Plant` keeps both conjuncts as written. A collapsed single alternative is no longer an
`OR` at all.

> **Non-normative implementation note — observation, not choice.** Repeating the same alternative
> cannot make a requirement easier twice, so it collapses. Conjunct order is retained for faithful
> rendering and diagnostics even though evaluating the same conjunct twice changes no result.

**L4-8. `EVAL name` reads a class property as a requirement** (L12-12). Until it is expanded it has
no value of its own, and asking for one is a programming error.

> **Non-normative example — claiming a milestone.** The generic milestone rule gates its claim with
> `EVAL This.requirement`. Expansion must insert Gardener's greenery test, Mayor's city test, or the
> selected milestone's other printed requirement; `EVAL` itself has no universal truth value.

**L4-9. A requirement observes; it never chooses.** Nothing inside a requirement is an open choice
for a player to settle. Abstract expressions there describe the observed domain rather than
declaring Type variables (T13-8).

> **Non-normative example — Sponsor.** `HAS "3 CardFront(HAS 20 cost)"` counts any three qualifying
> cards. Treating the abstract `CardFront` as a choice would capture one expensive card type and ask
> for three copies of it instead of observing the player's tableau.

**L4-10. Requirements round-trip.** Grouping is re-inserted wherever re-parsing would otherwise
read the tree differently.

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

> **Non-normative example — Celestic.** It scores one victory point per three floaters. Seven and
> eight floaters must both yield two points; retaining a fractional remainder would create scoring
> values the physical game cannot represent.

**L5-4. `M MAX N` is the smaller of the two values.** A cap may not be directly capped again.

> **Non-normative example — Jupiter Floating Station.** Its action pays 1 MC per floater, capped at
> four. `Floater<This> MAX 4` limits the payout metric without limiting how many floaters the card may
> hold.

**L5-5. `M - N` subtracts, saturating at zero, and is left-associative.** `A - B - C` is `(A - B) -
C`, and a metric never goes negative, so `Plant - 20` is 0 rather than a debt.

> **Non-normative example — Venus Shuttles.** Its action cost is `1 MC / (12 - VenusTag)`. Once the
> player has twelve or more Venus tags, the discount metric must stop at zero rather than turn into
> negative money and pay the player to take the action.

**L5-6. `A OR B` counts the union of its alternatives without double-counting.** Its arms must be
plain component counts: subtraction discards the component identity a union needs, so
`Plant - Steel OR Heat` is rejected. Duplicate alternatives written by an author are rejected;
programmatic construction and later rewrites collapse alternatives that have become equal. A
single remaining count is no longer a union.

> **Non-normative example — Geologist.** A tile can be both on a volcanic area and adjacent to one.
> The milestone's union must count that tile once; summing the two arms would let overlapping tiles
> inflate the required group of three.

**L5-7. Precedence, tightest first: scaling and `MAX`, then subtraction, then `OR`.** So
`A MAX 5 - B` caps `A` before subtracting, while `(A - B) MAX 5` caps the difference. Where a metric
is nested, its container decides how much grouping is needed (L4-5), and after the `/` of an
instruction a top-level `OR` must be grouped because a bare `OR` there begins an instruction
alternative (L6-7).

> **Non-normative example — Industrial Complex.** Its catch-up production uses the union of the
> resource's Class component, the live `QuickStartVariant` component, and any `ProdOffset`
> components to build its signed target, then subtracts the current `Production`. Parentheses keep
> that target calculation together.

**L5-8. `receiver.name` reads a class property**, and `EVAL` includes a property's own syntax
(L12-12). A property metric with no receiver takes one from the enclosing refinement candidate or
context.

> **Non-normative example — card payment.** The generic `PlayCard` rule creates debt from
> `CardFront.cost`. Supplying the chosen concrete card as receiver is what turns one generic rule
> into the correct price for every card face.

**L5-9. `RANK Selector { m1, m2, ... }` is a competition rank.** It denotes the highest-first
position of one candidate among the components matching `Selector` in one state, comparing the
listed metrics lexicographically. Equal metric vectors share one rank, and the next unequal vector's
rank skips the places occupied by the tie. Authored syntax leaves the candidate open; a refinement
supplies it. Inside a `HAS` refinement only, the selector may also be omitted from a counted rank:
`Foo(HAS =1 (RANK { score }))` means `Foo(HAS =1 (RANK Foo { score }))`, using the unrefined
expression that owns that refinement as the field. Outside an expression refinement there is no
outer domain to supply that selector, so `RANK { score }` is invalid. At least one metric is
required. A marker on `Selector` explicitly makes the candidate available through the same marker
on `SelectorRoot` in the metrics; other expressions in those metrics retain their ordinary
meanings. An argument-free marked reference retains the selector's dependency arguments through
elaboration, while the selector's refinement filters the field without becoming part of the
candidate value. There is no lowest-first form; subtracting the metric from a known upper cap
expresses the inverse ordering. This module pins the syntax and that scoping; ranking a live field
is realized where a world is available, and pinned by `engine/RankMetricTest.kt`.

> **Non-normative example — award scoring.** Award resolution ranks every player by the selected
> award's metric, then awards first and—when applicable—second place. Lexicographic metrics and a
> filtered selector let the same machinery represent ties without baking one award into the engine.

**L5-10. Metrics round-trip.** Grouping is re-inserted wherever re-parsing would otherwise read the
tree differently.

---

## 6. Instructions

An instruction denotes a relation between a before-state and an after-state. It is the only kind of
element that does.

**L6-1. The elementary instructions are gain, removal and transmutation.** `n Foo` says the after
state holds n more components of type `Foo`; `-n Foo` that it holds n fewer; `n Foo FROM Bar` that
n components of `Bar` have become n of `Foo`.

A direct gain of the system `Signal` class fires both gain and removal triggers while its count
remains unchanged. Both changes are real — that is how a signal does its work, by what its gain and
its removal trigger (section 8) — and no signal component remains behind. This point-event behavior
belongs only to a direct Signal gain; writing `SignalSubtype FROM SignalSubtype` is an ordinary
transmutation. A `Signal` gained by an explicit transmutation from another type is gained normally,
then removes itself.

**L6-2. A count is a positive integer or `X`.** `X` denotes an amount left open, and may carry a
coefficient: `2X Plant` is an even number of plants. A count of zero is rejected.

> **Non-normative example — Sulphur-Eating Bacteria.** `X Microbe<This> -> 3X MC` lets the player
> choose how many microbes to spend while fixing the three-to-one exchange rate. Replacing `X` with
> unrelated open counts would allow the paid and received amounts to drift apart.

**L6-3. A quantifier says how much of the count must happen.** `!` means the whole amount, `.` as
much of it as possible, and `?` any part of it including none. An authored change may omit the
quantifier; elaboration then supplies the class's default (T10-2, L12-5).

The three differ in *who* settles the amount, which is why later rules treat them so differently:

| Quantifier | The amount is | Settled by |
| --- | --- | --- |
| `!` | already fixed | nobody; the change happens in full or not at all |
| `?` | an open choice | whoever settles the instruction, who may choose none (L7-1) |
| `.` | fixed, but not yet known | the state the change is carried out against |

So `!` is concrete, `?` is abstract, and `.` is neither: it leaves no choice, but its amount is
read off a state rather than written down. Narrowing can settle a choice (section 7); only
resolution against a state can settle a `.`, which is `QUANTIFIERS.md`'s subject.

After both sides have narrowed to concrete Types, a transmutation is **reflexive** when those Types
are equal (T5-1), regardless of how they were spelled. A mandatory reflexive transmutation is
invalid; an optional or as-much-as-possible one resolves to `Ok` and produces no change event. The
same rule applies whether its quantifier was written or supplied by elaboration. An empty argument
list affects default acceptance and authored spelling (L3-2); it cannot make equal resolved Types
non-reflexive.

> **Non-normative examples — Artificial Lake and asteroid attacks.** Artificial Lake's special
> ocean placement is `!`: choosing that arm requires the exceptional land placement to succeed in
> full. Asteroid cards mark an opponent's plant loss `?`, because the attacker may choose fewer
> plants or none. One generic “optional” flag cannot express both rules.

**L6-4. `Ok` is the instruction that relates a state to itself.** Gaining `Ok` denotes no change at
all; it vanishes from a group (L6-8) rather than appearing as an empty member, and a group with
nothing left in it is `Ok`.

> **Non-normative example — Local Heat Trapping.** Its owner may spend one, two, or three floaters
> for different heat payouts, or choose `Ok` and do nothing. Treating `Ok` as a physical component
> would leave a meaningless token behind instead of representing the legitimate no-change arm.

> **Non-normative design note — identity and impossibility.** Instructions denote relations between
> states, so they need both an identity relation and an impossible relation. `Ok` is the identity:
> composing with it changes nothing. `Die` (L12-14) is the impossible relation: it has no legal
> after-state. Giving both ordinary Pets names lets choices and rewrites retain the icon grammar
> instead of introducing a separate control-flow notation; neither denotes a component that can
> remain in a world.

**L6-5. `I / M` scales a change by a metric's value.** `Titanium / 3 EarthTag` grants one titanium
per three complete Earth tags. Only an elementary change may be scaled this way.

> **Non-normative example — Community Services.** It grants MC production per card with no tags.
> Scaling the one production change by `CardFront(HAS MAX 0 Tag)` creates the aggregate reward;
> scaling an arbitrary sequence would leave unclear which stages repeat and in what order.

**L6-6. `r: I` gates an instruction on a requirement.** The gate is not a choice (L7-5); when its
requirement fails, the instruction cannot be carried out. `OR` binds tighter than a gate, so
`3 PlantTag: Plant OR 4 Plant` gates both alternatives, and a gate on one alternative alone must be
parenthesized. A gate does not directly contain another gate. When a first-stage gate uses a type
variable shared with that stage and a later `THEN` stage, resolution waits for the first-stage
choice, substitutes it throughout the sequence, and then checks the gate before executing the
change (T13-8).

> **Non-normative example — Factorum.** Its first action grants energy production only under
> `MAX 0 Energy`. The requirement decides whether that result is available; it is not another arm a
> player can narrow or waive after choosing the action.

**L6-7. `I OR J` is a choice among alternatives.** Duplicate alternatives written by an author are
rejected. Programmatic construction and later rewrites collapse arms that have become equal, and a
single remaining outcome is no longer an `OR`. An `OR` that remains is always open (L7-1), because
the choice is the point.

> **Non-normative example — Atmo Collectors.** Spending one floater offers 2 titanium, 3 energy, or
> 4 heat. Those remain three player choices because their resulting changes are distinct. By
> contrast, two context-dependent spellings that elaborate to the same change offer only one
> resulting move, so retaining both would present a meaningless duplicate choice.

**L6-8. `,` separates independent instructions and has the lowest precedence.** The result is a
*group*, not one instruction: nothing in this language relates the members of a group to each other,
which is exactly what makes them independent. Groups flatten, and a group of one renders as that
one.

> **Non-normative example — Big Asteroid.** Its two temperature steps, four titanium, and optional
> opponent plant loss form a comma-separated group. The card does not say one waits for another;
> interpreting commas as a sequence would invent timing and change what later triggers can observe.

**L6-9. `A THEN B` says A happens before B.** The relation is stated between the two changes
themselves and is right-associative, so `A THEN B THEN C` is one sequence of three stages rather than
nested pairs. Every stage before the last must be a single instruction: a group or another sequence
on the left is rejected. A group is only an envelope around independent instructions (L6-8) — there
is no shell around `(A, B)` for a `THEN` to relate to, and nothing that "before" could name. What
waiting means for pending work is `SEQUENCING.md`'s subject.

> **Non-normative example — solo neutral tiles.** Each placement pairs a city with a subsequent
> greenery adjacent to a city. `THEN` ensures the new city exists before the greenery's legal-area
> query is settled; a comma would let the second choice be evaluated against the old board.

**L6-10. `EACH Selector { body }` quantifies over one state.** It denotes one independent branch of
`body` for each component occurrence matching `Selector` present in the state. A marker on
`Selector` explicitly makes that occurrence's concrete type available through the same marker on
`SelectorRoot` in the body;
an argument-free marked reference retains the selector's dependency arguments through elaboration,
while the selector's refinement filters candidates but is not part of that exposed value. The
selector's scope includes nested sequences and full transmutations, which claim only matching
markers not already supplied by the selector. Other body expressions retain their ordinary
meanings. Equal occurrences produce equal but independent branches. The selector may serve only as
the repetition source, so the body need not name the selected component. The body may not be empty,
fanouts do not nest, and a concrete selector is rejected where the fanout is resolved against a
world.
`EACH.md` specifies how and when that world is enumerated. This module pins the syntax and scoping;
`engine/EachSelectorOwnerTest.kt` and `engine/InstructionResolutionTest.kt` pin the rest.

> **Non-normative example — map setup.** `EACH Class<@MarsArea> { @MarsArea }` creates one
> component of every concrete area Class. The selector explicitly exposes the represented Class to
> the body.

**L6-11. `I BY Actor` names who performs the change.** It distributes over a group, so
`(A, B) BY Player1` is `A BY Player1, B BY Player1`. Attribution itself is `IDENTITY.md`'s subject.

> **Non-normative example — solo reserve mirroring.** When a player gains or loses a resource, the
> neutral solo reserve performs its matching change `BY Admin`. The actor mark prevents that mirror
> from being mistaken for another player action and recursively mirrored; canonical source does not
> currently need the group-distribution shorthand.

**L6-12. A transmutation may be written compactly when both sides share a class.**
`Foo<Same, Here, To FROM From>` is `Foo<Same, Here, To> FROM Foo<Same, Here, From>`. Exactly one
argument may change. The compact form remains one `FROM` expression while it is open: each
unchanged argument occurs once, and the gained and removed Types are projections of that one tree.
Narrowing replaces a retained argument once, so the two projections cannot acquire different
values. Execution reads the two Types only after the instruction is concrete.

> **Non-normative example — Air Raid.** `5 MC<Owner FROM Anyone>` transfers five MC by changing only
> the ownership argument. Compact transmutation preserves the resource class and amount on both
> sides, so the card cannot accidentally remove one currency and grant another.

**L6-13. Precedence, tightest first:** a scaled expression and its quantifier, `/`, `BY`, `OR`, the
gate `:`, `THEN`, `,`. Parentheses group, and rendering re-inserts grouping wherever re-parsing
would otherwise read the tree differently — including around a transmutation written in full inside
an `OR`, whose bare `FROM` would be ambiguous.

> **Non-normative example — Pharmacy Union.** Its science-tag rule combines a transmutation, a
> state-gated fallback sequence, and an `OR`. The precedence ladder—and the renderer's extra
> parentheses—keeps “spend a disease for TR” separate from “if none remain, archive the corporation
> and gain three TR.”

**L6-14. `X` is one open amount per instruction, and may span a sequence.** The stages of a `THEN`
share one `X`, and must then agree about its value (L7-7); so do an action's cost and result (L9-2),
and a trigger and the instruction it triggers (L8-5).

A group links nothing (L6-8), so it neither joins nor separates the `X`s inside it. Each member's
`X` is simply the one introduced around the group, if there is one, and otherwise that member's own
open amount. In `X Foo, X Bar` the two amounts are unrelated; in `X Qux: X Foo, X Bar` both are the
trigger's amount, and so equal — through the trigger, not through the comma.

> **Non-normative example — Public Plans.** It reveals `X` cards from hand, returns those same `X`
> cards, then grants `X` MC. Sharing the count across the sequence makes the payout equal the number
> temporarily revealed.

**L6-15. A `THEN` sequence marks any Type choice shared across stages explicitly.** Matching
marked occurrences in two or more stages use one choice. At least one occurrence must
choose or match a value; an observing occurrence in a requirement, metric, or refinement may appear
before that supplying occurrence. Other expressions belong only to the stage where they are written.

> **Non-normative example — neutral solo tiles.**
> `@CityTile<> THEN GreeneryTile<LandArea(HAS Neighbor<@CityTile>)>` makes the greenery
> adjacent to the city just placed. `ProjectCard THEN -ProjectCard` declares no variable, so the
> drawn and discarded cards are independent choices.

**L6-16. A full transmutation marks any Type choice shared by its two sides explicitly.** Matching
marked occurrences on the gained and removed sides use one choice. A non-observing
marker on either side may supply that choice; the other side may use it in an observing refinement.
The pair is settled atomically. Other expressions belong only to the side where they are written. A
marker already supplied by an enclosing `EACH` or `RANK` scope remains a reference to that value;
the full transmutation claims only otherwise-unbound matching markers. A marker occurring on only
one side may instead belong to an enclosing `THEN` sequence when that sequence also marks it.

> **Non-normative example — Kaguya Tech.**
> `CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>` replaces a greenery with a city in
> that same area. The unmarked form chooses its source and destination areas independently.

> **Non-normative example — Market Manipulation.**
> `ColonyProduction(NOT Source@ColonyProduction) FROM Source@ColonyProduction` chooses a source
> colony track and excludes that same track from the destination choice.

---

## 7. Narrowing: what remains open

An authored instruction usually leaves something open — an abstract type, an unfixed count, a choice
between alternatives. **Narrowing** is the relation "this more specific instruction is an acceptable
way of carrying out that more general one": P narrows Q when every change P can bring about is one Q
could have brought about, and every choice P still leaves open is one Q left open. The rules below
are that relation, decided from the two instructions alone.

**Narrowing is not resolution.** Settling `3 Plant.` against a state with room for two yields
`2 Plant!`, and that is not a narrowing: nothing in the two instructions says so, and in another
state the same `.` settles differently. Narrowing is what a settler may choose; resolution is what a
state decides, and it is `QUANTIFIERS.md`'s subject. Every rule here is state-independent, except
where a `HAS` refinement asks a world about a candidate (T8-8).

This section is about *elaborated* instructions. An authored change carries no quantifier until
elaboration supplies one (L6-3), and narrowing has nothing to compare until it does.

**L7-1. An instruction is abstract when something is still open.** `isAbstract` reports an unfixed
`X`, an absent or optional quantifier, an abstract expression (T5-3), or an `OR`. `2 Plant<Player1>!`
is not abstract; `2 Plant<Player1>?` is, because doing less is permitted and nothing has said how
much less.

> **Non-normative example — Virus.** Its `-2 Animal<Anyone>? OR -5 Plant<Anyone>?` remains open even
> after one arm is selected: the target player and how much optional loss actually occurs still need
> settlement. Calling the authored card concrete would skip choices printed on it.

**L7-2. A proposal must have the same shape.** A narrowing preserves the kind of node, the number of
`THEN` stages and the size of a group. Two exceptions, and only two: any instruction may narrow an
`OR` by narrowing one of its arms, and `Ok` may narrow an optional change.

> **Non-normative example — Flooding.** Choosing its ordinary ocean arm may replace the surrounding
> `OR`; choosing the compensation arm must retain both its ocean-placement and payment stages.
> Permitting arbitrary shape changes would let a proposal keep the attractive half of that sequence.

**L7-3. A change may narrow its count, its quantifier and its types.** The count may not grow, and
may shrink only under `?`. Each written expression must narrow the authored one (T6-2).

Only `?` leaves a choice, so only `?` narrows: it may become `!`, `.` or a smaller count. `!` and
`.` leave no choice at all, so neither narrows to the other (L6-3). In particular a proposal may not
turn `3 Plant.` into `3 Plant!`: as much as possible is not the same claim as all of it, and which
amount `.` means is for resolution, not for the settler, to decide.

> **Non-normative example — Comet.** Its optional loss of up to three plants may narrow to one, two,
> three, or none, but its mandatory temperature and ocean gains may not shrink. The quantifier—not
> merely the number—carries that printed asymmetry.

**L7-4. `Ok` narrows an optional change and nothing else.** Declining `2 Plant?` entirely is a
narrowing; declining `2 Plant!` or `2 Plant.` is not.

> **Non-normative example — Virus.** If no useful opponent target is chosen, `Ok` is a valid
> realization of the selected optional removal. Letting it replace a mandatory or
> as-much-as-possible loss would turn “may remove” into a universal escape hatch.

**L7-5. A gate, a `/` metric, a `BY` actor and an `EACH` selector are not choices.** A proposal
must reproduce each of them exactly; only what they contain may narrow.

> **Non-normative example — Saturn Surfing.** Its payout is scaled by the floaters on that card and
> capped at four, with one bonus MC alongside it. A proposal may choose an open target or count, but
> may not improve the deal by narrowing the payout metric to a more favorable formula.

**L7-6. An `OR` is satisfied by any arm.** A proposal narrows an `OR` when it narrows at least one
alternative; an `OR` narrows an `OR` when every one of its alternatives does.

> **Non-normative example — Artificial Photosynthesis.** The card offers plant production or two
> energy production. Either concrete choice satisfies the authored `OR`; a proposed `OR` is safe
> only when every arm remains one the card actually offered.

**L7-7. `X` takes one value everywhere it appears, and that value is at least one.** Each
occurrence receives the value multiplied by its own coefficient, so `X Plant THEN 2X Heat` may
become `3 Plant THEN 6 Heat` but not `3 Plant THEN 5 Heat`. A proposed count that is not a multiple
of the coefficient is rejected, and so is zero: an instruction that offers `X` offers a real amount,
never a way to do nothing. Declining belongs to `?` and `Ok` (L7-4).

> **Non-normative example — Energy Market.** `2X MC -> X Energy` permits 2, 4, 6, … MC for 1, 2, 3,
> … energy. A three-MC proposal cannot be reconciled with the coefficient and must not round into a
> transaction the card never offers.

**L7-8. A shared type variable takes one value everywhere it appears.** Narrowing a sequence,
action, or transmutation with a shared variable must supply one consistent value for it (T13-7); two
different values are rejected. A sequence, Action, or full transmutation marks that variable with
the same marker at each occurrence (L6-15, L6-16, L9-8). Selecting one `THEN` stage binds that
value in every later stage, including when the selected instruction chose an arm of an `OR`.

> **Non-normative example — Utopia Invest.**
> `PROD[@StandardResource] -> 4 @StandardResource` means reduce one chosen production
> track and gain four units of that same resource. Binding the two
> occurrences independently would allow trading steel production for four plants.

**L7-9. `narrows` is the boolean form of `ensureNarrows`.** The former answers, the latter throws
`NarrowingException` and says why. A failure caused by something other than narrowing — an unknown
class, a malformed proposal — is not converted into a "no".

> **Non-normative implementation note — bad input is not an unavailable move.** A proposal naming an
> unknown card or malformed expression indicates a caller error. Returning ordinary false would
> make broken input indistinguishable from a well-formed action the current state simply forbids.

**L7-10. Groups narrow elementwise.** Members are matched by position, and the sizes must agree.
A group is not itself pending work — its members become independent tasks, each narrowed on its own
— so this rule reaches only a group nested inside an `OR` arm or a `THEN` stage.

> **Non-normative implementation note — position is a key, not an order.** A group's member order
> carries no gameplay meaning (L6-8); matching by it is only how a proposal says which member it is
> speaking about. Matching members up by content instead would be ambiguous exactly where it
> matters: with `3 Metal!, 4 Steel?` a proposal of two steel gains could pair either way, and the
> two members may carry different continuations.

---

## 8. Effects

An effect is a rule attached to a class: `CityTile: 2 MC` says that whenever a city tile appears,
this component's owner gains 2 MC. Every component of that class carries the rule for as long as it
exists.

**L8-1. An effect is a trigger, a colon, and an instruction.** The trigger says which changes the
rule is about; the instruction is the change the rule then requires. A trigger is decided against
the state its event produced — that goes for the expression it matches, for a `HAS` refinement
inside that expression, and for an `IF` condition (L8-7) alike.

**L8-2. `::` marks an automatic effect** — a consequence carrying no choice, which the rule intends
to be inseparable from the event that caused it. It is greedy: every automatic consequence of one
event is carried out before any queued (`:`) effect of that same event is even tested, so a queued
trigger is decided against a state in which the automatic consequences have already happened. That
is also why an automatic effect can observe an intermediate state that no queued effect ever sees.
What follows from this for pending work is `SEQUENCING.md`'s subject.

> **Non-normative example — Birds.** `This:: AnimalTag<This>` installs the printed animal tag as an
> automatic consequence of the card entering play. Making it an ordinary `:` effect would present a
> fictitious choice to omit an icon physically printed on the card.

**L8-3. There are two kinds of trigger.** `This` and `-This` are about this very component being
gained or removed. Any other expression is a *subscription* to gains, or with a leading `-` removals,
of components matching it.

> **Non-normative example — Tharsis Republic.** `This` grants that corporation's starting package;
> `CityTile<Anyone, MarsArea>` subscribes to every city placement. Confusing the two would either
> replay setup for each city or pay only when the corporation component itself appeared.

**L8-4. A self trigger is not a subscription to its own type.** There is no way to spell one as the
other: writing the bare `This` placeholder as a subscription target *is* the self trigger, however
its empty argument list was written (L3-5). `This` is about changes to this very component, while a
subscription is about changes anywhere that match an expression.

They also scale differently, which is part of what each one means:

- A matching change of n components makes a self trigger's instruction happen n times over: `This: 2
  MC` on a component gained three at once grants 6 MC.
- A subscription is carried by each copy of the effect-bearing component, and each of those
  activations likewise scales by the n of the matching change. Two copies of a card watching a gain
  of three plants react as six.
- An `X` trigger (L8-5) takes that n as the value of `X` instead of multiplying, so the rule reacts
  once and can speak about the size of what happened.

> **Non-normative example — played events.** The generic event rule's `-This` follows the removal of
> that exact face-up event into `PlayedEvent<Class<This>>`. A subscription to the card's type could
> fire once for every matching event-rule bearer instead of closing over the component being removed.

**L8-5. `X` before a trigger's expression binds the size of the change.** `X Plant: X Heat` reacts to
a gain of any number of plants with the same number of heat. A removal is written `-X Plant`.

> **Non-normative example — resource-removal watchers.** `-X Resource<...> BY Player` records one
> removal event whose magnitude is `X`. Preserving that trigger count lets downstream insurance rules
> recognize one removal without subscribing separately to every possible integer amount.

**L8-6. `OR` joins triggers, and self and subscribed triggers may not mix.** `This OR -This` is fine;
`This OR Plant` is not, because one is about this component and the other about the world.

> **Non-normative example — CrediCor.** Its rebate listens to either an expensive card play or use of
> an expensive standard project. Both are subscriptions, so one effect can join them; mixing in
> `This` would combine a one-time setup event with repeatable world events that scale differently.

**L8-7. `BY` restricts a trigger by actor and `IF` by state.** Precedence, tightest first: `OR`,
`BY`, `IF`. Parentheses give one alternative its own qualifier. A `BY` selector is an expression
specialized by the Actor recorded on the event. `BY @Player` explicitly makes that concrete
Player available as `@Player` elsewhere in the Effect. An unmarked or refined selector only filters
the event Actor (T13-9).

> **Non-normative example — Lakefront Resorts.** `OceanTile BY Anyone: PROD[1 MC]` pays its owner
> whenever any player places an ocean. The actor qualifier belongs to the trigger event, while an
> `IF` would ask about board state rather than attribute who performed the placement.

**L8-8. An unqualified subscription on an owned component watches its owner's events.** A rule
printed on a player's card means what the icon means: *yours*. How that is said depends on whether
the watched type has an owner of its own.

- When it does, ownership says it, and no actor restriction is added. `CityTile` on a player's card
  is already `CityTile<Owner>` by L12-4, so it watches that player's cities however they arose.
- When it does not, there is no ownership to say it with, so the rule watches only events that
  player performed: `OceanTile` on a card reacts to the oceans its owner places, not an opponent's.
- A `System` type is exempt: `ProductionPhase` and other Admin-only machinery are the table's own
  events, belonging to no player, and every owner's rule sees them.

Writing any `BY` selector replaces this implicit restriction, which is what `BY Anyone` is for
(L8-7): it says the rule watches everyone's events, including the table's. A rule on a component
with no owner has no such restriction to begin with, and takes its event's Actor instead where its
result needs a player (L12-13). Who is then credited with the resulting change is `IDENTITY.md`'s
subject; this module pins the spellings, and `engine/ByTriggerCharacterizationTest.kt` pins the
matching.

> **Non-normative example — Arctic Algae and Tharsis Republic.** Arctic Algae writes
> `OceanTile BY Anyone: 2 Plant` because it must react to everyone's oceans; without the marking it
> would react only to its owner's. Tharsis Republic needs no such marking on
> `CityTile<Anyone, MarsArea>`: it says whose cities it watches by naming the owner it accepts.

**L8-9. A static non-event may not be a subscribed trigger.** `Class<Foo>: Bar` is rejected: the one
component per concrete class is fixed before any effect runs (T4-6), so nothing ever gains one.
`Ok: Bar`, `-Ok: Bar`, and a subscription rooted at any nominal supertype of `Ok` are likewise
invalid: `Ok` is the identity instruction and produces no change event. A refinement does not make
such an overly broad subscription valid; for example, `Signal(NOT Ok): Bar` is still forbidden.
This does not prohibit a self trigger inherited from such a supertype; there is no `Ok` gain from
which that trigger could fire.

> **Non-normative implementation note — static representatives do not happen.** Canonical cards use
> class literals to name tag, resource, or card kinds, never as events. Accepting the syntax would
> create a subscription guaranteed never to fire and likely conceal a missing ordinary type.

**L8-10. There is no universe-wide subscription.** `Component: Bar` is rejected when it is parsed,
and qualifying it changes nothing: `Component` is a supertype of `Ok`, so `Component IF Foo: Bar`
and `Component BY Anyone: Bar` are rejected when the declaration is loaded, by L8-9.

> **Non-normative implementation note — one consequence, two messages.** An unqualified
> `Component` trigger is caught without a class table so that the error names the real mistake, a
> forgotten type name. The broader ban follows from where `Ok` sits in the hierarchy, and no
> canonical rule wants to watch every component gain anyway.

**L8-11. Effects round-trip, and a gated instruction is parenthesized after the colon** so that the
effect's own colon stays unambiguous.

> **Non-normative implementation note — two colons, two roles.** Rendering must distinguish an
> effect's trigger separator from a requirement gate inside its result. Without parentheses, parsing
> the rendered form could attach the gate to the trigger and produce a different rule.

**L8-12. Matching marked occurrences explicitly identify an Effect-local Type variable.**
Following L3-9, a matching expression in the trigger supplies the value and the instruction must
contain the same marker. Requirements, metrics and refinements may observe that
value without supplying it. The marked trigger occurrence's complete structural expression is the
variable's bound.

For example, Manutech writes `PROD[@StandardResource]: @StandardResource`: the production
increase supplies the resource kind, and the instruction shares that choice.

---

## 9. Actions

An action is a rule a player may invoke: `Steel -> 5 MC` offers to turn one steel into 5 MC.

**L9-1. An action is an optional cost, an arrow, and an instruction.** The cost is written without a
minus sign; it is understood to be given up.

**L9-2. An action means: spend the cost, then do the result.** `cost -> I` denotes `-cost! THEN I`,
and a costless action denotes just `I`. The `THEN` is L6-9's, with nothing added, and the cost and
the result share one `X` because they are its two stages (L6-14).

That is what the arrow means *in this language*. A Catalog may then rewrite the lowered cost into
something it considers the same act — Terraforming Mars replaces a standard-resource cost with a
debt that other rules may discount and that several tenders may settle. Such a rewrite changes
meaning and belongs above this specification, with the Catalog that performs it (`ACTIONS.md`); Pets
defines the form it starts from.

> **Non-normative example — the Aquifer standard project.** `18 MC -> OceanTile<>` must remove all
> 18 MC before offering the placement. Lowering the arrow to mandatory payment followed by the result
> prevents a player from placing first and discovering afterward that payment cannot complete —
> and gives the Terraforming Mars payment rewrite one instruction to recognize.

**L9-3. A cost is a scaled expression, optionally scaled by a metric, optionally inside a transform
block.** A comma-separated or gated cost is rejected — alternative costs are written as separate
actions, so that each is one thing a player can choose to do.

> **Non-normative example — trading with a colony.** The Trade action exposes separate 9-MC,
> 3-energy, and 3-titanium actions. Treating those as a comma or gated cost would require several
> payments at once or make payment contingent on a result instead of presenting three alternatives.

**L9-4. An action becomes an effect keyed to the action's position on its class.** The nth action of
a class lowers to an effect triggered by `UseAction<This, ActionN>`, and a class may offer at most
three.

The order in which a class writes its actions is therefore significant, exactly as the order of the
boxes printed on a card is: the first action *is* `Action1`, and inserting or swapping actions
renames what a saved game, a replay, or an action-used marker was talking about. Editing a class's
action list is a change to its content, not a formatting choice. Action identity, availability and
payment are `ACTIONS.md`'s subject; the standard-resource cost rewrite that currently rides along in
this module belongs there too, and `Transforming.kt` carries a TODO to move it into `tfm-canon`.

> **Non-normative example — Energy Market.** Its two printed actions lower to distinct `Action1` and
> `Action2` triggers. Keying by position lets an action-used marker distinguish buying energy from
> selling production even though both rules live on the same card component.

**L9-5. A class's effects are its authored effects followed by its lowered actions.**

> **Non-normative implementation note — stable inspection order.** No card may rely on this ordering
> to resolve simultaneous gameplay; sequencing owns that question. The rule makes introspection and
> diagnostics deterministic after actions become ordinary effects.

**L9-6. An instruction that happens on gain is the effect `This: I`.** This is how a card's "do this
now" section becomes an ordinary rule; an immediate `Ok` produces no effect at all.

> **Non-normative example — Protected Valley.** Its immediate production increase and special greenery
> placement are written `This: ...`, so they happen when that card component enters play. Treating
> the instruction as free-floating would offer the bonus without playing the card.

**L9-7. Actions round-trip.** The cost keeps its authored form, and the result's grouping is
L6-13's.

**L9-8. An Action marks any Type choice shared by its cost and result explicitly.** Matching
marked occurrences of one bound Class on both sides of the arrow use one choice, supplied by a matching or
choosing occurrence in the cost. Requirements, metrics and refinements may observe that value
without supplying it. Other expressions belong only to the side where they are written.

> **Non-normative example — Utopia Invest.**
> `PROD[@StandardResource] -> 4 @StandardResource` lowers one production track and gains
> four of that same resource.

---

## 10. Transform blocks

A transform block marks a subtree for rewriting by a named handler. `PROD[...]` is the one every
card uses: inside it, `Plant` means plant *production*.

**L10-1. A block is an all-caps kind name, square brackets, and one node.** The kinds of node that
accept a block are instruction, action cost, metric, requirement and trigger.

> **Non-normative example — Mine.** `PROD[Steel]` marks steel as a production-track change rather
> than a steel-cube gain. Keeping the mark around one typed node lets the production handler rewrite
> the noun without giving brackets general statement-like semantics.

**L10-2. Every mark names a kind its Catalog defines.** A declaration using a kind for which the
Catalog supplies no handler is rejected when it is loaded. One rewriting pass may still leave
another pass's kind in place — a Catalog can dispatch one kind while assembling declarations and
another only once a class table exists — but a mark that no pass will ever claim is a mistake in
the source, not a message to some later stage.

> **Non-normative implementation note — a typo should not survive to runtime.** `PORD[Plant]` used
> to be carried along silently and to fail much later, if that branch was ever reached at all.
> Checking the Catalog's kinds once, at load, catches it where the source is.

**L10-3. A handler rewrites only inside its own block**, and what it returns must belong to the same
category of Pets it was given: an instruction for an instruction, a metric for a metric, and so on.
It need not be the same *kind* of node — a gain may come back a group, a requirement may come back a
conjunction — and a block that expands into several independent instructions splices into the
surrounding group (L6-8). A block that expands into a sequence at the final stage of another
sequence likewise splices into that surrounding sequence (L6-9).

> **Non-normative example — Noctis City.** `PROD[-Energy, 3 MC]` expands into two independent
> production-track changes. Splicing the returned group preserves the card's surrounding gains;
> wrapping the pair as one alien node would break ordinary instruction narrowing.

> **Non-normative example — resource differences.** `PROD[StandardResource(NOT MC)]` becomes
> `Production<Class<StandardResource>(NOT Class<MC>)>`. Production represents its resource kind
> with a class literal, so both sides of a resource difference move into that representation. The
> two sides must retain the same resource dependencies; a difference between distinct owners, for
> example, cannot be represented by the class literal and is rejected.

**L10-4. A trigger block wraps only a gain or removal**, never `OR`, `BY` or `IF` — the mark applies
to the event being watched, not to the restrictions on it.

> **Non-normative example — Manutech.** `PROD[@StandardResource]: @StandardResource` listens
> for a production increase of a chosen resource. If `PROD` swallowed `BY` or `IF`, the production
> handler would be asked to rewrite actor attribution or state conditions that are not production
> changes.

**L10-5. Nesting a block inside a block of the same kind is representable but not processable.** The
syntax admits `PROD[PROD[Plant]]`; any handler for that kind rejects it, because the second mark
could only mean what the first already means.

> **Non-normative implementation note — no double production.** No card has “production of
> production.” Rejecting the nested mark catches accidental double-wrapping rather than interpreting
> it as a second multiplier or silently applying the same rewrite twice.

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

> **Non-normative example — Inventrix.** Its compulsory starting card draw needs a private
> `RequiredAction` component with its own action. Declaring that class at the gain site keeps the
> one-card rule next to its use without requiring an otherwise meaningless global name.

**L11-2. The generated name is the owner's name, an underscore, and the base class name.** The
occurrence becomes that name, so the effect above is `This: Inventrix_RequiredAction`, and the
generated declaration is `CLASS Inventrix_RequiredAction : RequiredAction`.

> **Non-normative example — Mining Rights.** `SpecialTile<> {}` becomes
> `MiningRights_SpecialTile`, which the card's later placement-bonus test names explicitly. A stable
> owner-derived name connects the inline tile definition to that later effect without ordinals.

**L11-3. The body follows the complete expression.** Arguments specialize both the occurrence and
the generated class's declared supertype; refinements constrain only the occurrence and are removed
recursively from the supertype, because a refined type cannot be a supertype (L1-8).

Within an argument, `This` still denotes the enclosing owner: the occurrence retains `This` to name
that owner instance, while the generated class's supertype names the enclosing owner Class.

```pets
SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {}
```

becomes the occurrence `MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>` and declares
`CLASS MiningArea_SpecialTile : SpecialTile<LandArea>`.

> **Non-normative example — Mining Area.** The local tile's occurrence must retain “adjacent to an
> owned tile” for placement, while its generated supertype retains only `LandArea`. Dropping the
> refinement everywhere would allow illegal placement; keeping it in the signature would make the
> class state-dependent.

**L11-4. The local body may contain invariants, properties, effects and actions**, and may not
contain `DEFAULT` clauses or nested declarations. The generated class inherits applicable defaults
from its supertypes like any other.

**L11-5. Owner-local classes do not nest.** Neither a local body nor an argument of the occurrence
may declare another one.

> **Non-normative implementation note — one lowering boundary.** No canonical card needs an unnamed
> class owned by another unnamed class. Rejecting nesting prevents generated names such as
> `Card_Signal_RequiredAction` whose ownership and later reference point would be ambiguous.

**L11-6. One owner declares at most one unnamed local class per base name.** A second is rejected
rather than distinguished by an ordinal, so the derived name stays stable and meaningful. Two local
classes with the same natural suffix must be declared explicitly.

> **Non-normative implementation note — no positional identities.** A card needing two different
> signals or special tiles must name them. Inventing `_2` would make class identity depend on source
> order, so inserting an earlier local declaration could retarget saved components and references.

**L11-7. The syntax is available only where a declaration file is being read.**
`parseOneLinerClass` rejects it, and `Parsing.parse` parses and validates it before rejecting it with
`PetSyntaxException`, because a submitted instruction has no definition owner and a live game's
class table is frozen (T1-6).

> **Non-normative implementation note — submitted moves cannot extend the game.** A player may
> choose among classes already in the Catalog, but cannot submit `SpecialTile { ... }` to create a
> new one midgame. Parsing far enough to issue the specific error keeps this distinct from malformed
> syntax.

**L11-8. Naming the base class alone still means the base class.** An occurrence with no local body
is an ordinary expression; it does not resolve to some nearby derived class.

> **Non-normative example — Mining Rights.** After its inline tile declaration lowers, later tests
> explicitly say `MiningRights_SpecialTile`; bare `SpecialTile` still means the base class. Otherwise
> one nearby local declaration could silently change unrelated expressions in the same card.

---

## 12. Elaboration

Authored Pets is not yet the form that sections 4 to 9 describe. **Elaboration** is the
class-table-dependent rewriting that fills in what a physical game leaves implicit — that a tile goes
on a land area, that a resource belongs to the player doing the thing, that "gain 3 cards" means
three separate cards. It changes how a source *reads*; it never changes which types exist, which is
what section 10 of the type system specification means by a default not being a bound.

**L12-1. Elaboration is one rewriting, in one order, of an element against a context.** The stages
are, in this order: record Type-variable scopes (T13-6 through T13-9); split atomized gains (L12-11);
insert defaults (L12-4 through L12-10); bind the contextual owner (L12-3); dispatch transform blocks
(section 10); expand property evaluations (L12-12).

Where the Pets came from does not change that order. It supplies the context, and two things follow
from the context rather than from a different pipeline:

| | An element a player submits | A class's own effects |
| --- | --- | --- |
| The context is | `This` — the submitting player's own scope | the class's own context |
| Contextual owner | bound to the submitting player | left open, and `BY Owner` added where the result needs one (L12-13) |
| Property evaluations | rejected, except in a metric (L12-12) | expanded once the receiver is concrete |

> **Non-normative example — player setup.** `10 ProjectCard` must become ten independent card gains,
> each defaulted to the setting-up player. Atomizing before defaulting means each card is defaulted
> in its own right, rather than one aggregate pseudo-card being split afterward.

**L12-2. `This` is replaced by the context expression.** `Class<This>` becomes the class literal for
the context's class, and `This<Foo>` keeps its own arguments while adopting the context's class.

> **Non-normative example — Asteroid.** The inherited event cleanup creates
> `PlayedEvent<Class<This>>`. Specializing the rule for Asteroid must produce
> `Class<AsteroidCard>`, while ordinary `This` occurrences still close over the exact owned card
> component.

**L12-3. `Owner` is replaced by the context owner**, everywhere except inside the body of an `EACH`
whose selector is itself an owner — there the selection supplies the owner instead, so an ordinary
owned body reads on a card exactly as it does anywhere else. The selector itself is not shielded: it
names components in the enclosing context, so `EACH ProjectCard<Owner> { ... }` means the cards the
enclosing owner holds. A `RANK` selector shields nothing.

> **Non-normative example — Sponsored Academies.** `EACH Player { ProjectCard }` gives a card to each
> selected player. If the enclosing card's owner replaced `Owner` inside that body, every branch
> would give its card to the same player instead of the player selected for that branch.

**L12-4. Every expression receives its class's all-use dependency defaults** (T10-1), recursively.

> **Non-normative example — Ecoline.** Its starting `3 Plant` omits an owner because ordinary owned
> resources default recursively to the corporation's player. Without all-use defaults, the gain
> would remain an abstract pile of plants belonging to nobody.

**L12-5. A gain or removal also receives the defaults for its use kind**, and a gain must opt in.
When a class has gain dependency defaults, a gain may not leave its argument list implicit: write
`OceanTile<>` to accept them, or supply at least one argument. This keeps a defaulted placement
visible at the point of use.

Where both sets speak to one dependency key, the use-kind default wins: it is the more specific
statement about what this use means. The all-use set then fills only the keys it left open. A
use-kind default that merely restates the declared bound records nothing at all (T10-4), so it
cannot be used to *cancel* an all-use default for that key.

> **Non-normative example — Subterranean Reservoir.** The card writes `OceanTile<>` to advertise that
> its ocean will use the standard empty-water-area placement default. Allowing bare `OceanTile`
> would conceal a board choice behind syntax that elsewhere merely names the type.

**L12-6. A removal declines its use-specific defaults by writing nothing.** An implicit argument list
on a removal is not an error: it simply does not receive the removal-only dependency defaults, though
all-use defaults (L12-4) still apply. `-Marker<>` accepts them.

> **Non-normative example — debt removal.** The symmetric rule — a gain *or* removal must opt in —
> is the tempting one, and it is wrong. Requiring it of removals rejects `-Owed`, written bare in
> the action payment lowering, and with it `CryoSleepTest`, `AridorTest`,
> `DistantPressureMassTest`, three whole-game replays and two integration suites. A removal names a
> component that already exists; there is no placement left to default.

**L12-7. `Foo<>` is invalid where that use has no dependency defaults to accept.** An empty list is
an acceptance, not merely a second spelling of the same expression.

> **Non-normative implementation note — visible intent must correspond to something.** `Plant<>`
> cannot honestly mean “accept the plant placement defaults,” because there are none. Rejecting it
> catches cargo-cult opt-in syntax instead of preserving a misleading no-op distinction.

**L12-8. The gained and removed projections of `A FROM B` are defaulted independently.** Compact
syntax remains compact; defaulting its two projections does not duplicate a retained argument.
Where the transmutation writes no quantifier of its own, it must satisfy both projections' defaults
at once, and takes the one quantifier that permits exactly the amounts both of them permit.

That is not a ranking of the three quantifiers (L7-3 keeps `!` and `.` incomparable); it is what
their policies leave in common. `!` permits only the full count; `.` permits only the most the state
allows; `?` permits anything up to that. So `!` with `.` permits the full count when the state
allows it and nothing otherwise, which is `!`; `.` with `?` permits only the most possible, which is
`.`; and `!` with `?` is `!`. Written as a table, the combination is:

| | `!` | `.` | `?` |
| --- | --- | --- | --- |
| **`!`** | `!` | `!` | `!` |
| **`.`** | `!` | `.` | `.` |
| **`?`** | `!` | `.` | `?` |

> **Non-normative example — Public Plans.** `ProjectCard<Revealed FROM Hand>` changes a card's
> location while retaining its contextual owner. Defaulting the gained and removed card types
> independently preserves both locations; defaulting the transmutation as one expression could
> overwrite the very argument that changes.

**L12-9. Inside a `HAS` refinement, a bare dependent expression reserves a slot for the candidate.**
It keeps the first dependency position that could accept the refined domain free, so that candidate
substitution (T8-3) binds it: `Player(HAS StartToken)` asks `StartToken<p>` of each candidate `p`,
even though `StartToken` inherits a contextual owner default. The explicit spellings remain
available: `StartToken<Owner>` requests the contextual owner and `StartToken<>` accepts the default.

> **Non-normative example — the starting-player marker.** `Player(HAS StartToken)` must test each
> candidate player's own marker. Reserving the owner slot lets candidate substitution fill it;
> eagerly inserting the enclosing owner would ask whether every candidate has one particular
> player's token.

**L12-10. Inside a refinement, a default is deferred when its dependency is a direct use of a
class-header type variable** (T13-2), so that candidate substitution can bind it through that
occurrence. Writing `<>` still accepts the default explicitly.

> **Non-normative example — CEO's Favorite Project.** `CardFront(HAS CardResource)` asks whether each
> candidate card can hold a resource. Deferring the resource holder's header-variable default lets
> the candidate card fill that slot; eager owner defaulting would ask about a generic resource owned
> by the enclosing player instead.

> **Non-normative design note — candidate binding precedes defaulting.** Rules L12-9 and L12-10 are
> consequences of one precedence: a refinement first reads its requirement about the candidate, and
> only then may omitted dependency context receive a default. The candidate is not concrete until the
> refinement is tested, so elaboration implements that precedence by reserving or deferring the
> affected slot. This is staging of one implicit-argument rule, not a second meaning for refinements.

**L12-11. A gain of several `Atomized` components becomes several gains of one.** `3 ProjectCard`
becomes three independent gains, because three cards are three separate things to choose.
When a counted gain is a left stage of `THEN`, its gains become consecutive stages; a counted gain
at the end remains a group after the preceding stage.

> **Non-normative example — Inventrix.** Its required action draws three project cards. Atomizing the
> gain creates three independent card identities and choices; a single “three-copy ProjectCard”
> change could not represent three different faces drawn from the deck.

**L12-12. `EVAL` includes a class property's own syntax where it is written.** `EVAL Goal.score`
expands to the metric that class's `score` property holds, with `This` inside it bound to the
property's class. It needs a receiver context, so it is expanded in a class effect and in a
submitted *metric*, which is given one, and rejected in an ordinary submitted instruction, which is
not. An evaluation whose receiver is still abstract stays unexpanded until it is not, and a property
that would expand into itself is rejected.

> **Non-normative example — Landlord scoring.** The generic award rule uses `EVAL Award.metric`.
> Once Landlord is selected, elaboration inserts its `COUNT "OwnedTile"` syntax with the award's
> context; evaluating too early would have only the abstract `Metric` property bound.

**L12-13. A class's effects are elaborated against that class's own context.** They are gathered from
every superclass, and an effect on a class that is neither an owner nor owned, whose instruction
needs an owner, has `BY Owner` added to its trigger — that is how an unowned rule learns whose event
it is reacting to.

> **Non-normative example — placement bonuses.** A map area is neither a player nor owned, yet its
> `Placement<This>: Plant` rule must give the plant to whoever placed there. Adding `BY Owner` to the
> trigger captures that actor instead of leaving the reward ownerless or assigning it to the area.

**L12-14. Changes to uninhabited Types become `Die` or `Ok`.** After specialization, a change whose
Type expression violates a dependency bound (T3-4, T3-5) becomes a gain of `Die`. A valid change
whose Type is uninhabited (T12-4) becomes `Die` when mandatory and `Ok` when its quantifier permits
zero. An expression containing an open Type variable is not tested for inhabitance until that
variable is bound; specialization may give the expression a nonempty domain (T8-7).

> **Non-normative example — cross-expansion branches.** Cimmeria grants a colony only in a game
> containing the Colonies expansion. If specialization reaches that branch in another game, a
> mandatory colony gain becomes `Die`, while an optional gain becomes `Ok`.

**L12-15. Specializing an effect closes it over one exact component.** Given a component's type,
`specializeEffect` binds the class's type variables (T13-5), the `This` context and the contextual
owner together, in one step.

> **Non-normative example — Tharsis Republic.** Its generic city trigger belongs to one concrete
> corporation component. Closing over that component and its owner ensures the MC production and
> rebate go to the player holding this copy of Tharsis Republic, not every player with the same rule.

---

## Appendix A: deliberately unspecified

- **Which parentheses a renderer adds beyond the ones round-tripping requires.** Rendering is
  required to round-trip (L4-10, L5-10, L6-13, L8-11), not to be minimal, and today it is not
  minimal.
- **Exception messages.** Rules name exception *types* where the type is part of the contract.
- **The order in which elaboration visits nodes.** Only the order of the stages (L12-1) is
  specified.
- **Where a transform handler comes from**, and what any particular kind such as `PROD` rewrites.
  Section 10 specifies the mark and the contract every handler owes (L10-3), not what any one kind
  means; that belongs to the Catalog defining it.
