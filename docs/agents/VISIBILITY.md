# Kotlin visibility

> **Read when:** reducing declaration visibility, responding to explicit-API diagnostics, moving a
> caller across modules, or conducting a whole-project visibility audit.
>
> **Skip when:** changing behavior with no declaration-access change.
>
> **Status:** working rules and current audit procedure.

## Configuration entry points

- [`solarnet.kotlin-base.gradle.kts`](../../gradle/build-logic/src/main/kotlin/solarnet.kotlin-base.gradle.kts)
  — search for `explicitApi` to inspect repository-wide Kotlin API policy.
- The affected module's `build.gradle.kts` and source sets define the actual cross-module interface.
- For a whole-project audit, use a temporary copy under `$TMPDIR`; do not inspect another linked
  working copy.

Every declaration should have the narrowest effective visibility compatible with its real callers.

- Use `private` when all callers are in the containing declaration or file.
- Use `internal` when callers cross files but remain in one module.
- Use `protected` only for subclass access.
- Use `public` only for cross-module use, required public overrides, or externally discovered
  entrypoints.
- Test classes and `@Test` methods should be `internal`, not private; runners must still discover
  them.
- Application `main` functions and JMH-discovered classes and methods remain public.

Strict explicit-API mode prevents accidental production API growth, but it does not prove that an
existing public declaration is necessary and it does not cover implicit test visibility. IDE and
static-analysis suggestions are useful hints, not sufficient evidence across multiplatform source
sets and Gradle modules.

## Audit procedure

Perform a broad audit in a temporary project copy under `$TMPDIR`:

1. Change explicit `public` declarations to `internal`.
2. Change `internal` and `protected` declarations to `private` in separate passes.
3. Parse declarations with implicit visibility, including constructor properties, and attempt to
   make them private unless an enclosing private declaration already makes that effective.
4. Compile every production and test target, including both multiplatform tasks and the plain JVM
   `compileKotlin`/`compileTestKotlin` tasks.
5. Restore only declarations named by compiler diagnostics, repeating until compilation reaches a
   fixed point.
6. Make implicit test classes and `@Test` methods explicitly internal, and preserve externally
   discovered entrypoints.
7. Run the full build and compare discovered test-suite counts with the baseline.
8. Verify mechanically that the final source diff changes only visibility and formatting.

The compiler-guided mutation belongs in temporary storage. Do not add a permanent source-rewriting
framework merely for an occasional whole-project audit.
