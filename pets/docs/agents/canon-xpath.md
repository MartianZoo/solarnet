# Querying Canon with XPath

From the Solarnet working copy, run:

```sh
pets/query --form elaborated '/Canon/Card[@cost <= 10]/@name'
pets/query --form authored --xml '/Canon/Card[@name="NitrogenRichAsteroid"]'
pets/query --form authored --file pets/queries/canon-invariants/authored/animalHoldersHaveAnAnimalTag.xpath
```

The launcher builds and runs the JVM tool through Gradle. In a standalone Pets checkout, use
`./query` instead. Quote expressions to keep the shell from interpreting XPath syntax.
Choose `--form authored` or `--form elaborated` explicitly. `--xml` prints complete matching
nodes for inspection. Without it, matching Card/Class elements print their names; AST nodes print
the containing declaration and Pets spelling. Attributes, strings, numbers, and booleans print
their values. No matches produce no output and a successful exit. Invalid options or XPath fail.

## Use from Kotlin

Add `implementation(project(":search"))` to the consuming source set (or `testImplementation`
for JVM tests). The optional module supports JVM, browser JavaScript, and browser Wasm/JS.
It depends on Canon; the model modules do not depend on Search.

```kotlin
import dev.martianzoo.search.CanonQuery

val query = CanonQuery(CanonQuery.Form.ELABORATED)
val cards = query.evaluate("/Canon/Card[tags/Tag/@name = 'AnimalTag']")
println(cards.map { it.name })
println(query.evaluate("count(/Canon/Card)").single().value)
```

Reuse the query object to reuse its parsed document. Supply `catalog = myCatalog` for another
`TfmCatalog`. `query.xml` exports the complete selected view. Each `XPathMatch` exposes its string
`value`, optional serialized `xml`, top-level declaration `name`, containing declaration `owner`,
and normalized `pets` spelling. `describe()` produces the CLI's readable output.
Result XML is serialized only when you call `query.evaluate(xpath, includeXml = true)`; the CLI
sets this option for `--xml`. Ordinary queries retain names, owners, and Pets evidence without
serializing matching subtrees. These are query results, not mutable Pets AST or card objects.

The implementation uses Jaxen on JVM and native `document.evaluate()` in browsers, including
Wasm through JavaScript interop. There is no browser XPath package, JVM reflection dependency,
or Saxon dependency. All mappings and invariant expressions are shared across the three targets.

## Forms

Both forms describe the complete Canon class table, not a selected game or a live World.

- **Authored:** each class's declared actions and effects, retaining PROD transforms, explicit
  argument-list spelling, conditions, and source-shaped variables. Card fields are derived from
  the assembled class table. This is parsed Pets, not the original JSON5 text.
- **Elaborated:** `effects` uses `PetElaborator.classEffects`, including inherited effects,
  defaults, production lowering, atomization, and actions lowered into effects. There is no
  separate `actions` subtree in this form. Other declaration fields pass through the catalog's
  transform dispatcher; they are metadata, not effects specialized to a live component.

Neither form executes instructions, proves conditions achievable, or follows runtime consequences.
Elaborated expressions can still contain variables and choices. Metadata such as tags and resource
storage is derived from Canon in both forms.

## Find a nested production gain

```sh
pets/query --form elaborated '
  /Canon/Card[
    effects/Effect[trigger/WhenGain]/instruction//Gain[
      @count >= 2 and @gainingClass = "Production"
      and .//Expression[@className = "Plant"]
    ]
  ]'
```

This finds a fixed gain of at least two Plant production within a self-gain effect, even beneath
`Or` and `Gated`. It includes Nitrogen-Rich Asteroid. It does not sum separate gains or determine
whether a `Per` multiplier can exceed a threshold. To return the evidence rather than the cards,
move the bracketed path outside the Card predicate:

```xpath
/Canon/Card/effects/Effect[trigger/WhenGain]/instruction//Gain[
  @count >= 2 and @gainingClass = 'Production' and .//Expression[@className = 'Plant']
]
```

## Document shape

The root is `Canon[@form]`. Its children are `Card` for playable card classes and `Class` for
other classes. Each has `@id`, `@name`, and `@abstract`; the ID is the unique class name. Cards also expose `@deck`, `@category` for project
cards, `@cost` when available, `@resource` when storage identifies a resource type, and
`@hasPersistentBehavior` as described below.

Children:

- `isA/Type[@name]`: the nominal superclass closure, including the class itself.
- `tags/Tag[@name][@count]`: printed tag multiplicity, including the Event icon.
- `requirement` and `autoSelectWhen`: their requirement AST, when present.
- `supertypes` and `dependencies`: declared expression ASTs.
- `invariants`: declared requirements; `effectiveInvariants`: resolved inherited requirements.
- `properties/Property[@name]`: resolved property values, including type bounds.
- `defaults/universal`, `gainOnly`, `removeOnly`, `triggerOnly`: declared defaults, with optional
  `@quantifier` and expression children.
- `extraNodes`: remaining declaration AST nodes.
- `actions/Action/cost` and `actions/Action/instruction`: authored action operands.
- `effects/Effect[@automatic]/trigger` and `effects/Effect/instruction`: effect operands.

AST element names follow the concrete Pets class: `Gated`, `Per`, `Or`, `Gain`, `Subtract`, etc.
`@kind` distinguishes shared names, such as a requirement `Or` from an instruction `Or`.
Named operands are lowercase containers taken from constructor properties. For example:

```text
Gated
  gate/Min[@minimum]
    countedMetric/Count/expression/Expression
  inner/Gain
    scaledEx/ScaledExpression
      scalar/ActualScalar[@value]
      expression/Expression
Per
  inner/<instruction>
  metric/<metric>
Then
  stages/<instruction>*
  continuation/<instruction>
```

