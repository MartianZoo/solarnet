# Life of an Effect

This document follows one Effect in an authored canonical card declaration until its Instructions
produce recorded State Changes. The example is Recyclon because it passes through most of the
interesting stages without making any one of them unusually difficult:

```pets
CLASS Recyclon : ResourceCard<Class<Microbe>, Class<CorporationCard>> {
  cost = 0
  This:: MicrobeTag<This>, BuildingTag<This>
  This: 38 MC, PROD[Steel]
  BuildingTag<>: Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
}
```

At every stage, the important question is not merely “what code ran?” but “what is now safe to
assume about the Pets tree?” Those postconditions are called out explicitly.

The last part takes the point of view of a Task Queue client which does not use Autoexec. The client
plays by selecting Tasks and narrowing the selected Task, then observes the engine's resolution,
execution, and resulting State Changes. It does not need to know how those consequences are carried
out internally.

The `Gameplay` API exposes the Player activities as `selectTask` and `narrowTask`; resolution and
execution remain engine consequences.

`PetTransformer` is the implementation's common mechanism for turning one Pets tree into another.
Each numbered stage below lists the PetTransformers which touch our Effect, in execution order. A
composite transformer such as `insertDefaults` is expanded so that its internal order is visible.
“None” means that the stage may validate, combine, resolve, or record Pets data, but does not run a
PetTransformer over this Effect. These lists describe the current implementation; the
postconditions remain the semantic contract.

## The whole journey at a glance

| Stage | What has become known |
|---|---|
| Pets source | The card declaration and Effect are still text. |
| Source Effect | The whole string has been read as one well-formed Effect. |
| Class Declaration | The Effect belongs to Recyclon's parsed declaration. |
| loaded Class | Recyclon's hierarchy, Dependencies, defaults, and Invariants have Type-system meaning. |
| Class Effect | Inheritance and every Class-level transformation have been applied. |
| Component Effect | `Owner`, `This`, and inherited Dependencies have been bound for one concrete Recyclon Type. |
| Live Effect | A corresponding Recyclon Component actually exists and can respond to Change Events. |
| Triggered Instruction | One exact Change Event has matched and supplied any trigger-linked choices. |
| Task | The queued Instruction has an Assignee and Cause and may still contain choices. |
| Selected Task | The client has chosen the next Task, and current Game World facts have been applied. |
| State Change | An exact gain or removal has happened and a Change Event records it. |

## 1. The Pets source

**PetTransformers, in order:** none.

The Effect initially appears as one line in Recyclon's `cards.pets` declaration. Nothing has parsed
or validated it yet. In particular, it has not been checked against any Class Table, and no omitted
Dependency or Quantifier has been filled in.

**Postcondition:** the source resource contains the original Effect text. The program does not yet
have an Effect.

## 2. The string becomes a Source Effect

**PetTransformers, in order:** `DerivedClassLowerer`.

Class parsing reads the entire line as an Effect, not as a general Instruction or some other Pets
element. Reading must consume the entire string. The result has three principal parts:

```pets
Trigger:      BuildingTag<>
Instruction:  Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
kind:         queued, because the separator is `:` rather than `::`
```

The grouping is now settled. The right side is one `OR`. Its second arm is one Sequential
Instruction whose first stage removes two microbes and whose second stage raises plant production.
Parentheses affect that structure but are not retained as a separate element.

This is also when repeated authored Type Expressions that denote one choice are recorded as Type
Variables. That recognition happens before defaults or Production Box lowering can make unrelated
expressions look alike. `This` and `Owner` are contextual bindings, not Type Variables. Recyclon has
no Type Variable linking its Trigger to its Instruction.

An Effect can also declare a Class local to its card. Such a declaration would be given a stable
card-owned Class Name here. Recyclon's Effect does not do so, so its visible structure is unchanged.
`DerivedClassLowerer` nevertheless visits the whole parsed Effect; for Recyclon it returns an equal
tree.

**Postcondition:** there is one complete Source Effect with a fixed Trigger, Instruction tree,
queued/automatic choice, and set of authored Type Variables. Its Type Expressions are still source
expressions; Class-level defaults and validity have not yet been established.

