# Linkages

## Goal

Make a linkage a first-class Pets concept: an authored equality constraint whose type or
scalar occurrences narrow together. Preserve independent choices, make coupled choices
visible, and let task splitting operate from explicit linkage state rather than syntax
heuristics.

Treat `This` as a distinct, explicitly named contextual binding on the same underlying
machinery, not as a repeated-expression linkage.

The proposed language rules are in
[the concise type-system specification](../docs/type-system-spec.md#10-linkages). This plan
does not call for an immediate runtime rewrite.

## Findings

The intended concept already appears in several implementations that do not share a model:

- class loading groups repeated simple bounds for the same dependency key;
- class loading separately records dependency paths containing `This` so that subclassing
  does not turn a late-bound placeholder into a fixed class literal;
- component-effect and linked trigger narrowing substitute by class name, with an agreement
  check when several occurrences are found;
- component effects replace `This` from their exact concrete component, while the special
  self triggers `This:` and `-This:` use changed-count rather than active-effect multiplicity;
- `THEN` keeps every `X` together and validates scalar values by traversal-order pairing;
- task creation splits an ordinary `THEN`, which loses repeated type choices that have not
  yet been narrowed.

These mechanisms explain the current successes, but none records why two occurrences are
the same choice. Matching after type resolution would be too broad. Defaults already make
`Tile` equal to `Tile<Area>`, for example, while those spellings need not express the same
choice. Parser-inserted bare-resource notation creates similar false candidates.

The proposed direction is therefore source-based recognition: compare parsed authored
trees after expanding aliases and assigning explicitly written bounds to their dependency
keys, but before defaults and preprocessing. Preserve omission and the
authored refinement/complement tree. Prefer maximal repeated expressions, so a linked
composite does not also manufacture linkages for all of its descendants.

## Canon audit

The audit covered every class declaration loaded by `Canon`, working card and engine tests,
dormant card source, `TODO.md`, and the relevant GitHub issues.

| Result under the proposed rules | Canon cases |
| --- | --- |
| Preserved | `Cardbound`, `Trade`, and `Cathedral` class-signature links; dependency-to-effect narrowing including `Production`, `PlayCard`, `PlayedEvent`, `PaymentMechanic`, `Colony`, and `DelayedColonyTile`; linked trigger narrowing including Manutech, Viral Enhancers, colony trading, Splice, Trade Envoys, and Trading Colony; every working-canon `X` linkage |
| Newly recognized in loaded canon | Kaguya Tech's nested `LandArea`; the use-card action's `ActionCard`; behavior-neutral repetitions on Mining Rights and Sponsored Academies |
| Newly recognized in dormant source | Flooding's `Anyone` and Utopia Invest's `StandardResource` |
| Contextual rather than repetition-created | late-bound `Class<This>` in `CardResource`; self-bound invariants such as `HAS MAX 1 This`; the many card-resource effects such as `Microbe<This>` and `Floater<This>`; `This:` and `-This:` self triggers |
| Intentionally not recognized | the separately declared dependencies of `Adjacency<Tile, Tile>` and `Neighbor<Tile<MarsArea>, MarsArea>`; sibling dependency branches such as `Pair<Class<Component>, Class<Component>>`; the two complete operands of Market Manipulation's `ColonyProduction FROM ColonyProduction`; comma siblings; `OR` siblings; repetitions created only by defaults or preprocessing |
| Needs clearer source | Each solo setup pair intends the placed city, not the greenery's area, to be reused. Spell the later reference as `CityTile<LandArea>` so the maximal repeated expression says exactly that; the current bare `CityTile` should not become linked merely because its default is `LandArea` |

The working scalar cases are Carbon Nanosystems (two effects), Dirigibles, Energy Market,
Martian Lumber Corp, Power Infrastructure, Psychrophiles, Sell Patents, Sulphur-Eating
Bacteria, and Titan Shuttles: ten effect/action occurrences across nine definitions.

`BugsTest` currently characterizes the incorrect Kaguya Tech, solo setup, and use-card
action outcomes. As implementation reaches each scope, those tests should move to their
own integration suites and assert that disagreement is rejected or the tail is narrowed.

This audit finds no intended existing linkage that the source rule must discard. It does
identify behaviors currently obtained from broad class-name substitution rather than a
real linkage; parity tests must distinguish those from defaults, contextual `Owner`/`This`
binding, and other non-linkage transformations before the heuristic is removed.

The primary issue trail is [#12](https://github.com/MartianZoo/solarnet/issues/12) for
`THEN`, [#37](https://github.com/MartianZoo/solarnet/issues/37) for class signatures, and
[#29](https://github.com/MartianZoo/solarnet/issues/29) for incremental `THEN` execution.

## Runtime model

Introduce an immutable binding substrate with:

- a stable identity within its source declaration;
- kind, source scope, and source expression or reserved name;
- the exact AST occurrence paths that participate;
- its binding source and current value or constraint.

Represent repetition-created type and scalar linkages on that substrate. Represent `This`
as a distinct contextual binding: it can have one occurrence, continues into subclasses,
and is fixed by the concrete component rather than by narrowing. `Class<This>` projects
the root class from that value. Keep `Owner` contextual substitution separate until its
semantics are similarly explicit.

The existing class-loader `selfBindings` paths are good evidence for occurrence-path
identity, but they should become `This` binding provenance rather than being folded into
the repeated-expression detector. Likewise, the special self trigger remains an event
selector associated with the binding; it is not modeled as an ordinary expression match.

Narrowing should constrain the linkage once, substitute the result into every occurrence,
and validate every containing expression. Conflicting constraints fail as one operation.
No behavior should depend on AST traversal order.

A task boundary is splittable exactly when no unresolved linkage crosses it. If executing
or choosing the head fixes a crossing linkage, rewrite the tail with that value before
splitting. Linkages already fixed while binding a component effect or matching a
trigger do not keep later instructions artificially joined.

Do not add explicit linkage syntax initially. If later canon needs two differently
structured expressions to share only part of a choice, add a named binding construct
rather than broadening recognition to semantic type equality.

## Path forward

1. Preserve source provenance through parsing and add an audit-only binding discoverer.
   It should report repeated-expression groups, scalar groups, and `This` occurrence paths,
   while explicitly rejecting sibling-argument inference. Compare its report with focused
   semantic tests; do not freeze the whole canon report as a change-detector test.
2. Add the binding substrate and migrate class-signature linkage grouping and `This`
   self-binding paths first, retaining behavior while replacing the private structural
   representations.
3. Migrate dependency-to-effect and linked trigger narrowing. Keep contextual `Owner`
   substitution separate, and express `This` through its contextual binding kind.
4. Make instruction narrowing linkage-aware. Fix type-linked `THEN` and transmutation
   before replacing the existing special case for `X`.
5. Clarify the two solo expressions, enable the dormant affected cards as their other
   blockers permit, and close or narrow the issue/TODO entries case by case.
6. Remove class-name-wide substitution and traversal-order scalar pairing only after
   positive and negative integration tests establish parity.

The essential tests are behavioral: linked choices must reject disagreement, independent
choices must still diverge, and a `THEN` must split as soon as every crossing linkage is
fixed. Market Manipulation, `Adjacency`, and ordinary incremental `THEN` execution are the
negative regression cases.

For `This`, integration tests should prove that a supertype's `Class<This>` follows the
concrete subclass, an explicit class literal stays fixed, and a self trigger scales with
the changed count but not with pre-existing component multiplicity.
