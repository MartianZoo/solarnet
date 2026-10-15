# Pets Kotlin type generator

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing the isolated Pets-to-Kotlin generator or its output model.
>
> **Status:** current tool behavior. Generated output is compiled for verification only.

The JVM-only `codegen` module contains a KotlinPoet generator over any resolved Pets `ClassTable`.
Its production code has no Canon or Terraforming Mars dependency. The current experiment is a
library rather than a command-line application; gameplay code does not consume its output.

`./gradlew :codegen:test` generates Kotlin for every class in `Canon.classTable` and compiles it
in the isolated `generatedCanon` source set before running the generator's unit tests. Canon is a
test-only dependency. The test launcher `generateCanonCompilationTest.kt` writes under the module's
`build/generated/canonCompilationTest` directory (relocated by local build isolation). Generation
tracks the test runtime classpath, including Canon and the generator, so changes to either invalidate
the output. This verification also runs through the normal repository `test`, `check`, and `build`
tasks. It checks JVM compilation, not execution of the generated classes or JavaScript compilation.

The generated compilation check disables Kotlin incremental compilation for this source set only.
Earlier merge builds reported duplicate helper functions from a file containing a single copy;
cleaning `codegen` restored compilation. The precise trigger has not been reproduced. This scoped
workaround compiles the complete generated file when its inputs change, while retaining Gradle's
up-to-date checks and build cache. Other Kotlin compilation tasks keep their incremental behavior.

The generator writes the supplied vocabulary to one Kotlin file. Abstract Pets classes become
interfaces and concrete classes become final classes. Generated components retain their Pets
`Expression`, expose authored effects and resolved class properties, and use typed root
descriptors for Pets class literals.

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
are written instead of receiving redundant Kotlin parameters. Parameter names derive from
their bound class abbreviations, with numeric suffixes only when independent parameters would
otherwise have the same name.

The generator omits invariants, defaults, executable inherited effects, and component values. Its
unit tests verify the generated source model using small Pets vocabularies; the full Canon
compilation test verifies that the complete vocabulary produces compilable Kotlin.
