# Pets Kotlin type generator

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

The `tools` module contains `pets-type-generator`, a standalone KotlinPoet generator for the resolved
canonical Pets vocabulary. It loads every class in `ClassLoader(Canon)`, freezes that `ClassTable`,
and generates declarations from the table's `Class`, `Type`, and `Dependency` objects. The emitted
set is then restricted to classes parsed directly from gameplay `.pets` resources, including
`system.pets`; JSON-defined cards, map areas, milestones, awards, colonies, and standard actions are
not emitted. The generator does not re-parse or reinterpret declaration signatures.

Run it from Gradle:

```shell
./gradlew :tools:runPetsTypeGenerator --args='--output build/CanonicalPetsTypes.kt'
```

The installed `tools` distribution also contains a `pets-type-generator` launcher. The supported
options are `--package`, `--file-name`, and `--output`; without `--output`, source is written to
standard output.

Pets abstract classes become sealed interfaces and concrete classes become final classes. Every
dependency key present on a resolved Pets class becomes one covariant Kotlin type parameter, even
when its upper bound has narrowed to one concrete type; the generated file suppresses Kotlin's
`FINAL_UPPER_BOUND` warning for those faithful-but-predetermined parameters. Direct supertypes retain
the resolved dependency projection of the subtype. Parameter names come from their resolved bound
root rather than the dependency key. They use the bound class's explicit Pets short name when one
exists (`MA` for `MarsArea`) and otherwise derive an acronym from its class name. A unique
abbreviation remains unsuffixed, while independent parameters with the same abbreviation receive
zero-based suffixes (`T0`, `T1`).
The root generated `Component` interface exposes `val type: dev.martianzoo.types.Type`. Every
concrete component class takes that value in its constructor and stores it as an overriding
property, so code retains the resolved Pets type while using the generated nominal hierarchy.
Since a Pets class literal refers to a class root
rather than a fully parameterized component type, the output includes a parallel nominal hierarchy under `PetsClasses`;
`Class<PetsClasses.Plant>` can therefore narrow
`Class<PetsClasses.StandardResource>` without illegal recursive Kotlin bounds.
The ordinary component declarations appear first in the file; the dependency-free `PetsClasses`
markers appear last so that they are not mistaken for the modeled component types.

Declarations use hierarchy-aware depth-first ordering. Every superclass precedes its subclasses,
and a class is grouped with the branch of its first declared superclass whenever its other
superclasses have already been emitted.

The generator deliberately omits invariants, defaults, effects, metrics, and component values. The
`:tools:generatedPetsTypesClasses` verification task generates the entire canonical vocabulary and
compiles it as Kotlin; `:tools:check` depends on that task.
