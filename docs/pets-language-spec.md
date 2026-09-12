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

> **Non-normative example — the card taxonomy.** Core source declares `CorporationCard,
> ProjectCard`, `ActiveCard, AutomatedCard`, and `Action1, Action2, Action3` in compact groups. Bodies
> are forbidden because one shared body would make it unclear whether its rules belong to every
> generated class or only the last signature.

**L1-4. A body is brace-delimited, and its elements are separated by newlines or by semicolons.** A
body element is an invariant (`HAS r`), a `DEFAULT` clause, a property assignment (`name = value`),
an effect, or an action. A newline-separated body may also contain nested declarations; a
semicolon-separated one may not.

```pets
CLASS GreeneryTile : Tile { HAS MAX 1 This; This: OxygenStep }
```

> **Non-normative example — map spaces.** Generated map declarations such as `Hellas_1_4` keep row,
> column, and placement bonus in one semicolon-separated body. Allowing nested declarations in that
> form would make a “one physical space per line” record expand into invisible sibling classes.

**L1-5. A nested declaration becomes a sibling that names its container as a supertype** (T2-2).
The container is returned first, then its nested declarations in source order, recursively.

> **Non-normative example — cards and locations.** `CorporationCard` and `ProjectCard` are written
> inside `CardBack`, itself inside `Card`, but the type table needs ordinary globally named classes.
> Lowering nesting to sibling inheritance preserves the readable taxonomy without creating a
> namespace the rest of Pets does not have.

**L1-6. A docstring is a quoted string on the line before `CLASS`.** It is retained on the
declaration (T2-1) and re-emitted when the declaration is rendered.

**L1-7. `DEFAULT` clauses name the class that declares them** (T10-3) and are merged into one set
per use kind (T10-1). Separate compatible clauses may supply the dependency arguments and quantifier
of one use-kind default. Clauses that disagree about their class, dependency arguments or quantifier
are rejected; declaration order never selects a winner. A clause naming another class is rejected.

> **Non-normative implementation note — defaults have one source.** No card needs to install a
> remote class's default. Permitting it would let an unrelated expansion silently change what bare
> `OceanTile<>` or `CardBack` means merely by being loaded later.

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

> **Non-normative implementation note — reject conflicting print data early.** A card cannot have
> two printed costs or two milestone requirements. Rejecting the second assignment at parse time
> prevents declaration order from becoming an accidental override rule.

**L1-9. Signature expressions carry no refinements**, at any depth. A dependency bound or supertype
written `Foo(HAS Bar)` or `Foo(NOT Bar)` is rejected, because a refined type cannot be a bound.

> **Non-normative example — Mining Area.** The card declares a local special tile at
> `LandArea(HAS Neighbor<OwnedTile>)`. The live refinement constrains this occurrence, while the
> generated class extends plain `SpecialTile<LandArea>`; putting the board query in its signature
> would make a state-dependent predicate part of permanent class identity.

**L1-10. Whitespace and comments.** Horizontal whitespace separates tokens and carries no other
meaning: it may appear between any two of them, and no construct depends on how much of it there is.
It is required only where two tokens would otherwise run together into one, so `2 MC` is a scalar and
a name while `2MC` is neither. `//` begins a comment that runs to the end of the line. A backslash
immediately before a line ending continues the line, so one element may span several source lines.
Newlines are significant only as separators (L1-1, L1-4).

> **Non-normative example — Mars Nomads.** Its action moves a marker and then pays every marked
> area's placement bonus. A backslash lets that one action span source lines without a newline being
> mistaken for the end of the body element.

**L1-11. A declaration renders as parseable source, and round-trips.** `toString()` produces a
multi-line declaration and `toString(oneLine = true)` a semicolon-separated one; parsing either
yields an equal declaration.

> **Non-normative implementation note — generated declarations are real source.** Owner-local
> classes and diagnostic output are rendered and parsed again in tests and tooling. Round-tripping
> prevents a formatter from changing an action's grouping or turning a nested class into a body
> element with different ownership.