## 3. Recyclon's Class Declaration enters the Catalog

**PetTransformers, in order:** `FollowModeNeutralizer`.

Parsing `cards.pets` contributes Recyclon's Class Declaration directly. Its behavior-bearing part
is:

```pets
CLASS Recyclon : ResourceCard<Class<Microbe>, Class<CorporationCard>> {
  cost = 0
  This:: MicrobeTag<This>, BuildingTag<This>
  This: 38 MC, PROD[Steel]
  BuildingTag<>: Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
}
```

The last line is our original Source Effect. The preceding lines directly author the card's tags,
immediate instruction, cost, deck, and resource role. Parsing does not silently merge, remove, or
reorder authored Effects. `FollowModeNeutralizer` preserves generic card-location operations while
delegating printed-face constraints to the follow-mode client. Recyclon's Effect contains no such
operation and is unchanged.

**Postcondition:** the Effect now has a Class Declaration as its Context. The declaration says what
Recyclon directly contributes, but remains inert: it is not yet a Class and has not inherited
behavior or received Class-level defaults.

## 4. The Class Declaration is loaded

**PetTransformers, in order:** none on this Effect. Class Loading uses
`replaceThisExpressionsWith` while resolving certain Class-owned Type Expressions, but does not run
it over Recyclon's Source Effect.

The Catalog collects Recyclon's authored declaration with the Rule Classes and other Content
Classes. Class Loading turns that mutually referring set into a Class Table. Recyclon thereby
becomes a Class with a resolved hierarchy through `ResourceCard`, `CardFront`, `TagHolder`, `Card`,
and `Owned`.

The Game Premise then supplies a game-specific Class-table Projection. If Recyclon is an Active
Class there, it can contribute Effects and Recyclon Components can exist. If it is Uninhabited, its
name and hierarchy remain meaningful, but its Effect cannot become live.

Loading does not yet bind one owner or one Recyclon Component. `Owner` and `This` therefore remain
meaningful unresolved Context.

**Postcondition:** every referenced Class has one Catalog-scoped identity; Recyclon's inherited
Classes, Dependencies, defaults, properties, and Invariants form one compatible hierarchy; and the
game knows whether Recyclon is Active. These facts may now be used to form its Class Effects.

## 5. Inheritance and Class-level transformation produce a Class Effect

**PetTransformers, in order:**

1. `insertTriggerDefaults`
2. `insertGainRemoveDefaults`
3. `insertExpressionDefaults`
4. `atomizer`
5. `Prod.deprodify`
6. `fixEffectForUnownedContext`, only when the Effect's declaring Class is neither `Owned` nor an
   `Owner`
7. `evaluateProperties`, using the inheriting Class as Context and leaving abstract property values
   for a later stage

The first three are the expanded contents of `insertDefaults`. Steps 1–6 form
`attachToClassTransformer` and run separately on each Source Effect with its declaring Class as
Context. After Effects from every superclass have been collected, step 7 runs on all of them with
Recyclon as Context. For this Effect, steps 1–3 and 5 cause the visible changes below; steps 4, 6,
and 7 leave it alone.

Recyclon's Class Effects consist of the Source Effects contributed by Recyclon and all its
superclasses, transformed for Recyclon as far as possible without choosing a concrete Component.
The original line becomes:

```pets
BuildingTag<Owner, CardFront<Owner>>:
  Microbe<This>. OR
  (-2 Microbe<This>! THEN Production<Owner, Class<Plant>>!)
```

Several changes have happened at once:

- `BuildingTag<>` accepts the Trigger default declared by `Tag`, so its holder becomes
  `CardFront`. Because both tag and card are `Owned`, their ownership Dependencies are the same
  contextual `Owner`.
- The omitted Quantifier on gaining a Card Resource becomes AMAP (`.`), as declared by the
  `CardResource` default.
- The omitted Quantifier on the removal becomes mandatory (`!`).
- `PROD[Plant]` is a Production Box. It is lowered to the ordinary gain
  `Production<Owner, Class<Plant>>!`.
