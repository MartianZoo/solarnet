# Syntax

Here's a quick overview of the syntax of the PETS language. The best way to learn is to read the example cards
in `cards.json5`.

## Type expressions

```
typeExpression    := dependentTypeExpr | classLiteral
dependentTypeExpr := className [dependencyBounds] [hasRefinement]
dependencyBounds  := '<' [dependencyBound (',' dependencyBound)*] '>'
dependencyBound   := ['!'] typeExpression
hasRefinement     := '(' ('HAS' | 'HAS?') requirement ')'
classLiteral      := 'Class' '<' className '>' [hasRefinement]
className         := upperCamelRE
```

Type expressions are the heart of the PETS language. There are two kinds.

Class Declarations contain only their Class Name; Class Names use ASCII UpperCamelCase, with digits and underscores allowed after the first letter. A session Vocabulary can accept localized Pets Names and separately configured input-only Class Synonyms, while engine state and rules use Class Names.

### Dependency-bearing type expression

This can be as simple as `Player1` or as complex as `CityTile<Player2, MarsArea(HAS MAX 0 CityTile<Anyone>)>`. First
comes a class name, then an optional list of one or more dependency bounds inside angle brackets, and finally an optional
requirement. Of course, each listed bound is an entire type expression itself.

These expressions are a way of identifying a type, and types are explained in the [type system](type-system.md) article.

Empty angle brackets explicitly accept any dependency bounds defaulted for that gain, removal, or trigger.
For example, if `GreeneryTile` has a gain dependency default, a gain must say either
`GreeneryTile<>` or provide at least one dependency argument. Likewise, if `Tag` has the trigger
default `Tag<CardFront>:`, a trigger must say `ScienceTag<>:` or provide at least one dependency
argument. Gain, removal, and trigger defaults are independent, including gain and removal on the
two sides of `FROM`.

A leading `!` can be used inside a dependency bound to mean "anything within this dependency's bound except the named type". For example, `OwnedTile<!Player1>` matches owned tiles whose owner is not Player1. Complement type expressions are dependency constraints and have no standalone type.

### Class literal

For any loaded class name `Foo`, you can write the class literal `Class<Foo>`. An instance of `Class<Foo>` is created upon
initialization of the type system if and only if `Foo` is a concrete class. So, for example, an instance
for `Class<StandardResource>` is not created; however if you `count Class<StandardResource>` you will get the answer `6`,
because it is counting all the subtypes. That is, class literals have the same subtype relationships as their
corresponding classes do.

### Quantified expressions

```
scalarAndType         := scalarAndOptionalType | optionalScalarAndType
scalarAndOptionalType := scalar [typeExpression]
optionalScalarAndType := [scalar] typeExpression
```

A quantified expression is just a number and a type expression. If the number is missing, it's inferred to be `1`. If
the type is missing, it defaults to `Megacredit`. At least one must be used.

### Metrics

```
metric           := subtractionMetric ('OR' subtractionMetric)*
subtractionMetric := maxMetric ('-' subtractionOperand)*
subtractionOperand := maxMetric | scalar
maxMetric        := scaledMetric ['MAX' scalar]
scaledMetric     := [scalar] metricAtom
metricAtom       := typeExpression | transform | '(' metric ')'
transform        := allCapsWord '[' metric ']'
```

A metric computes a non-negative integer. A type expression counts matching components; a scalar preceding another
metric counts complete groups of that size. `MAX` caps the value to its left. `-` subtracts the Metric or positive
scalar to its right but stops at zero, so a Metric never becomes negative. Subtraction is left-associative and binds less
tightly than complete-group scaling or `MAX`. A scalar by itself is not a Metric and may appear only as a subtrahend.

`OR` counts the union of its alternatives, without double-counting a component that matches more than one. Its arms must
be plain component counts: subtraction discards the component identity that union needs. `OR` binds less tightly than
subtraction. Thus `A MAX 5 - B` caps `A` before subtracting `B`, while `(A - B) MAX 5` caps the difference.

Where a Metric is nested, its container determines how much grouping is needed. A counting Requirement accepts one
`metricAtom`, so subtraction and unions must be grouped: `9 (Plant - Steel)`. After `/` in an Instruction or Action cost,
subtraction can be written directly (`Heat / Plant MAX 5 - Steel`), but a Metric union must be grouped because bare `OR`
belongs to the surrounding Instruction or cost (`Heat / (Plant OR Steel)`).

## Requirements

```
requirement := orReqt (',' orReqt)*
orReqt      := atomReqt ('OR' atomReqt)*
atomReqt    := minReqt | maxReqt | exactReqt | prodReqt | groupedReqt
countedReqt := scalarAndType | scalar metricAtom
minReqt     := countedReqt
maxReqt     := 'MAX' countedReqt
exactReqt   := '=' countedReqt
prodReqt    := 'PROD[' requirement ']'
groupedReqt := '(' requirement ')'
```

A Requirement expresses a condition that can be checked against a Game World to determine a `true` or `false` value. Of
course, these are familiar from cards; many control whether the card can be played (`MAX 4 OxygenStep`), and in a few
cases gate an instruction on the card (like in Nitro-Rich Asteroid, `PROD[Plant OR (3 PlantTag: 4 Plant)]`).