**L1-12. A declaration can also be parsed on its own.** `Parsing.parseOneLinerClass` accepts exactly
one declaration, with an optional semicolon-separated body, and rejects owner-local class syntax
(L11-7). This is how a declaration embedded in structured card data is read.

> **Non-normative implementation note — one record, one declaration.** Catalog composition accepts
> standalone declarations supplied by structured data. Rejecting a grouped second class or a local
> class keeps that API from smuggling additional globally named types through one record.

**L1-13. Every catalog also receives the system declarations.** `systemClassDeclarations` supplies
the classes this specification and the type system depend on — `Component` and `Class` (T1-4, T1-5),
the ownership vocabulary `Anyone`, `Owner` and `Owned`, the actor root `Actor`, and the signals `Ok`
(L6-4) and `Die` (L12-14) — plus `Atomized` (L12-11) and `Custom` (T2-9). A catalog's own source is
loaded alongside them. Which of these a *game* then contains is `OPTIONS.md`'s question, not this
document's.

> **Non-normative example — impossible and empty outcomes.** Elaboration uses the built-in `Die`
> and `Ok` signals when selected content makes a mandatory result impossible or an optional result
> empty. Supplying them in every Catalog lets that rule work before any Terraforming Mars module
> contributes its own classes.

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

**L2-2. Keywords are reserved and case-sensitive.** `ABSTRACT`, `BY`, `CLASS`, `COUNT`, `DEFAULT`,
`EACH`, `EVAL`, `FROM`, `HAS`, `IF`, `MAX`, `NOT`, `OR`, `RANK`, `THEN` and `X`, together with the
property-value words `Metric`, `Number` and `Requirement`, are the words the grammar itself uses, and
none of them may be a class name. Because the reserved spellings are exact, `Max`, `By` and `Has` are
perfectly good class names.

**L2-3. A property name is lowerCamelCase**: a lowercase letter followed by letters and digits.

**L2-4. A transform-kind name is an all-caps identifier** (section 10).

**L2-5. There is one namespace and no scoping.** A name is not declared, bound or shadowed by any
construct in this document; it means whatever class the class table says it means (T1-1, T1-7).
Which name a concept should get, and how names are displayed to a person, is `NAMING.md`'s subject.

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

**L3-7. An expression renders as the class name, the argument list if one was written, and the
refinement.** Whitespace is not preserved and duplicate refinement clauses collapse, but an authored
expression is not rewritten into its type's canonical form: `Tile` and `Tile<Area>` remain distinct
expressions even though they resolve to one type (T1-3, T5-5).

> **Non-normative implementation note — spelling drives capture.** Type-variable inference records
> authored repetition. Normalizing `Tile` and `Tile<Area>` to one canonical type before that pass could
> falsely turn two deliberately different spellings into one shared player choice.

**L3-8. Two expressions are equal when their structural spellings agree.** Argument order is part of
the spelling, while refinement-clause order and duplication are not (L3-3). Thus
`Microbe<Player1, Ants>` and `Microbe<Ants, Player1>` are different expressions for one type. This is
why the type system, not the syntax, is the authority on identity (T5-1).

> **Non-normative implementation note — syntax is not component identity.** No card distinguishes
> `Microbe<Player1, Ants>` from the reversed argument spelling once resolved. Keeping the syntax
> unequal preserves faithful rendering and exact variable-occurrence tracking without changing the
> game type.

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
for a player to settle, which is also why an abstract expression repeated only inside requirements
declares no type variable (T13-8).

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

> **Non-normative example — Industrial Complex.** Its catch-up production uses metrics such as
> `1 MC / 6 - MC` inside `PROD[...]`. Metric precedence keeps the deficit calculation attached to
> the scaling operation instead of turning the surrounding production group into alternatives.

**L5-8. `receiver.name` reads a class property**, and `EVAL` includes a property's own syntax
(L12-12). A property metric with no receiver takes one from the enclosing refinement candidate or
context.