- `This` deliberately remains. A Class Effect still describes every concrete Recyclon Type, not
  one Recyclon belonging to one Player.

Minimal Form can hide a Dependency which is already forced by another one. Thus
`Microbe<This>` does not print a separate `Owner` even though a microbe on an owned Resource Card
has that card's Owner.

### Inherited self Types and `This`

Inheritance also specializes references tied to a superclass's own Class Header. In the intended
model, if an abstract Class `Foo` uses its header's self Type inside an Effect, then the inherited
Class Effect on `Bar : Foo` uses `Bar` in that position. This is not the same rule as `This`:

```pets
ABSTRACT CLASS Foo { Foo: This }
CLASS Bar : Foo

// Class Effect inherited by Bar
Bar: This

// Component Effect for a Bar Component
Bar: Bar
```

- a header-linked `Foo` becomes the inheriting Class's corresponding Type at the Class Effect
  stage;
- `This` stays late-bound through inheritance and becomes the exact context Component Type only at
  the Component Effect stage.

So a Class Effect on `Bar` can already say `Bar` where the declaration said header-linked `Foo`,
while still saying `This` elsewhere.

*The current recognition and propagation of Class Header Type Variables has known gaps. The rule
above is the intended contract; in particular, an unrelated occurrence of the same Class Name
should not be changed merely because its spelling matches. The current Class Effect chain has no
separate inheritance transformer for that rule; header-linked replacements currently occur in the
Component Effect chain described next.*

Recyclon's selected Effect is declared directly on Recyclon, so it does not visibly exercise this
replacement. Its `This` occurrences do exercise the separate late binding in the next stage.

**Postcondition:** a Class Effect has all inherited Class Context, defaults, Quantifiers, and
Production Box lowering applied. Its expressions can now be interpreted through the game Class
Table. Only facts that require an exact Component—most visibly `This`, `Owner`, and inherited
Dependency choices—remain open.

## 6. A concrete Recyclon Type produces a Component Effect

**PetTransformers, in order:** `checkedEffectSubstituter` composes:

1. `substituter`, for ordinary Class and Dependency specialization
2. zero or more `PetNode.replacer` instances, one for each unambiguous Class Header Dependency
   binding used by the Effect
3. `replaceOwnerWith`
4. `replaceThisExpressionsWith`
5. `insertDeferredComplementDefaults`
6. `invalidChangesToDie`

The Effect's Types are checked after this chain; that check is not a PetTransformer. On Recyclon,
step 2 replaces the header-linked `Owner` with `Player1`, and step 4 replaces `This` with the exact
Recyclon Type. The explicit owner replacement in step 3 then has nothing left to change. There is
no deferred complement or invalid branch, so steps 5 and 6 also make no visible change.

Suppose Player1's Recyclon has the full Type:

```pets
Recyclon<Player1, Class<Microbe>>
```

Its Minimal Form is normally just `Recyclon<Player1>`, because the microbe Class is fixed by the
Recyclon declaration. Specializing the Class Effect for that concrete Type produces:

```pets
BuildingTag<Player1, CardFront<Player1>>:
  Microbe<Recyclon<Player1>>. OR
  (-2 Microbe<Recyclon<Player1>>! THEN
    Production<Player1, Class<Plant>>!)
```

The inherited ownership Dependency has bound `Owner` to `Player1`. Every `This` has become the
exact Recyclon Type, so both branches target this Recyclon rather than an arbitrary Resource Card.
Defaults that could not be settled without those concrete bounds are now inserted, and every Type
in the resulting Effect is checked.

This still does not assert that a Recyclon Component exists. A Component Effect is the behavior
*such a Component Type would have*.

**Postcondition:** the Effect contains no contextual `This` or `Owner`, and every inherited
Dependency is bound consistently with one Concrete Type. It is a valid Component Effect, but not
yet live behavior.

## 7. The Component Effect becomes a Live Effect

**PetTransformers, in order:** none. Forming a subscription and pairing the Effect with an existing
Component add Context without rewriting the Pets tree.

When a `Recyclon<Player1>` Component exists in the Game World, its Component Effects become Live
Effects. This usually does not change the printed Pets tree. It changes its Context: the Effect is
now paired with an existing Component and is ready to respond to matching Change Events.

