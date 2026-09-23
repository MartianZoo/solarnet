# Pets Kotlin type generator

> **Read when:** changing the isolated Pets-to-Kotlin generator or its output model.
>
> **Status:** current tool behavior. No production or test source set consumes its output.

The JVM-only `codegen` module contains a KotlinPoet generator over `Canon.classTable`. It uses the
resolved canonical master vocabulary and deliberately excludes premise-local declarations such as
concrete Player seats and `Premise`.

Run the generator with:

```shell
./gradlew :codegen:runPetsTypeGenerator --args='--output-dir build/generated-pets-types-manual'
```

The supported options are `--package`, `--file-prefix`, and `--output-dir`. Without an output
directory, files are written consecutively to standard output with filename comments. The
`generatePetsTypes` task writes the default package and prefix under the module's build directory,
and `check` compiles that output in an isolated source set. No production or test source set
consumes it.

The default output splits the vocabulary among `CanonicalPetsTypes.kt`,
`CanonicalPetsCards.kt`, `CanonicalPetsGoals.kt`, and `CanonicalPetsMapAreas.kt`. Abstract Pets
classes become interfaces and concrete classes become final classes. Generated components retain
their Pets `Expression`, expose direct authored effects and resolved class properties, and use
typed root descriptors for Pets class literals.

Open dependency roots become covariant Kotlin parameters. Shared Kotlin parameters come only from
the identity of Pets class-header Type variables; equal unmarked expressions remain independent.
The generator locates each variable occurrence by the identity of its authored `Expression`, then
maps that occurrence to its resolved dependency path. Inherited variable relationships are included
from the class hierarchy. This preserves T13-2: spelling the same bound twice does not itself make
one variable, while explicitly repeated markers do.

A variable shared only by nested positions becomes a Kotlin helper parameter used in the enclosing
dependency bounds. It is not itself a Pets dependency argument; expression reconstruction projects
only the generated parameters that represent open dependency roots.

Direct supertypes retain the subtype's resolved dependency projection. Concrete inherited bounds
are written directly instead of receiving redundant Kotlin parameters. Parameter names derive from
their bound class abbreviations, with numeric suffixes only when independent parameters would
otherwise have the same name.

The generator omits invariants, defaults, executable inherited effects, and component values. Its
tests verify the generated source model and semantic file split; the module compiles generated
output as a verification step but does not adopt it in callers.