> **Non-normative example — card payment.** The generic `PlayCard` rule creates debt from
> `CardFront.cost`. Supplying the chosen concrete card as receiver is what turns one generic rule
> into the correct price for every card face.

**L5-9. `RANK Selector { m1, m2, ... }` is a competition rank.** It denotes the highest-first
position of one candidate among the components matching `Selector` in one state, comparing the
listed metrics lexicographically. Authored syntax leaves the candidate open; a refinement supplies
it. At least one metric is required, and the selector's refinement filters the field without
becoming part of the name the metrics use. This module pins the syntax and that scoping; ranking a
live field is realized where a world is available, and pinned by `engine/RankMetricTest.kt`.

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

**L6-2. A count is a positive integer or `X`.** `X` denotes an amount left open, and may carry a
coefficient: `2X Plant` is an even number of plants. A count of zero is rejected.

> **Non-normative example — Sulphur-Eating Bacteria.** `X Microbe<This> -> 3X MC` lets the player
> choose how many microbes to spend while fixing the three-to-one exchange rate. Replacing `X` with
> unrelated open counts would allow the paid and received amounts to drift apart.

**L6-3. A quantifier says how much of the count must happen.** `!` means the whole amount, `.` as
much of it as possible, and `?` any part of it including none. An authored change may omit the
quantifier; elaboration then supplies the class's default (T10-2, L12-5). What "as much as possible"
resolves to against a real state is `QUANTIFIERS.md`'s subject.

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

**L6-5. `I / M` scales a change by a metric's value.** `Titanium / 3 EarthTag` grants one titanium
per three complete Earth tags. Only an elementary change may be scaled this way.

> **Non-normative example — Community Services.** It grants MC production per card with no tags.
> Scaling the one production change by `CardFront(HAS MAX 0 Tag)` creates the aggregate reward;
> scaling an arbitrary sequence would leave unclear which stages repeat and in what order.

**L6-6. `r: I` gates an instruction on a requirement.** The gate is not a choice (L7-5); when its
requirement fails, the instruction cannot be carried out. `OR` binds tighter than a gate, so
`3 PlantTag: Plant OR 4 Plant` gates both alternatives, and a gate on one alternative alone must be
parenthesized. A gate does not directly contain another gate.

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
on the left is rejected, because "before" needs one identifiable change to be before. What waiting
means for pending work is `SEQUENCING.md`'s subject.

> **Non-normative example — Polder Tech Dutch.** Its required action places an ocean and then a
> greenery adjacent to an ocean. `THEN` ensures the new ocean exists before the greenery's legal-area
> query is settled; a comma would let the second choice be evaluated against the old board.

**L6-10. `EACH Selector { body }` quantifies over one state.** It denotes one independent branch of
`body` for each distinct concrete type matching `Selector` present in the state, with the selector's
spelling in the body denoting that type. A refinement on the selector filters which components take
part without becoming part of the name the body uses. The body may not be empty, and fanouts do not
nest. A concrete selector and a body that never names its selection are both meaningless — every
branch would be the same instruction — and are rejected where the fanout is resolved against a
world, which is `EACH.md`'s subject, along with how that world is enumerated and when. This module
pins the syntax and that scoping; `engine/EachSelectorOwnerTest.kt` and
`engine/InstructionResolutionTest.kt` pin the rest.

> **Non-normative example — Mars Nomads.** After moving its marker, the card uses `EACH LandArea(HAS
> NomadsMarker) { Placement<LandArea> }` to award the bonus of the newly marked area. The selector
> both filters the live board and supplies the concrete area used in each branch.

**L6-11. `I BY Actor` names who performs the change.** It distributes over a group, so
`(A, B) BY Player1` is `A BY Player1, B BY Player1`. Attribution itself is `IDENTITY.md`'s subject.

