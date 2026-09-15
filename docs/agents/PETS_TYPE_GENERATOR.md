# Pets Kotlin type generator

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

The JVM-only `codegen` module contains `pets-type-generator`, a KotlinPoet generator for the resolved
canonical Pets vocabulary. It generates declarations from `Canon.classTable`, so generation uses
the same validated, normalized master authority as the runtime rather than recreating it. Premise-
local declarations, including concrete Player seats and the generated `Premise`, are not part of
that reusable vocabulary.

Run it from Gradle:

```shell
./gradlew :codegen:runPetsTypeGenerator --args='--output-dir build/generated-pets-types-manual'
```

The installed `codegen` distribution also contains a `pets-type-generator` launcher. The supported
options are `--package`, `--file-prefix`, and `--output-dir`. Without `--output-dir`, the generated
files are written consecutively to standard output with filename comments.

The default output consists of `CanonicalPetsTypes.kt`, `CanonicalPetsCards.kt`,
`CanonicalPetsGoals.kt`, and `CanonicalPetsMapAreas.kt`. All files use the same package and together
contain one declaration for every class. Cards and map areas are the two large semantic families;
goals are kept separate for navigation even though they are smaller. These three files are content
catalogs and contain only concrete classes. Their abstract vocabulary and the remaining classes are
in the types file.

Pets abstract classes become sealed interfaces and concrete classes become final classes. Resolved
dependency linkages collapse linked dependency paths onto the same covariant Kotlin type variable;
independent dependency roots remain independent. When an inherited dependency is narrowed to a
class literal for a concrete class, that resolved type is written directly into the supertypes and
the redundant Kotlin type variable is omitted. Dependencies whose domains remain abstract still
become Kotlin type variables. Direct supertypes retain the resolved dependency projection of the
subtype. Parameter names come from their resolved bound root rather than the dependency key and
derive an acronym from its class name (`MA` for `MarsArea`). A unique
abbreviation remains unsuffixed, while independent parameters with the same abbreviation receive
zero-based suffixes (`T0`, `T1`).
The root generated `Component` extends `HasExpression` and exposes `val expression: Expression`.
Every concrete companion supplies `className`, a typed `c` class literal, an `invoke` factory, and a
`fromExpression` adapter. Calling a generated class such as `AerialMappers<Player>()` produces a
typed component occurrence; `AerialMappers.c` identifies the unapplied card class without inventing
an owner. `fromExpression` checks the class root and wraps an already prepared expression; it exists
for trusted runtime boundaries. The generated `generatedPetsComponent` factory selects that adapter
by concrete class name. The companion itself is not a Pets component.
Every generated `Class<C>` is also a `HasClassName`; its instance `className` is the represented
class's name rather than `Class`. Typed class-literal dependencies therefore expose their subject
without inspecting the expression argument.
Concrete payload constructors are internal. Each concrete class provides an `invoke` factory which
uses reified Kotlin parameters and `typeOf` to build the corresponding Pets AST directly. Thus
`CityTile<Player, Tharsis_4_4>()` has expression `CityTile<Player, Tharsis_4_4>` without accepting
a caller-supplied expression or parsing a string. Each final concrete class also implements
`toString()` as `expression.toString()`.
The generated `Class` component is covariant in the generated `Component` hierarchy. A Pets class
literal names an unapplied class root, so generic roots use Kotlin star projection:
`Class<CardResource>` becomes `Class<CardResource<*, *>>`. Pets does not allow a specialized type
such as `Class<Foo<Bar>>`, and the generator never emits one. This representation avoids a second
parallel marker hierarchy while preserving class-root subtype relationships. When an exact
class-literal bound would create a direct or mutual recursive Kotlin upper bound, the generator
widens only that bound to the represented class's first proper superclass. The stored runtime
`Expression` remains exact.

Declarations use hierarchy-aware depth-first ordering. Every superclass precedes its subclasses,
and a class is grouped with the branch of its first declared superclass whenever its other
superclasses have already been emitted.

Class properties become Kotlin properties. The interface where a property originates declares its
Kotlin contract, and concrete classes supply their effective resolved values. Pets `Number`,
`Metric`, `Requirement`, and `Requirement?` origins become Kotlin `Int`, `Metric`, `Requirement`,
and `Requirement?`, respectively. A numeric value narrowing a `Metric` origin is parsed as a
constant `Metric`; a property originating as `Number` remains an `Int`. Requirement and Metric
syntax is parsed once in the concrete class's companion and shared by its instances.

Every generated component exposes `_authoredEffects`. It is empty by default; a concrete class with
effects authored directly in its declaration overrides it with a companion-cached parsed list.
This is deliberately the direct authored declaration view: it neither accumulates inherited
effects nor substitutes defaults, lowers actions, or applies engine transformations. The leading
underscore distinguishes generated declaration metadata from a Pets class property.

The generator deliberately omits invariants, defaults, executable effects, and component values. The
multiplatform `generated` module treats `:codegen:generatePetsTypes` as its `commonMain` source
producer, so ordinary JVM and JavaScript compilation generates and compiles all four files. Its
small authored support source lives beside that generated vocabulary. The `gameConfig` factory
accepts separately typed lists of `Class<Module>`, `Class<Milestone<*>>`, `Class<Award>`, and
`Class<CardFront<*, *>>`; its `extra` string is reserved for exclusions, noncanonical test classes,
and selections outside those four roles. Functional tests use this boundary without changing the
untyped `GameConfig` data model.

Generated-value test adapters are additive: existing `ClassName` overloads remain. They are
extensions on `TfmGameplay<P>` so Kotlin carries the same generated owner into card occurrences and
operation bodies without a parallel gameplay wrapper. Hand-tracking operations instead accept
`Class<CardFront<*, *>>`, because drawing or discarding a card does not create an owned occurrence.
The broader front type is necessary because the current Pets hierarchy does not relate a card
front to its project, corporation, or prelude back. Concrete generated player types are deliberately
absent, so a generated owner parameter cannot name a premise-local runtime Player such as `Player1`.
Replay code can use the abstract generated `Player` only where an adapter consumes the card's class
root; owner-bearing selections such as `Trade<Player1, Ceres>` remain context-relative strings.
`GeneratedPetsTypesUsageTest` is the smaller executable example of construction, properties, and
compile-time shapes.
