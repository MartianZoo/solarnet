# Make types types again

> **Status:** exploratory proposal requested by the user; implementation is not authorized by this
> report. No compatibility requirements. Source inspected on 2026-10-16; no implementation trial or
> gameplay verification has been performed for this proposal.
>
> **Read when:** considering removal of `HAS` from types, or planning the corresponding changes to
> expression interpretation, queries, and task selection. This report owns the proposal, not the
> current language contract.

## The proposed distinction

A type should have a meaning independent of live game state. Keep `Foo(HAS Bar)` as an Expression,
but stop treating it as a Type. Apply that restriction recursively: a type cannot contain a
state-dependent expression as a dependency bound either.

For example:

- `LandArea` describes a type regardless of the board.
- `LandArea(HAS MAX 0 Tile)` selects land areas that currently have no tile.
- `Tile<LandArea(HAS MAX 0 Tile)>` describes a placement choice whose area must currently be empty.
  Its structural type information is `Tile<LandArea>`; the complete expression retains the condition.
- A placed tile has a concrete type such as `CityTile<Player1, Tharsis_2_2>`.
  Filling that area changes the answer to the selection expression, not the placed tile's type.

Types are already immutable objects. The proposed improvement concerns their meaning and the
operations permitted on them: subtype checks should not need a world, and a type's extension
should not depend on current component counts. Queries naturally remain state-dependent.

`Player(NOT Player1)` needs a separate decision. Its exclusion is structural and state-independent,
so the motivating principle does not require removing it from types. Do not silently expand this
proposal into removal of every refinement. A combined expression such as
`CardFront(HAS BuildingTag, NOT First@CardFront)` must still work whichever home is chosen for `NOT`.

## Why this could be worth doing

The principal value is a simpler account of what the language means. A caller asking whether a
concrete area is a subtype of `LandArea` asks a permanent structural question. A caller asking
whether that area is empty asks about the board. The API should make those distinct questions clear.

Today the [type specification](../../pets/docs/type-system-spec.md), especially T6-1, T6-4, and T8-8,
explicitly permits state-dependent narrowing and a subtype check that fails when it needs a world.
Contextual narrowing is not even transitive: an abstract area domain can satisfy an aggregate
marker query although a particular member does not. That behavior can be meaningful as an
expression query, but it makes type reasoning surprisingly difficult.

The change could remove world-dependent narrowing, candidate substitution, and predicate retention
from the type implementation. It should also make it harder for callers to accidentally use an
aggregate query as proof that every possible choice is legal. CEO's Favorite Project is a concrete
example: finding some card resource does not establish that a subsequently selected empty card is
an eligible holder.

These are expected design benefits, not measured code or performance savings. Predicate evaluation
is still necessary. Moving it into another large representation without simplifying consumers
would not establish the proposal's value.

## Current implementation and likely reach

The current [Type](../../pets/src/common/dev/martianzoo/pets/types/Type.kt) and
[GroundType](../../pets/src/common/dev/martianzoo/pets/types/GroundType.kt) explicitly include
refinements. `GroundType` combines predicates in `glbIn`, binds candidates in `formRequirement`,
consults `TypeInfo.has` during narrowing, and preserves unfinished choice conditions in
`ensureSelectionNarrows`. Refinements also participate in type identity and expression rendering.

[ClassTable](../../pets/src/common/dev/martianzoo/pets/types/ClassTable.kt) exposes expression
resolution as a ground type and handles refinements during enumeration and unique-candidate
resolution. [GameReaderImpl](../../src/common/dev/martianzoo/state/GameReaderImpl.kt) resolves query
expressions into types before counting or matching. [ActorEngine](../../src/common/dev/martianzoo/engine/ActorEngine.kt)
and [Limiter](../../src/common/dev/martianzoo/engine/Limiter.kt) depend on these facilities for
selection and automatic choices. This is a cross-cutting change through Pets and Solarnet.

There is useful existing separation: component types are concrete and refinement-free, class
signatures reject refinements recursively, and the
[Expression AST](../../pets/src/common/dev/martianzoo/pets/ast/Expression.kt) already carries the
syntax we want to retain. The stored component model should therefore need much less change than
query and choice interpretation. Pets now builds separately under `pets/`; verification must cover
both that build and its Solarnet consumers.

## The difficult cases

**Nested conditions and argument assignment.** For `Tile<LandArea(HAS MAX 0 Tile)>`, interpretation
must associate the predicate with the selected area, including after compact arguments are assigned
to dependency keys and defaults are elaborated. Stripping predicates and attaching an outer filter
would lose that association. Preserve the complete expression while using structural types to
resolve its dependencies; investigate whether existing elaboration provides enough information.