> **Non-normative example — solo reserve mirroring.** When a player gains or loses a resource, the
> neutral solo reserve performs its matching change `BY Admin`. The actor mark prevents that mirror
> from being mistaken for another player action and recursively mirrored; canonical source does not
> currently need the group-distribution shorthand.

**L6-12. A transmutation may be written compactly when both sides share a class.**
`Foo<Same, Here, To FROM From>` is `Foo<Same, Here, To> FROM Foo<Same, Here, From>`. Exactly one
argument may change; the unchanged ones occupy both roles.

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

**L6-14. `X` is one open amount per instruction, and may span a sequence.** Two independent
instructions may not share an `X` (there is nothing to make the two amounts agree), while the stages
of a `THEN` may, and then must agree (L7-7).

> **Non-normative example — Public Plans.** It reveals `X` cards from hand, returns those same `X`
> cards, then grants `X` MC. Sharing the count across the sequence makes the payout equal the number
> temporarily revealed; allowing a comma-separated group to share it would assert equality without
> any temporal operation connecting the choices.

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
may shrink only under `?`. The quantifier may narrow: `?` may become anything, while `!` and `.` are
incompatible with each other. Each written expression must narrow the authored one (T6-2).

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

**L7-7. `X` takes one value everywhere it appears.** Each occurrence receives that value multiplied
by its own coefficient, so `X Plant THEN 2X Heat` may become `3 Plant THEN 6 Heat` but not
`3 Plant THEN 5 Heat`. A proposed count that is not a multiple of the coefficient is rejected.

> **Non-normative example — Energy Market.** `2X MC -> X Energy` permits 2, 4, 6, … MC for 1, 2, 3,
> … energy. A three-MC proposal cannot be reconciled with the coefficient and must not round into a
> transaction the card never offers.

**L7-8. A shared type variable takes one value everywhere it appears.** Narrowing a sequence or a
transmutation that repeats an abstract expression must supply one consistent value for it (T13-6,
T13-7); two different values are rejected.

> **A known gap.** When the repeated expression is written with an empty argument list, the variable
> is declared but never binds, and the stages may diverge after all. Characterized in `LangBugsTest`.

> **Non-normative example — Utopia Invest.** `PROD[StandardResource] -> 4 StandardResource` means
> reduce one chosen production track and gain four units of that same resource. Binding the two
> occurrences independently would allow trading steel production for four plants.

**L7-9. `narrows` is the boolean form of `ensureNarrows`.** The former answers, the latter throws
`NarrowingException` and says why. A failure caused by something other than narrowing — an unknown
class, a malformed proposal — is not converted into a "no".

> **Non-normative implementation note — bad input is not an unavailable move.** A proposal naming an
> unknown card or malformed expression indicates a caller error. Returning ordinary false would
> make broken input indistinguishable from a well-formed action the current state simply forbids.

**L7-10. Groups narrow elementwise.** Members are matched by position, and the sizes must agree.

> **Non-normative example — Deimos Down.** Its temperature steps, steel gain, and optional plant
> loss are independent but not interchangeable fields in a proposal. Positional matching prevents a
> client from reordering or dropping one consequence while still claiming to execute the card's
> complete group.

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
its empty argument list was written (L3-5). The two say different things — `This` is about changes
to this very component, and scales its instruction by the number of copies changed, while a
subscription is about changes anywhere that match an expression, and is carried once per copy of the
effect-bearing component. How many times each actually fires is `ENGINE.md`'s subject.

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
`BY`, `IF`. Parentheses give one alternative its own qualifier. A `BY` selector is an expression, so
`BY Player(NOT Owner)` is a filter and `BY Player` may declare an actor variable (T13-9).

> **Non-normative example — Lakefront Resorts.** `OceanTile BY Anyone: PROD[1 MC]` pays its owner
> whenever any player places an ocean. The actor qualifier belongs to the trigger event, while an
> `IF` would ask about board state rather than attribute who performed the placement.

