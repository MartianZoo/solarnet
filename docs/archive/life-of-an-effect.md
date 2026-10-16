# Life of an Effect (archived historical walkthrough)

> This walkthrough predates lexical `Me` ownership. Its `Owner` substitution stages describe the
> previous engine; see the [language specification](../../pets/docs/pets-language-spec.md) for current semantics.

This document follows one Effect in an authored canonical card declaration until its Instructions
produce recorded State Changes. The example is Recyclon because it passes through most of the
interesting stages without making any one of them unusually difficult:

```pets
CLASS Recyclon : ResourceCard<Class<Microbe>, Class<CorporationCard>> {
  cost = 0
  HAS =1 MicrobeTag<This>, =1 BuildingTag<This>
  This:: MicrobeTag<This>!, BuildingTag<This>!
  This: 38 MC, PROD[Steel]
  BuildingTag: Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
}
```

At every stage, the important question is not merely “what code ran?” but “what is now safe to
assume about the Pets tree?” Those postconditions are called out explicitly.

The last part takes the point of view of a client using one Actor's filtered Task view without
Autoexec. The client plays by selecting Tasks and narrowing the selected Task, then observes the
engine's resolution, execution, and resulting State Changes. It does not need to know how those
consequences are carried out internally.

The Agent API exposes Player activities such as `selectTask` and `narrowTask`. Every ordinary
explicit or policy-chosen mutation passes through that Actor's Agent, which delegates to the
World's stable policy-free `ActorEngine`. Resolution and execution remain engine consequences.

`PetTransformer` is the implementation's common mechanism for turning one Pets tree into another.
Each numbered stage below lists the PetTransformers which touch our Effect, in execution order. A
composite transformer such as `insertDefaults` is expanded so that its internal order is visible.
“None” means that the stage may validate, combine, resolve, or record Pets data, but does not run a
PetTransformer over this Effect. These lists describe the current implementation; the
postconditions remain the semantic contract.

## The whole journey at a glance

| Stage | What has become known |
|---|---|
| Card data | The card fields and Effect are still text. |
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

Declaration stages 1–6 are now in the [Pets half of this archived walkthrough](../../pets/docs/archive/life-of-an-effect.md).

## 7. The Component Effect becomes a Live Effect

**PetTransformers, in order:** none. Forming a subscription and pairing the Effect with an existing
Component add Context without rewriting the Pets tree.

When a `Recyclon<Player1>` Component exists in the Game World, its Component Effects become Live
Effects. This usually does not change the printed Pets tree. It changes its Context: the Effect is
now paired with an existing Component and is ready to respond to matching Change Events.

The number of Components matters. One existing Component contributes one live copy of each Effect;
removing that Component removes its live behavior. Recyclon's Invariant permits at most one
matching copy at operation completion, but live Effects track intermediate counts as well.

**Postcondition:** there is an existing context Component for this Effect, and the Game World will
test the Live Effect against relevant Change Events for exactly as long as that Component exists.

## 8. A Change Event produces a Triggered Instruction

**PetTransformers, in order:** `PetElaborator.specializeVariables` first binds each Trigger variable from
the matching Change Event, then builds this effective chain over the Instruction:

1. binding of the Type Variable occurrences shared with the Trigger
2. `replaceOwnerWith`, only if a contextual `Owner` remains in the Trigger
3. `invalidChangesToDie`

The resulting Instruction is then multiplied by the matching State Change's count; multiplication
is not a PetTransformer. Recyclon has no Trigger-shared Type Variable and its Component Effect has
already replaced `Owner`, so only step 3 runs over its Instruction and it changes nothing. A
Manutech Component Effect has likewise already replaced contextual `Owner` with its card owner's
Player Type; step 1 then replaces its anonymous `@StandardResource` Type Variable with `Plant`.

Now suppose Player1 plays Titanium Mine. Its printed building tag produces the exact State Change
that gains a `BuildingTag` dependent on `TitaniumMine<Player1>`. Its Change Event matches:

```pets
BuildingTag<Player1>
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
Mine tag does not further change its result. Manutech shows one authored variable surviving default
expansion and component contextualization:

```pets
// Source Effect
PROD[@StandardResource]: @StandardResource

// Class Effect
Production<Owner, Class<@StandardResource>>: @StandardResource!

// Component Effect on Manutech<Player1>
Production<Player1, Class<@StandardResource>>: @StandardResource!

// Triggered by gaining Production<Player1, Class<Plant>>
Plant<Player1>!
```

The matching `@StandardResource` markers identify one Type Variable. The exact Trigger match
narrows it to `Plant`, and that same choice narrows the result. Default expansion changes the
recorded occurrence spellings without declaring another variable from the inserted `Owner`.
Component specialization independently replaces that contextual placeholder with `Player1`.

**Postcondition:** the Trigger is finished. Its exact Change Event has bound every Trigger-declared
Type Variable it can bind, and the result has been repeated by the matched State Change's count. The
result is an Instruction, but it can still be abstract because choices inside the result belong to
the Assignee.

## 9. The queued Effect becomes a Task

**PetTransformers, in order:** creating and adding the Task uses none. If a client supplies Pets
text to choose or narrow it, that input first passes through this pipeline:

1. `DerivedClassLowerer`, as part of reading the submitted Pets text
2. `rejectPropertyEvaluations`
3. `canonicalize`
4. `useFullNames`
5. `recordTypeVariableScopes`
6. `atomizer`
7. `insertGainRemoveDefaults` and `insertExpressionDefaults`, together exposed as `insertDefaults`
8. `replaceOwnerWith`, when the client is a Player
9. `Prod.deprodify`

First-stage narrowing binds authored Type Variables shared across `THEN` through their recorded
`TypeVariableScope`, followed by `bindXTo` when stages share `X`. When marked syntax has lowered a
removal's source form, the same scope captures the selected Ground Type from the lowered form before
applying `invalidChangesToDie`. Recyclon's `THEN` shares neither a Type Variable nor `X`, and its
removal needs no binding, so selecting its first stage changes the Task structure.

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
Trade<@ColonyTile>:
  ColonyProduction<@ColonyTile>? THEN -TradeBarrier<@ColonyTile>
```

The anonymous `@ColonyTile` marker identifies a Type Variable shared by the Trigger and first stage. The
Sequential Instruction must retain that link until an exact event such as `Trade<Luna>` narrows the
first stage to `ColonyProduction<Luna>?`. Only then is the first stage safely independent of its
continuation.

**Postcondition:** the selected Task is a valid narrowing of the offered Task, and any later `THEN`
stages have been retained with the same Assignee, Cause, and Performer. No later stage can appear
before the selected first stage completes.

## 10. Selecting, resolving, and executing the first stage

**PetTransformers, in order:** none during resolution or execution of Recyclon's ordinary Change
Instruction. Resolution handles its count, Quantifier, Type, and Limits. If the client
identifies the Task by Pets text rather than Task ID, that text uses the client-input pipeline
listed in stage 9 before resolution begins.

The client selects the removal Task. The engine resolves it against the current Game World. With at
least two microbes on Recyclon, it becomes the Selected Task for exactly:

```pets
-2 Microbe<Recyclon<Player1>>!
```

Resolution has checked that the source microbes exist and applied every Game-World-dependent
calculation. The Selected Task is locked as the next Task to finish, because those conclusions were
drawn from the current World. Here there is no remaining choice.

The engine executes the removal and all its recursive automatic consequences, then validates the
applicable count Invariants to complete the operation. A failed check rolls back the transaction.
Here the operation succeeds, and the client observes:

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
separate, observable changes, and other Effects may respond between them. Each selected Task here
initiates its own operation, including its recursive automatic consequences, and must satisfy count
Invariants at that operation's completion. The queued production gain cannot repair an invariant
violation left by the removal.

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