Structural fields use their Kotlin constructor-property names unchanged: scalar values become
attributes, node values become named child containers, and collections preserve iteration order.
Nulls are omitted. `ClassName`, `PropertyName`, and type-variable markers are scalar spellings when
used as property values. `@kind` is the node's Pets API kind. Every AST element has `@pets` with its
normalized Pets spelling; available `@line` and `@column` refer to its source fragment, not a file.
Expressions expose `@className`, `@argumentsSpecified`, optional `@typeVariableName`, `arguments/Expression`,
and `refinement`. Variable spelling is available; this first version does not expose binding identity.

Changes additionally expose derived `@count` for a fixed scalar, or `@xMultiple` for X; `@gaining`
and `@removing` for expression spelling; and `@gainingClass` and `@removingClass` for their root
class names. The original operands remain under `scaledEx` or `fromEx`; they are not duplicated
as alias subtrees. A missing quantifier means omitted source syntax, not a forced default.

Exact string comparison of `@gaining` is not subtype matching. Use the class-table closure for
nominal class checks:

```xpath
/Canon/Card[.//*[@gainingClass][id(@gainingClass)/isA/Type/@name = 'Tile']]
```

`id()` is XPath 1.0's indexed lookup. For example, `id('NitrogenRichAsteroid')` returns that
card and `id(@className)` follows an expression's class reference. The JVM adapter registers the
XML `id` attributes as IDs; browser XML DOMs recognize them directly.

XPath 1.0 supports recursive descendant searches (`//`), predicates, ancestor/descendant axes,
node-set comparisons, unions (`|`), and scalar functions such as `count()`, `not()`, and
`contains()`. Use `@className = 'Animal' or @className = 'Microbe'` for alternatives. There are no
`let` bindings, inline functions, sequences, or XPath 2.0/3.1 functions such as `exists()`.
No custom query grammar or test-specific Kotlin predicates are involved.

`:codegen:generatePetsXml` walks the sealed `PetNode` hierarchy using JVM reflection during the
build and generates an exhaustive field visitor in common Kotlin. It reads public properties
corresponding to primary-constructor parameters; computed getters, caches, and other incidental
members are excluded. An ordinary AST node or constructor field is therefore exposed without
editing Search. Unsupported field types fail generation. Reflection is a dependency of the
existing `codegen` tool only, not of Search or the model modules. The XML writer and the derived
facts below remain handwritten; there is no second maintained list of AST fields.

## Derived search facts

These attributes summarize the existing tree; they do not replace its nodes or evaluate game state.

- Changes expose `@gainingArgumentCount` and `@gainingArgumentsSpecified` alongside `@gainingClass`.
  These describe the gaining expression equally for `Gain` and `Transmute`. For example, `CityTile`
  has count `0` and specified `false`; `CityTile<>` has count `0` and specified `true`.
- Trigger nodes expose `@selfGainOnly` and `@endOnly`. Conditions, actors, and transforms are
  unwrapped for this classification; every branch of an alternative must qualify. Thus `End IF
  Plant` is end-only, whereas `Plant OR End` is not. The original wrappers remain queryable.
- Expressions expose `@shapeKey`, a document-local equality key for their structure with only the
  outer class name replaced. Arguments, their order, refinements, explicit `<>`, and structural
  variable markers remain significant. `Production<Class<MC>, Player>` and
  `ProdOffset<Class<MC>, Player>` share a key; changing the resource or owner changes the key.
  Keys are opaque, cannot be compared across documents, and do not establish type equivalence or
  resolved variable-binding identity. `Count` exposes the same key as `@countedShapeKey` and its
  expression's class as `@countedClass`.
- Cards expose `@hasPersistentBehavior`, computed from authored behavior in either query form.
  Actions and subscriptions other than self-gain-only or end-only behavior qualify. Positive
  exact invariants attaching non-tag components to `This`, and gains in nondirect self-gain
  effects, also qualify when the component or its superclasses declare actions or ongoing
  subscriptions. Nonautomatic bare `This:` effects are direct play benefits and excluded.
  For example, `Plant: MC` qualifies; `This: Plant` and `End IF Plant: VictoryPoint` do not.
  This is a syntactic classification, not analysis of whether conditions can be met or of
  arbitrary chains of consequences. It does not consult the card's declared color.

For example, find self-gain effects even when their triggers have conditions or actor restrictions:

```xpath
/Canon/Card/effects/Effect[trigger/*[@selfGainOnly = 'true']]
```

## Canon invariants

`queries/canon-invariants/authored/` and `elaborated/` contain executable counterparts of the
23 checks in `CanonInvariantsTest`, expressed as 24 queries: event tags and green/blue behavior
classification are checked separately. Each expression returns offenders; an empty result means that
check passed. Each query test adds a small, deliberately invalid Pets class to the full Canon
catalog and requires exactly a single result belonging to that decoy. For example, the city-gain
decoy omits `<>`, and the class-limit decoy permits two copies instead of a single copy. These
exercise the parser, catalog, XML projection, and XPath evaluator together. Additional tests cover
MC-offset arithmetic and other query edge cases. The original Canon checks are retained while
the search interface is established.

The production-box checks deliberately use authored form: lowering removes the box grouping.
The MC-offset query compares structural expression keys, including resource and owner; it
recognizes the current correction forms, not arbitrary algebraic equivalence.

Run the same suite on all supported platforms:

```sh
./gradlew :search:jvmTest :search:jsBrowserTest :search:wasmJsBrowserTest
```

Browser tests use headless Chrome (`CHROME_BIN` can select an alternative executable). The tests
load and elaborate Canon separately on each target. Positive/negative examples check the more
involved class-reference, category, requirement, and MC-offset expressions as well as zero hits
on Canon. `:search:test` includes all three targets.