**L8-8. A class literal may not be a trigger.** `Class<Foo>: Bar` is rejected: the one component per
concrete class is fixed before any effect runs (T4-6), so nothing ever gains one.

> **Non-normative implementation note — static representatives do not happen.** Canonical cards use
> class literals to name tag, resource, or card kinds, never as events. Accepting the syntax would
> create a subscription guaranteed never to fire and likely conceal a missing ordinary type.

**L8-9. A bare `Component` subscription must be qualified.** `Component: Bar` subscribes to
everything and is rejected; `Component IF Foo: Bar` and `Component BY Anyone: Bar` are accepted,
because each states what the rule is actually watching for.

> **Non-normative implementation note — reject accidental global listeners.** No canonical rule
> needs an unqualified subscription to every component gain. Requiring `IF` or `BY` makes a rare
> universe-wide watcher state its filter instead of turning a forgotten type name into trigger spam.

**L8-10. Effects round-trip, and a gated instruction is parenthesized after the colon** so that the
effect's own colon stays unambiguous.

> **Non-normative implementation note — two colons, two roles.** Rendering must distinguish an
> effect's trigger separator from a requirement gate inside its result. Without parentheses, parsing
> the rendered form could attach the gate to the trigger and produce a different rule.

---

## 9. Actions

An action is a rule a player may invoke: `Steel -> 5 MC` offers to turn one steel into 5 MC.

**L9-1. An action is an optional cost, an arrow, and an instruction.** The cost is written without a
minus sign; it is understood to be given up.

**L9-2. An action means: spend the cost, then do the result.** `cost -> I` denotes `-cost! THEN I`,
and a costless action denotes just `I`. That is the whole of what the arrow means; the `THEN` is
L6-9's, with nothing added.

> **Non-normative example — the Aquifer standard project.** `18 MC -> OceanTile<>` must remove all
> 18 MC before offering the placement. Lowering the arrow to mandatory payment followed by the result
> prevents a player from placing first and discovering afterward that payment cannot complete.

**L9-3. A cost is a scaled expression, optionally scaled by a metric, optionally inside a transform
block.** A comma-separated or gated cost is rejected — alternative costs are written as separate
actions, so that each is one thing a player can choose to do.

> **Non-normative example — trading with a colony.** The Trade action exposes separate 9-MC,
> 3-energy, and 3-titanium actions. Treating those as a comma or gated cost would require several
> payments at once or make payment contingent on a result instead of presenting three alternatives.

**L9-4. An action becomes an effect keyed to the action's position on its class.** The nth action of
a class lowers to an effect triggered by `UseAction<This, ActionN>`, and a class may offer at most
three. Action identity, availability and payment are `ACTIONS.md`'s subject; the standard-resource
cost rewrite that currently rides along in this module belongs there too, and `Transforming.kt`
carries a TODO to move it into `tfm-canon`.

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

---

## 10. Transform blocks

A transform block marks a subtree for rewriting by a named handler. `PROD[...]` is the one every
card uses: inside it, `Plant` means plant *production*.

**L10-1. A block is an all-caps kind name, square brackets, and one node.** The kinds of node that
accept a block are instruction, action cost, metric, requirement and trigger.

> **Non-normative example — Mine.** `PROD[Steel]` marks steel as a production-track change rather
> than a steel-cube gain. Keeping the mark around one typed node lets the production handler rewrite
> the noun without giving brackets general statement-like semantics.

**L10-2. A block whose kind has no handler is preserved verbatim**, so a source may carry marks that
a later stage will interpret.

> **Non-normative implementation note — extensible marks.** Every canonical Terraforming Mars block
> currently has a handler, but the language parser does not own that registry. Preserving an unknown
> kind lets a Catalog-specific stage interpret it instead of the generic parser deleting information.

**L10-3. A handler rewrites only inside its own block**, and what it returns must be the same kind of
Pets it was given. A block that expands into several independent instructions splices into the
surrounding group (L6-8).

