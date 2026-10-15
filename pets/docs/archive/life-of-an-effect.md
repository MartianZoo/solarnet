# Life of an Effect (archived historical walkthrough)

> This walkthrough predates lexical `Me` ownership. Its `Owner` substitution stages describe the
> previous engine; see the [language specification](../pets-language-spec.md) for current semantics.

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

Stages 1–6 cover declaration loading and specialization. Continue with
[stages 7–11 in Solarnet](../../../docs/archive/life-of-an-effect.md#7-the-component-effect-becomes-a-live-effect)
for live effects, tasks, and execution. This is historical material, not a current API guide.

## 1. The structured card data

**PetTransformers, in order:** none.

The Effect initially appears as one string in Recyclon's `cards.json5` record. Nothing has parsed or
validated it yet. In particular, it has not been checked against any Class Table, and no omitted
Dependency or Quantifier has been filled in.

**Postcondition:** the source resource contains the original Effect text. The program does not yet
have an Effect.

## 2. The string becomes a Source Effect

**PetTransformers, in order:** `DerivedClassLowerer`.

The card generator parses the entire string as an Effect, not as a general Instruction or some
other Pets element. Reading must consume the entire string. The result has three principal parts:

```pets
Trigger:      BuildingTag
Instruction:  Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
kind:         queued, because the separator is `:` rather than `::`
```

The grouping is now settled. The right side is one `OR`. Its second arm is one Sequential
Instruction whose first stage removes two microbes and whose second stage raises plant production.
Parentheses affect that structure but are not retained as a separate element.

This is also when matching marked occurrences are recorded as one Effect-local Type
Variable. A `THEN` nested in the instruction similarly owns any variable it marks across its stages.
That recognition happens before defaults or Production Box lowering.
`This` and `Owner` are contextual bindings, not Type Variables. Recyclon has no Type Variable
linking its Trigger to its Instruction.

Those links belong to the complete authored region. Rendering and reparsing an isolated subtree
creates a new region and cannot reconstruct a variable identity shared with nodes outside that
subtree. Transform the original typed tree, or resubmit the complete authored region through normal
elaboration; do not use a subtree's text as a substitute for its recorded occurrence links.

An Effect can also declare a Class local to its card. Such a declaration would be given a stable
card-owned Class Name here. Recyclon's Effect does not do so, so its visible structure is unchanged.
`DerivedClassLowerer` nevertheless visits the whole parsed Effect; for Recyclon it returns an equal
tree.

**Postcondition:** there is one complete Source Effect with a fixed Trigger, Instruction tree,
queued/automatic choice, and set of authored Type Variables. Its Type Expressions are still source
expressions; Class-level defaults and validity have not yet been established.

## 3. Recyclon's Class Declaration enters the Catalog

**PetTransformers, in order:** none.

The card generator assembles Recyclon's Class Declaration, renders it into generated `cards.pets`,
and the Catalog source pipeline parses that declaration. Its behavior-bearing part is:

```pets
CLASS Recyclon : ResourceCard<Class<Microbe>, Class<CorporationCard>> {
  cost = 0
  HAS =1 MicrobeTag<This>, =1 BuildingTag<This>
  This:: MicrobeTag<This>!, BuildingTag<This>!
  This: 38 MC, PROD[Steel]
  BuildingTag: Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
}
```

The last line is our original Source Effect. The preceding lines are generated from the card's tags,
direct effect, cost, deck, and derived resource role. Tags use the same exact-count invariant
and automatic creation path as fixed card attachments. Generation preserves the authored order of
Effects. The parsed declaration enters the Catalog without a card-mode transformation.

**Postcondition:** the Effect now has a Class Declaration as its Context. The declaration says what
Recyclon contributes, but remains inert: it is not yet a Class and has not inherited
behavior or received Class-level defaults.

## 4. The Class Declaration is loaded

**PetTransformers, in order:** none on this Effect. Class Loading uses
`replaceThisExpressionsWith` while resolving certain Class-owned Type Expressions, but does not run
it over Recyclon's Source Effect.

The Catalog collects Recyclon's authored declaration with the Rule Classes and other Content
Classes. Class Loading turns that mutually referring set into a Class Table. Recyclon thereby
becomes a Class with a resolved hierarchy through `ResourceCard`, `CardFront`, `Card`, and `Owned`.

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

1. `recordTypeVariableScopes`
2. `insertGainRemoveDefaults`
3. `insertExpressionDefaults`
4. `atomizer`
5. `Prod.deprodify`
6. `fixEffectForUnownedContext`, only when the Effect's declaring Class is neither `Owned` nor an
   `Owner`
7. `evaluateProperties`, using the inheriting Class as Context and leaving abstract property values
   for a later stage

The two default passes are the expanded contents of `insertDefaults`. Steps 1–7 form
`attachToClassTransformer` and run separately on each Source Effect with its declaring Class as
Context. After Effects from every superclass have been collected, step 8 runs on all of them with
Recyclon as Context.

Step 7 does nothing to Recyclon or Manutech because both belong to an `Owned` hierarchy. Its
canonical case is the ownerless `GlobalParameter` rule `This: TerraformRating`. Defaults elaborate
the result to `TerraformRating<Owner>!`, but its Trigger has no owner expression. The step produces
`This BY Owner: TerraformRating<Owner>!`, allowing a triggering Player—but not Admin—to supply the
contextual owner. This is an implicit Actor rule from the icon grammar, not Type-variable linkage.

Recyclon's Class Effects consist of the Source Effects contributed by Recyclon and all its
superclasses, transformed for Recyclon as far as possible without choosing a concrete Component.
The original line becomes:

```pets
BuildingTag<Owner>:
  Microbe<This>. OR
  (-2 Microbe<This>! THEN Production<Owner, Class<Plant>>!)
```

Several changes have happened at once:

- The inherited `Owned` default supplies the contextual `Owner`; `Cardbound` constrains the omitted
  card dependency to `CardFront<Owner>`.
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

- a Class-header variable declared as `Foo` becomes the inheriting Class's corresponding Type at the
  Class Effect
  stage;
- `This` stays late-bound through inheritance and becomes the exact context Component Type only at
  the Component Effect stage.

So a Class Effect on `Bar` can already use the Class-header variable declared by `Foo`, while still
saying `This` elsewhere. An unrelated occurrence of the same Class Name remains an ordinary Type.

Recyclon's selected Effect is declared on Recyclon, so it does not visibly exercise this
replacement. Its `This` occurrences do exercise the separate late binding in the next stage.

**Postcondition:** a Class Effect has all inherited Class Context, defaults, Quantifiers, and
Production Box lowering applied. Its expressions can now be interpreted through the game Class
Table. Only facts that require an exact Component—most visibly `This`, `Owner`, and inherited
Dependency choices—remain open.

## 6. A concrete Recyclon Type produces a Component Effect

**PetTransformers, in order:** `PetElaborator.specializeEffect` composes:

1. `replaceOwnerWith`
2. `replaceThisExpressionsWith`
3. binding of the Class-header variables actually used by the Effect
4. `invalidChangesToDie`

The Effect's Types are checked after this chain; that check is not a PetTransformer. On Recyclon,
step 1 replaces contextual `Owner` with `Player1`, step 2 replaces `This` with the exact Recyclon
Type, and step 3 captures the component values for any used Class variables. There is no invalid
branch, so step 4 makes no visible change.

Suppose Player1's Recyclon has the full Type:

```pets
Recyclon<Player1, Class<Microbe>>
```

Its Minimal Form is normally just `Recyclon<Player1>`, because the microbe Class is fixed by the
Recyclon declaration. Specializing the Class Effect for that concrete Type produces:

```pets
BuildingTag<Player1>:
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
