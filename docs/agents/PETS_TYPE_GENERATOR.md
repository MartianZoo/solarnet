# Pets Kotlin type generator

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing the isolated Pets-to-Kotlin generator or its output model.
>
> **Status:** current tool behavior. No production or test source set consumes its output.

The JVM-only `codegen` module contains a KotlinPoet generator over any resolved Pets `ClassTable`.
It has no Canon or Terraforming Mars dependency. The current experiment is a library rather than a
command-line application; no production or test source set consumes its output.

The generator writes the supplied vocabulary to one Kotlin file. Abstract Pets classes become
interfaces and concrete classes become final classes. Generated components retain their Pets
`Expression`, expose direct authored effects and resolved class properties, and use typed root
descriptors for Pets class literals. Authored effects use `EffectTree` so whole-effect transform
marks remain visible in generated metadata.

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