> **Non-normative example — Noctis City.** `PROD[-Energy, 3 MC]` expands into two independent
> production-track changes. Splicing the returned group preserves the card's surrounding gains;
> wrapping the pair as one alien node would break ordinary instruction narrowing.

**L10-4. A trigger block wraps only a gain or removal**, never `OR`, `BY` or `IF` — the mark applies
to the event being watched, not to the restrictions on it.

> **Non-normative example — Manutech.** `PROD[StandardResource]: StandardResource` listens for a
> production increase of a chosen resource. If `PROD` swallowed `BY` or `IF`, the production handler
> would be asked to rewrite actor attribution or state conditions that are not production changes.

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
recursively from the supertype, because a refined type cannot be a supertype (L1-9).

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

> **Non-normative example — Focused Organization.** Its inline `Signal` has an effect that restores
> the chosen project card and resource after the two costs are paid. Allowing local effects makes
> that temporary protocol expressible; inheriting defaults keeps the local body from installing a
> second, hidden default policy.

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

**L11-7. The syntax is available only where a declaration file is being read.** `parseOneLinerClass`
rejects it, and `Parsing.parse` parses and validates it before rejecting it with
`NoNewClassDeclarationsException`, because a submitted instruction has no definition owner and a
live game's class table is frozen (T1-6).

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

**L12-1. Elaboration is a fixed set of stages, applied differently depending on where the Pets came
from.** The stages are: infer type variables (T13-6 through T13-9); split atomized gains (L12-11);
insert defaults (L12-4 through L12-10); bind the contextual owner (L12-3); dispatch transform blocks
(section 10); expand property evaluations (L12-12).

Two entry points apply different subsets, in different orders:

| | An element a player submits | A class's own effects |
| --- | --- | --- |
| Defaults are inserted against | `This` | the class's own context |
| Order of defaults and atomizing | atomize, then default | default, then atomize |
| Contextual owner | bound to the submitting player | left open, and `BY Owner` added where the result needs one (L12-13) |
| Property evaluations | rejected, except in a metric (L12-12) | expanded once the receiver is concrete |

The shared core — inferring variables, atomizing, defaulting and dispatching — is the same rewriting
in both.

> **Non-normative example — player setup.** `10 ProjectCard` must become ten independent card gains,
> each defaulted to the setting-up player. The fixed stage order prevents owner defaulting from
> happening on one aggregate pseudo-card.

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

**L12-8. The two halves of `A FROM B` are defaulted independently**, and where the transmutation
writes no quantifier of its own, the gained half's gain default and the removed half's removal
default are intersected, the stricter winning: mandatory beats as-much-as-possible, which beats
optional.

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

**L12-11. A gain of several `Atomized` components becomes several gains of one.** `3 ProjectCard`
becomes three independent gains, because three cards are three separate things to choose.

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

**L12-14. A change to a type this game cannot hold becomes `Die` or `Ok`.** A type expression that
becomes invalid when specialization substitutes a dependency outside its declared bound (T3-4,
T3-5) becomes a gain of `Die`, so the invalid branch can never be carried out. When a specialized
instruction instead names a resolved type that is not active (T12-1), a mandatory change becomes
`Die` and a change that permits zero becomes `Ok`. This keeps a rule that mentions absent content
from silently succeeding.

> **Non-normative implementation note — cross-expansion safety.** Cimmeria conditionally grants a
> colony only with the Colonies expansion. If specialization nevertheless reaches an inactive
> mandatory colony gain, `Die` preserves the impossibility; an inactive optional branch becomes
> `Ok` so omitted content cannot masquerade as a successful reward.

**L12-15. Specializing an effect closes it over one exact component.** Given a component's type,
`specializeEffect` binds the class's type variables (T13-5), the `This` context and the contextual
owner together, in one step.

> **Non-normative example — Tharsis Republic.** Its generic city trigger belongs to one concrete
> corporation component. Closing over that component and its owner ensures the MC production and
> rebate go to the player holding this copy of Tharsis Republic, not every player with the same rule.

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