**Partially selected tasks.** Selecting `Tile<LandArea>` must not erase an empty-area condition.
Selecting a concrete area can discharge it by checking that area's state. Expression narrowing
must retain conditions on unfinished choices, including choices nested in dependencies. This
obligation survives even though it no longer belongs in `GroundType`.

**Candidate binding.** `Player(HAS StartToken)` tests each candidate player's token. Class-literal
queries such as `Class<@Tag>(HAS @Tag<Player1>)` substitute the represented class instead. Property
receivers, ranks, explicit arguments, and lexical variables also participate. Preserve the existing
meaning needed by Canon without keeping every incidental type-level formulation.

**Conditions across time.** Queries and effect triggers must keep observing the states prescribed
by their existing semantics. Moving a predicate must not evaluate it prematurely or cache its
answer as a permanent type fact. This proposal does not select new timing rules.

**Combination and rendering.** Combining placement defaults and written constraints currently
uses type intersections that retain predicates. The replacement must preserve those expressions,
including nested `HAS` and mixed `HAS`/`NOT`, through specialization and display. English consumers
must adapt if they currently assume every expression resolves to a type; card syntax need not change.

## A promising implementation sequence

1. Specify the distinction and acceptance examples first. Decide the treatment of `NOT`, what
   resolving a filtered expression means, and which expression comparisons remain supported.
   Recommend that an API promising a Type reject `HAS` rather than silently discard it. Any operation
   extracting just structural information must make that loss explicit.
2. Try a small end-to-end experiment with nested placement and partial task selection together.
   Use the existing Expression representation and existing dependency interpretation. Establish
   structural compatibility first, then evaluate or retain predicates at their expression positions.
   Do not begin by inventing a parallel tree of resolved filtered types.
3. If that experiment stays coherent, migrate expression queries, counts, effect matching, and
   choice resolution to the same expression semantics. Change APIs accepting Types when their real
   input is a state-dependent selection. Keep structural subtype and enumeration operations on Types.
4. Remove `HAS` storage and evaluation from types, recursively. Remove obsolete APIs and temporary
   migration paths; no compatibility layer is required. Separate expression predicate combination
   from structural type intersection. Keep world-query callbacks where expression semantics need
   them without making the static Pets build depend on the Solarnet engine.
5. Reconcile the type and language specifications, KDoc, tests, renderers, and other consumers.
   Review the entire result for actual deletion and reduced conceptual cost before declaring success.

This is a sequence for investigation, not a settled API design. If correct nested matching requires
a substantial new representation or processing stage, stop and explain that cost before proceeding.

## Evidence and acceptance

Existing starting points include [Spec08RefinementsTest](../../pets/test/common/dev/martianzoo/pets/types/Spec08RefinementsTest.kt),
[Lang03NarrowingTest](../../pets/test/common/dev/martianzoo/pets/Lang03NarrowingTest.kt),
[RefinementQueryTest](../../test/common/dev/martianzoo/engine/RefinementQueryTest.kt), and
[TaskNarrowingTest](../../test/common/dev/martianzoo/engine/TaskNarrowingTest.kt). Type-level tests that
assert filtered values are Types must change; their existing expectations cannot veto the proposal.

Acceptance should include:

- Type construction, equality, subtype checks, and structural intersections require no live state;
  no Type carries `HAS`, including inside dependencies.
- Nested placement constraints survive defaults, specialization, partial selection, and rendering.
- Candidate-specific queries, represented-class binding, and trigger timing retain their meaning.
- Concrete Canon scenarios remain supported: Research Outpost's adjacency restriction, CEO's
  Favorite Project's eligible resource holder, Robotic Workforce's building-card selection, and
  Diversifier's counting of distinct tag classes.
- Meaningful expression tests and gameplay scenarios pass across both builds, followed by the
  applicable broader checks. Synthetic tests characterize mechanics; unsupported synthetic
  combinations are not by themselves reasons to retain machinery.
- The final design removes the old state-dependent type model rather than preserving it under a
  different name.

## Difficulty and recommendation

Initial estimate: several focused days of implementation and verification, with substantial
uncertainty until the nested-placement experiment is attempted. This is not a scheduled commitment
or a measured estimate. The broad cost comes from changing the assumption that every expression
resolves to a Type, not from parsing `HAS` or changing stored components.

No compatibility requirement makes this an attractive time to investigate. The recommendation is
to test whether the separation reduces permanent concepts while preserving Canon behavior. The
implementation experiment and the decisions above remain deferred; this report does not change
the current specifications or select the proposal for implementation.
