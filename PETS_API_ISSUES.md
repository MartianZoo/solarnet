# Potential incompatible API changes before the Pets move

Reviewed 2026-10-09 against `701adefee`.

This is an inventory of existing plans and TODOs that could require changes in consumers of the
modules moving to Pets. It does not select additional work or make these changes prerequisites for
build separation. The linked roadmaps and TODOs own the decisions; this document records their API
implications. Reassess an entry when its owning plan changes.

The [selected split](PETS_ROADMAP.md#next-step-separate-gradle-builds) moves nine modules:
`pets`, `catalog`, `tfm-card-data`, `tfm-card-generator`, `tfm-canon`, `tfm-fake`, `almanac`,
`pets-tools`, and `codegen`. Runtime modules remain in Solarnet. The split itself currently promises
unchanged Kotlin packages and behavior.

The review covered both roadmaps, `TODO.md`, inline source TODOs, the relevant focused design notes,
`PLAYGROUND.md`, and local plan/TODO files. It distinguishes three kinds of impact:

- **Kotlin API:** changing or removing public types, members, constructors, or visibility.
- **Dependency API:** moving an existing declaration into a different required artifact.
- **Input and semantic contract:** changing accepted Pets/configuration input, exported vocabulary,
  or what an existing call means, even when its Kotlin signature stays the same.

Adding a case to a public sealed hierarchy can require consumers to update exhaustive `when`
expressions. A new optional constructor parameter can preserve source calls while changing binary
signatures. Neither should automatically be described as compatible.

## 1. Separate natural, compact, and full expressions

**Explicit API redesign; `pets`.**

The [expression TODO](TODO.md) calls for separate APIs for an object's naturally available
expression, a resolved Type's compact expression, and its full expression. Resolving arbitrary
syntax into either resolved form must explicitly take a `ClassTable`.

Current surfaces include [`HasExpression.expression` and `expressionFull`](src/common/dev/martianzoo/pets/HasExpression.kt),
their implementations on [`Type`](src/common/dev/martianzoo/pets/types/Type.kt), and syntax
[`Expression`](src/common/dev/martianzoo/pets/ast/Expression.kt). Separating these responsibilities
could remove inherited members or replace properties with context-taking functions. The related
question about globally shortest compact expressions could change rendered output without changing
signatures.

## 2. Contract temporary public APIs and accidental visibility

**Explicit contraction TODOs; primarily `pets`.**

The following public declarations have source TODOs identifying temporary exposure:

- [`DerivedClassLowerer`](src/common/dev/martianzoo/pets/DerivedClassLowerer.kt).
- [`Parsing.parse(expectedType, elementSource, derivedClasses)`](src/common/dev/martianzoo/pets/Parsing.kt).
- [`systemClassDeclarations`](src/common/dev/martianzoo/pets/SystemDeclarations.kt), intended to give
  way to the generic Catalog contract.
- [`PetTransformer.transformWithoutKindCheck`](src/common/dev/martianzoo/pets/PetTransformer.kt).
- [`PetNode.kind`](src/common/dev/martianzoo/pets/ast/PetNode.kt).
- [`Class.classTable`](src/common/dev/martianzoo/pets/types/Class.kt).
- The declaration-lowering surface around
  [`ClassDeclaration.authoredEffectsWithActions` and `effects`](src/common/dev/martianzoo/pets/data/ClassDeclaration.kt).

Removing these members or reducing their visibility breaks callers. Their final replacements and
the exact extent of contraction remain to be designed. Some comments call these Canon-facing APIs
even though current consumers also include the card generator.

The [consumer-API roadmap](PETS_ROADMAP.md#internal-design-and-consumer-api) also selects removal of
incidental implementation exposure. The [visibility guidance](docs/agents/VISIBILITY.md) preserves
deliberate public AST construction and transformation contracts; absence of a current caller alone
is not sufficient reason to hide a member. Binary-API checks detect changes but do not themselves
require an incompatible change.

## 3. Extract parsing and eventually precompile Canon

**Selected extraction; dependency changes certain if adopted, signature changes conditional.**

The [parser plan](PETS_ROADMAP.md#code-clarity-and-confidence) moves `Parsing`,
`DerivedClassLowerer`, the parsed system-declaration provider, and source diagnostics into an
optional parser module. Consumers of those APIs would need that artifact even if package names
and signatures stay unchanged. Existing string-to-AST convenience entry points also need an
ownership decision when the model no longer depends on parsing.

Separately, compiling canonical content into typed declarations could affect
[`StandardFormBundle`](src/common/dev/martianzoo/tfm/canon/StandardFormBundle.kt), whose public
constructor accepts a resource directory, resource filenames, and a text reader. Replacing that
loading contract would affect custom bundles. Precompiling built-in Canon alone need not remove
support for source-backed bundles; that decision has not been made.

## 4. Finish content selection and provenance

**Selected direction with open representation choices; `catalog` and `tfm-canon`.**

The [content-selection direction](PETS_ROADMAP.md#canon-and-game-rule-modeling) and
[selection TODOs](TODO.md) include selecting all applicable Content from a bundle, individual
Turmoil parties and maps, shared goals with distinct source provenance, replacement defaults,
Aridor making unused colony definitions available, and eligibility based on surviving meaning.

Potentially affected public contracts include [`GameConfig`](src/common/dev/martianzoo/catalog/GameConfig.kt),
[`ClassSelection`](src/common/dev/martianzoo/catalog/ClassSelection.kt),
[`GamePremise`](src/common/dev/martianzoo/catalog/GamePremise.kt),
[`Catalog`](src/common/dev/martianzoo/catalog/Catalog.kt),
[`TfmCatalog`](src/common/dev/martianzoo/tfm/canon/TfmCatalog.kt), and
[`Bundle`](src/common/dev/martianzoo/tfm/canon/Bundle.kt). For example, a bare Class Name currently
cannot distinguish the proposed sources of an identical goal. Preserving that distinction could
change selection arguments or stored selections. Removing transitional CardPack concepts can also
invalidate existing configuration strings.

Some eligibility fixes can stay internal. The delegated-Catalog lookup TODO likewise describes a
behavioral defect, not an already selected signature change.

## 5. Structural conjunction and explicit refinement candidates

**Selected language direction; public AST changes likely, exact design open.**

The [language roadmap](PETS_ROADMAP.md#language-and-modeling) proposes a structural conjunction such
as `Tile(IS Owned)` and a way for a refinement to reference its candidate. Current public surfaces
include `Expression.Refinement`, `Type.refinement`, and `PetTransformer`'s expression/refinement
methods. New node variants or binding information could require changes in visitors, transformers,
type queries, and AST constructors.

The corresponding Canon change retires nominal `OwnedTile`; callers that name that class would
need to express the structural relationship instead.

## 6. Replace implicit ownership with explicit OWN

**Selected direction; semantic incompatibility intended if inference is removed.**

The [ownership proposal](PETS_ROADMAP.md#language-and-modeling) replaces implicit insertion with
`OWN[...]`, including whole-effect transforms and automatic card/map marks. Existing declarations
that rely on an omitted owner would need migration.

[`PetElaborator`](src/common/dev/martianzoo/pets/PetElaborator.kt) currently accepts owner context
and performs lexical ownership insertion. Its input contract and possibly its parameters would
change. Whole-effect transforms may also affect `Effect`, `TransformNode`, and `TransformHandler`;
the existing transform model should be assessed before assuming a new public representation.

## 7. Task priorities, point events, and cleanup lifetime

**Working runtime direction with Pets consequences.**

The [priority design](docs/agents/SEQUENCING.md#task-priority-working-direction) needs a Pets
spelling for settlement and workflow work. A new instruction wrapper or effect attribute would
affect public `Instruction`/`InstructionTree` or `Effect` construction and interpretation.

The same design considers deleting `Temporary` and `TemporaryScope`. The Pets roadmap separately
calls for truthful point events and independent lifetime and log visibility. These changes reach
[`systemClassDeclarations`](src/common/dev/martianzoo/pets/SystemDeclarations.kt),
[`SystemClasses`](src/common/dev/martianzoo/pets/api/SystemClasses.kt), and their exported
inheritance contracts: for example, `Signal` currently inherits `MustCleanUp` and `Hidden`.
Removing or changing those facts affects custom declarations as well as Kotlin callers.

Delegated control, payment simplification, and Player/Admin attribution remain related sources of
design pressure. They may be solved in runtime code and authored rules, but any chosen change to
`Action.Cost`, actor expressions, or instruction structure would also belong in this inventory.

## 8. Properties, defaults, and cardinality

**Exploratory features and unresolved semantics; `pets`, with Canon consumers.**

The [language roadmap](PETS_ROADMAP.md#language-and-modeling) considers abstract property defaults,
instruction-valued printed facts, and richer cardinality. Instruction values would extend or
reshape the public sealed [`PropertyValue`](src/common/dev/martianzoo/pets/ast/PropertyValue.kt)
hierarchy and property-query methods on `Type` and `Class`.

The [TODOs](TODO.md) also ask how narrower dependency defaults retain compatible refinements and
what happens when an inherited default is disjoint from a subclass's bound. These affect
`ClassDeclaration.DefaultsDeclaration`, `Defaults`, and type-construction semantics; signature
changes are possible but not established. Applying the chosen rules may require changes in Canon.

## 9. Richer instruction composition

**Mixed selected and exploratory work; API changes conditional on representation.**

The [nested-fanout direction](PETS_ROADMAP.md#language-and-modeling) and [TODOs](TODO.md) cover:

- Nested `EACH`, using Quick Start as the proving case: possible effects on `Instruction.Each`
  and its binding contract.
- Requirement-gated action costs: possible new cases in `Action.Cost`.
- Production-box copying that preserves enclosing bindings and conditions: possible effects on
  Canon's public `cardProductionBoxes` query and transform contracts.
- Instruction intersection preserving unresolved shared aliases and unnamed Type unions: possible
  changes in representable expressions or `InstructionTree.intersect`'s result contract.
- Card-face/card-back compatibility in `PlayCard`: potential changes in exported dependencies and
  selection expressions, even without a Kotlin signature change.

These proposals do not prove a new AST is necessary. Existing composition may suffice. TaskForm
navigation itself belongs to the nonmoving Agent module.

## 10. Source identity and diagnostics

**Selected improvements; potential public payload and construction changes.**

The [diagnostic TODOs](TODO.md) ask for original filenames through parsing and generated inputs,
preserved provenance through synthesized trees, better span targets, and retained intersection
failure reasons.

[`SourceLocation`](src/common/dev/martianzoo/pets/api/SourceLocation.kt) currently stores submitted
source text, offset, and length. File identity could change its constructor and generated data-class
methods. Parsing parameters, `PetNode.sourceLocation`, and exception payloads are other possible
surfaces. Better messages or provenance propagation alone need not change signatures.

## 11. Restrict accepted authoring forms

**Some explicit TODOs, some decisions still open; input compatibility risk.**

The [authoring TODOs](TODO.md) include rejecting new type-variable declarations in `OR` triggers,
considering owner-local class declarations only in gains, considering rejection of `@` on concrete
types, and resolving identical concrete arguments in nested self-transmutations. These may cause
existing parser/elaborator calls to reject input they currently accept.

The invalid `DEFAULT` root marker is a documented defect rather than a newly selected language
rule. Fixes for defaulted variable references, owner-local specialization, lexical `This`,
represented-Class markers in `PROD`, and shorthand inference generally repair or extend behavior;
no Kotlin API replacement is currently selected for them. Keep the distinction between a corrected
implementation and a changed intended contract explicit during the specification-fidelity audit.

## 12. Remove or rename exported Canon vocabulary

**Concrete candidates; primarily declaration/configuration compatibility.**

The [Canon roadmap](PETS_ROADMAP.md#canon-and-game-rule-modeling), [TODOs](TODO.md), and inline TODOs
identify these removals or representation changes:

- Mons Insurance, Crash Site Cleanup, and Law Suit, if removal permits deleting attack-history and
  Actor-value reuse machinery.
- `ModulesReady`, replaced by correctly timed `BootstrapPhase` entry. This also changes the
  construction-order promise currently documented on `GamePremise.bootstrapClassName`.
- `FinalScoringPending`, replaced by a real `FinalScoringPhase`; associated `MeasureAward` and
  victory scheduling would change.
- `AdminOceanPlacement`, replaced by a rule separating chooser and attribution.
- `PartyLeader`'s name or representation, and possibly `ApplyRulingBonus` ownership.
- Named helpers `CimmeriaPlacementBonus`, `PlaceNeutralTiles`, and `StageForReplicatedProject`.
- `ProdOffset`, whose inline TODO proposes `ProductionBasement`. This also reaches the public
  Kotlin constant `TfmClasses.PROD_OFFSET`.
- Singleton watchers and deferred-work devices such as `TradeBarrier` and
  `TurmoilSolarOperation`, if general rules make them unnecessary.

Even when no public Kotlin class has the corresponding name, consumers can name these declarations
through the public ClassTable and parsing APIs. Changed names, supertypes, or dependency arguments
can therefore require consumer changes. Local card-expression simplifications are not automatically
API redesigns.

## 13. Generated Kotlin vocabulary and derived applications

**Exploratory adoption; no existing public generator entry point to preserve.**

The [codegen direction](PETS_ROADMAP.md#internal-design-and-consumer-api) permits adoption only if
the generated hierarchy replaces helpers, constants, and duplicated interpretation. That could
remove existing Pets/Canon APIs rather than merely add a generated layer.

`PetsTypeGenerator` is currently internal, and no production or test source set consumes its
output as an adopted vocabulary. Removing that experiment alone is not removal of a public Kotlin
generator API. English, icons, and Almanac can pressure the model's contracts, but their roadmap
entries do not identify an additional concrete breaking API change in these nine modules.

## 14. Older map-data replacement plan: reconcile status first

**Explicit removals in a preserved local plan; current commitment uncertain.**

The local [`CARD_DATA_LAYER_PLAN.md`](_local/CARD_DATA_LAYER_PLAN.md) describes replacing structured
map objects with loaded Classes and deleting public `MarsMapDefinition`, its `AreaDefinition`,
`MarsMapReader`, and the old catalog map-access surface. Current `TfmCatalog.marsMap` and
`marsMapDefinitions` still expose those objects. The plan also removes the map-regeneration tool.

That plan proposes a `tfm-map-data` module and checked-in generated output, whereas today's split
list has no such module and current card generation writes build output. Treat this as a concrete
possible API removal requiring reconciliation, not as a confirmed current implementation schedule.
The linked file is local-only and may not exist in another checkout.

## Suggested order and exclusions

Start with the expression API, temporary public APIs, parser placement, and content-selection
contracts. They directly determine what consumers import and call. Decide the Pets representation
of task priority early enough to avoid stabilizing an instruction API that the runtime redesign
immediately needs to change. Reconcile the older map plan before treating current map types as
settled.

There is no separately identified breaking Kotlin API proposal for `tfm-card-data` or `tfm-fake`;
the latter still inherits effects from Catalog and Canon changes. Generator/tool application entry
points and their dependencies may move, and renaming `pets-tools` to `tools` is tracked for after
the split. That module/task-name change is distinct from a Kotlin type or function redesign.

Runtime-only changes to `World`, `Instructor`, `LiveEffect`, Agents, workflow callbacks, recordings,
and test fixtures do not qualify on their own because those APIs are staying in Solarnet. Pure
documentation, build-runner updates, and ordinary defect repairs likewise do not establish a need
to redesign the moving APIs.