The number of Components matters. One existing Component contributes one live copy of each Effect;
removing that Component removes its live behavior. Recyclon's Invariant permits at most one
matching copy, but the general rule is not card-specific.

**Postcondition:** there is an existing context Component for this Effect, and the Game World will
test the Live Effect against relevant Change Events for exactly as long as that Component exists.

## 8. A Change Event produces a Triggered Instruction

**PetTransformers, in order:** `checkedLinkageSubstituter` first uses `substituter` to calculate a
possible replacement for each linked source, then builds this effective chain over the Instruction:

1. zero or more `PetNode.replacer` instances for Type Variables linked to the Trigger
2. `replaceOwnerWith`, only if a contextual `Owner` remains in the Trigger
3. `invalidChangesToDie`

The resulting Instruction is then multiplied by the matching State Change's count; multiplication
is not a PetTransformer. Recyclon has no trigger-linked Type Variable and its Component Effect has
already replaced `Owner`, so only step 3 runs over its Instruction and it changes nothing. Manutech
uses step 1 to replace its linked `StandardResource` with `Plant`.

Now suppose Player1 plays Titanium Mine. Its printed building tag produces the exact State Change
that gains a `BuildingTag` dependent on `TitaniumMine<Player1>`. Its Change Event matches:

```pets
BuildingTag<Player1, CardFront<Player1>>
```

The Live Effect therefore produces this Triggered Instruction:

```pets
Microbe<Recyclon<Player1>>. OR
(-2 Microbe<Recyclon<Player1>>! THEN
  Production<Player1, Class<Plant>>!)
```

One matching tag produces one copy of the Instruction. Tags are atomized, so gaining two printed
tags produces two separately observable tag gains rather than one counted tag event.

Recyclon has no Type Variable crossing from Trigger to Instruction, so matching the exact Titanium
Mine tag does not further change its result. Manutech shows the missing case:

```pets
// Source Effect
PROD[StandardResource]: StandardResource

// Class Effect
Production<Owner, Class<StandardResource>>: StandardResource<Owner>!

// Triggered by gaining Production<Player1, Class<Plant>>
Plant<Player1>!
```

The two authored occurrences of `StandardResource` are one Type Variable. The exact Trigger match
narrows it to `Plant`, and that same choice narrows the result. The linkage comes from the authored
structure; it is not guessed later from two expressions that happen to resemble one another.

**Postcondition:** the Trigger is finished. Its exact Change Event has fixed every trigger-linked
Type Variable it can fix, and the result has been repeated by the matched State Change's count. The
result is an Instruction, but it can still be abstract because choices inside the result belong to
the Assignee.

## 9. The queued Effect becomes a Task

**PetTransformers, in order:** creating and adding the Task uses none. If a client supplies Pets
text to choose or narrow it, that input first passes through:

1. `DerivedClassLowerer`, as part of reading the submitted Pets text
2. `rejectPropertyEvaluations`
3. `canonicalize`
4. `useFullNames`
5. `atomizer`
6. `insertTriggerDefaults`, `insertGainRemoveDefaults`, and `insertExpressionDefaults`, together
   exposed as `insertDefaults`
7. `replaceOwnerWith`, when the client is a Player
8. `Prod.deprodify`

First-stage narrowing can then use `PetNode.replacer` for authored Type Variables shared across
`THEN`, followed by `bindXTo` when stages share `X`. When a removal's source form was lowered, the
fallback is `checkedSubstituter`: `substituter` followed by `invalidChangesToDie`. Recyclon's
`THEN` shares neither a Type Variable nor `X`, and its removal needs no binding, so selecting its
first stage changes the Task structure directly rather than through one of these PetTransformers.

Recyclon used `:`, so its Triggered Instruction becomes a Task in Player1's Task Queue. Had it used
`::`, it would have been an Automatic Effect and would not offer this client-visible Task.

The Task retains the whole `OR`, has Player1 as its Assignee, and carries a Cause pointing to the
Recyclon Context and the triggering building-tag Change Event. The Assignee may choose among Tasks
in the queue; their iteration order has no gameplay meaning.

