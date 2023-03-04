# Syntax

Here's a quick overview of the syntax of the PETS language. The best way to learn is to read the example cards
in `cards.json5`.

## Type expressions

```
typeExpression  := genericTypeExpr | classLiteral 
genericTypeExpr := className [specializations] [refinement]
specializations := '<' typeExpression (',' typeExpression)* '>'
refinement      := '(HAS' requirement ')'
classLiteral    := className '.CLASS'
className       := upperCamelRE
```

Type expressions are the heart of the PETS language. There are two kinds.

### Generic type expression

This can be as simple as `Player1` or as complex as `CityTile<Player2, MarsArea(HAS MAX 0 CityTile<Anyone>)>`. First
comes a class name, then an optional list of one or more specializations inside angle brackets, and finally an optional
requirement. Of course, each listed specialization is an entire type expression itself.

These expressions are a way of identifying a type, and types are explained in the [type system] article.

### Class literal

For any class name `Foo`, you can write the class literal `Class<Foo>`. An instace of `Foo.class` is created upon
initialization of the type system if and only if `Foo` is a concrete class. So, for example, an instance
for `Class<StandardResource>` is not created; however if you `count StandardResource.class` you will get the answer `6`,
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

## Requirements

```
requirement := orReqt (',' orReqt)*
orReqt      := atomReqt ('OR' atomReqt)*
atomReqt    := minReqt | maxReqt | exactReqt | prodReqt | groupedReqt
minReqt     := scalarAndType
maxReqt     := 'MAX' scalarAndType
exactReqt   := '=' scalarAndType
prodReqt    := 'PROD[' requirement ']'
groupedReqt := '(' requirement ')'
```

A requirement expresses a condition that can be checked against a game state to determine a `true` or `false` value. Of
course, these are familiar from cards; many control whether the card can be played (`MAX 4 OxygenStep`), and in a few
cases gate an instruction on the card (like in Nitro-Rich Asteroid, `PROD[Plant OR 3 PlantTag: 4 Plant]`).

The requirement `a, b, c` will be true if all three of given requirements are true. The comma is the lowest-precedence
operator.

The requirement `a OR b OR c` will be true if any one of the three given requirements is true. The `OR` operator has
higher precedence than the comma, so `a, b OR c` means that `a` must be true, and one of `b` or `c` must be true.

### Instructions

```
instruction := thenInst (',' thenInst)*
thenInst    := orInst ('THEN' orInst)*
orInst      := gatedInst ('OR' gatedInst)*
gatedInst   := [atomReqt ':'] (groupedInst | prodInst | customInst)
prodInst    := perInst | ('PROD[' instruction ']')
customInst  := '@' lowerCamelRE '(' [arguments] ')'
arguments   := typeExpression (',' typeExpression)*
perInst     := perableInst ['/' scalarAndType]
perableInst := gainInst | removeInst | fromInst | ('(' fromInst ')')
gainInst    := scalarAndType [intensity]
removeInst  := '-' scalarAndType [intensity]
fromInst    := [scalar] from [intensity]
from        := simpleFrom | complexFrom
simpleFrom  := genericTypeExpr 'FROM' genericTypeExpr
complexFrom := className '<' fromArgs '>' [refinement]
fromArgs    := (typeExpression ',')* from (',' typeExpression)*
groupedInst := '(' instruction ')'
```

Instructions are the meat of the language, as you can see. The elementary instructions are to gain some amount of a
component (`4 Plant<Player2>`), remove some amount of a component (`-8 Heat<Player1>`), or even transmute some amount of
one component directly into another (`3 Megacredit<Player4 FROM Player2>`).

Commas separate multiple independent instructions. The comma has the lowest precedence of all instruction operators.
Within each comma-separated section, you might find instructions separated by `THEN`; this is similar to the comma, but
the player can't choose the order the tasks will be carried out (even once we support choosing that order!). Next, `OR`
separates instructions which the player can choose between.

Continuing in precedence order: an instruction can be preceded by a requirement and a colon (':'), as seen within the
parentheses in the example `PROD[Plant OR (3 PlantTag: 4 Plant)]`. Note that because `4 Plant` is a mandatory
instruction, `3 PlantTag: 4 Plant` is as well; if there was not another option separated by `OR` then this entire
instruction would be unexecutable by a player with only 2 plant tags.

As with other PETS elements, any part can be surrounded by a `PROD[...]` block. Inside that block, only types
extending `StandardResource` are affected; for example, `Heat<Player2>` is transformed
into `Production<Player2, Class<Heat>>`. This transformation is done because it makes cards easier to write, easier to
render in the official one-prod-box-only style, and because one single card (Robotic Workforce) needs production blocks
to be discretely identifiable.

Custom instructions are supported because some cards would be extremely difficult to represent without them. For
example, Robotic Workforce includes the instruction `@copyProductionBox(CardFront(HAS BuildingTag))`. The player will
have to choose a concrete type that satisfies `CardFront(HAS BuildingTag)`, such as the card Mine or Manutech.

Certain instructions can be followed by a slash ('/') and a quantified expression, such as
in `PROD[Titanium / 3 EarthTag]`. When this instruction is executed the player's EarthTags will be counted, and for
every full three they will get a titanium production.

## Actions

```
action      := [cost] '->' instruction
cost        := orCost (',' orCost)*
orCost      := atomCost ('OR' atomCost)*
atomCost    := perCost | groupedCost
perCost     := prodCost ['/' scalarAndType]
prodCost    := spendCost | ('PROD[' cost ']')
spendCost   := scalarAndType
groupedCost := '(' cost ')'
```

An actions have an optional cost followed by an "arrow" and then an instruction. Costs resemble instructions, but are
assumed negative without need for a minus sign.

The engine actually translates these into triggered effects (if the second action on `ElectroCatapult` is `Plant -> 7`,
that gets translated to `UseAction2<ElectroCatapult>: -Plant THEN 7`).

### Effects

```
effect      := trigger (':' | '::') instruction
trigger     := prodTrigger | atomTrigger
prodTrigger := 'PROD[' atomTrigger ']'
atomTrigger := onGain | onRemove
onGain      := genericTypeExpr
onRemove    := '-' genericTypeExpr
```

An effect consists of a trigger, either one or two colons, then an instruction. The trigger is essentially just a type
optionally preceded by a minus sign. For each instance of that type that is gained (or, with minus sign, removed), the
instruction will be carried out.

TODO: refresh this whole page