The requirement `a, b, c` will be true if all three of given requirements are true. The comma is the lowest-precedence
operator.

The requirement `a OR b OR c` will be true if any one of the three given requirements is true. The `OR` operator has
higher precedence than the comma, so `a, b OR c` means that `a` must be true, and one of `b` or `c` must be true.

### Instructions

```
instruction  := thenInst (',' thenInst)*
thenInst     := gatedInst ('THEN' gatedInst)*
gatedInst    := [atomReqt ':'] orInst
orInst       := atomInst ('OR' atomInst)*
atomInst     := baseAtomInst ['BY' typeExpression]
baseAtomInst := groupedInst | prodInst | customInst
prodInst     := perInst | ('PROD[' instruction ']')
customInst   := '@' lowerCamelRE '(' [arguments] ')'
arguments    := typeExpression (',' typeExpression)*
perInst      := perableInst ['/' subtractionMetric]
perableInst  := gainInst | removeInst | fromInst | ('(' fromInst ')')
gainInst     := scalarAndType [quantifier]
removeInst   := '-' scalarAndType [quantifier]
fromInst     := [scalar] dependentTypeExpr 'FROM' dependentTypeExpr [quantifier]
groupedInst  := '(' instruction ')'
```

Instructions are the meat of the language, as you can see. The elementary instructions are to gain some amount of a
component (`4 Plant<Player2>`), remove some amount of a component (`-8 Heat<Player1>`), or even transmute some amount of
one component directly into another (`3 Megacredit<Player4> FROM Megacredit<Player2>`).

Commas separate multiple independent instructions. The comma has the lowest precedence of all instruction operators.
Within each comma-separated section, you might find instructions separated by `THEN`; this is similar to the comma, but
the player can't choose the order the tasks will be carried out (even once we support choosing that order!).

An instruction can then be preceded by a requirement and a colon (`:`). `OR`, which separates instructions the player
can choose between, binds more tightly than the gate: `3 PlantTag: Plant OR 4 Plant` gates both alternatives. To gate
only one alternative, surround it with parentheses, as in `PROD[Plant OR (3 PlantTag: 4 Plant)]`. Note that because `4 Plant` is a mandatory
instruction, `3 PlantTag: 4 Plant` is as well; if there was not another option separated by `OR` then this entire
instruction would be unexecutable by a player with only 2 plant tags.

As with other PETS elements, any part can be surrounded by a `PROD[...]` block. Inside that block, only types
extending `StandardResource` are affected; for example, `Heat<Player2>` is transformed
into `Production<Player2, Class<Heat>>`. This transformation is done because it makes cards easier to write, easier to
render in the official one-prod-box-only style, and because one single card (Robotic Workforce) needs production boxes
to be discretely identifiable.

Custom instructions are supported because some cards would be extremely difficult to represent without them. For
example, Robotic Workforce includes the instruction `@copyProductionBox(CardFront(HAS BuildingTag))`. The player will
have to choose a concrete type that satisfies `CardFront(HAS BuildingTag)`, such as the card Mine or Manutech.

A gain, removal, or transmutation instruction can be followed by a slash (`/`) and a Metric subtraction expression. Its
quantity is multiplied by the Metric value; for example, `PROD[Titanium / 3 EarthTag]` grants one titanium production
for every three complete EarthTags.

## Actions

```
action      := [cost] '->' instruction
cost        := orCost (',' orCost)*
orCost      := atomCost ('OR' atomCost)*
atomCost    := perCost | groupedCost
perCost     := prodCost ['/' subtractionMetric]
prodCost    := spendCost | ('PROD[' cost ']')
spendCost   := scalarAndType
groupedCost := '(' cost ')'
```

An actions have an optional cost followed by an "arrow" and then an instruction. Costs resemble instructions, but are
assumed negative without need for a minus sign.

The engine actually translates these into triggered effects (if the second action on `ElectroCatapult` is `Plant -> 7`,
that gets translated to `UseAction<ElectroCatapult, Second>: -Plant THEN 7`).

### Effects

```
effect         := trigger (':' | '::') instruction
trigger        := ifTrigger
ifTrigger      := byTrigger ['IF' requirement]
byTrigger      := orTrigger ['BY' typeExpression]
orTrigger      := triggerPrimary ('OR' triggerPrimary)*
triggerPrimary := rawTrigger | '(' trigger ')'
rawTrigger     := prodTrigger | atomTrigger
prodTrigger    := 'PROD[' atomTrigger ']'
atomTrigger    := onGain | onRemove
onGain         := dependentTypeExpr
onRemove       := '-' dependentTypeExpr
```

An effect consists of a trigger, either one or two colons, then an instruction. The trigger is essentially just a type
optionally preceded by a minus sign. For each instance of that type that is gained (or, with minus sign, removed), the
instruction will be carried out.

Triggers can be joined with `OR` and restricted with `BY` or `IF`.

## TODO

* Document complement bounds, `HAS?`, and refinements on class literals.