Assume Recyclon already holds at least two microbes. The client chooses this Task and narrows it to
the first stage of the second arm:

```pets
-2 Microbe<Recyclon<Player1>>!
```

That is a valid narrowing of exactly one `OR` arm. Because the chosen arm was a Sequential
Instruction, its remainder is retained for later; this later part is called its continuation:

```pets
Production<Player1, Class<Plant>>!
```

The continuation is not executed or resolved early. `THEN` promises only that the second stage
cannot precede completion of the first; it does not give either stage priority over unrelated
Tasks.

Trade Envoys illustrates why separation can sometimes wait longer:

```pets
Trade<ColonyTile>:
  ColonyProduction<ColonyTile>? THEN -TradeBarrier
```

The repeated `ColonyTile` is a Type Variable shared by the Trigger and first stage. The Sequential
Instruction must retain that link until an exact event such as `Trade<Luna>` narrows the first stage
to `ColonyProduction<Luna>?`. Only then is the first stage safely independent of its continuation.

**Postcondition:** the selected Task is a valid narrowing of the offered Task, and any later `THEN`
stages have been retained with the same Assignee, Cause, and Performer. No later stage can appear
before the selected first stage completes.

## 10. Selecting, resolving, and executing the first stage

**PetTransformers, in order:** none during resolution or execution of Recyclon's ordinary Change
Instruction. Resolution handles its count, Quantifier, Type, and Limits directly. If the client
identifies the Task by Pets text rather than Task ID, that text uses the eight-step client-input
chain listed in stage 9 before resolution begins.

The client selects the removal Task. The engine resolves it against the current Game World. With at
least two microbes on Recyclon, it becomes the Selected Task for exactly:

```pets
-2 Microbe<Recyclon<Player1>>!
```

Resolution has checked the current counts and applicable Invariants and applied every
Game-World-dependent calculation. The Selected Task is locked as the next Task to finish, because
those conclusions were drawn from the current World. Here there is no remaining choice.

The engine therefore executes it, and the client observes:

- the count of `Microbe<Recyclon<Player1>>` has decreased by two;
- a Change Event records that exact removal, its Performer, and the Task's Cause; and
- the plant-production continuation now appears as a Task.

The Change Instruction requested a removal. The State Change and Change Event are the evidence
that it actually happened; those are deliberately different concepts.

**Postcondition:** the exact removal is committed in the Game World and recorded in the Event Log.
The first Task is complete, and only now is its continuation pending.

## 11. Selecting and executing the continuation

**PetTransformers, in order:** none during resolution or execution. As in stage 10, client-supplied
Pets text separately passes through the stage 9 client-input chain.

The client next selects:

```pets
Production<Player1, Class<Plant>>!
```

It is already a mandatory gain of a Concrete Type, so resolution leaves its visible form alone.
The engine executes it, and the client observes the final State Change: one
`Production<Player1, Class<Plant>>` Component has been gained, with a corresponding Change Event.

Nothing in `THEN` makes this gain part of the same State Change as the microbe removal. They are two
separate, observable changes, and other Effects may respond between them.

**Postcondition:** the plant-production Component exists, its gain is recorded, and Recyclon's
Triggered Instruction has no remaining Task or continuation.

## What each stage deliberately does not promise

- A Source Effect is structurally valid Pets, but its Type Expressions have not yet been checked in
  a Class Table.
- A Class Declaration owns an Effect, but does not yet give it inherited behavior.
- A Class Effect is valid for a loaded Class, but can still contain contextual or abstract Types.
- A Component Effect is concrete about Context, but does not prove that a matching Component
  exists.
- A Live Effect can respond, but it has not responded until one Change Event matches.
- A Triggered Instruction has finished trigger specialization, but may still be an Abstract
  Instruction.
- A Task records permitted work and choices; only selection causes current Game World facts to be
  resolved into it.
- A Selected Task is locked next, but its selection and any partial narrowing are Task state rather
  than State Changes.
- Only execution produces State Changes, and only Change Events make those changes part of the
  Event Log.
