# Kotlin visibility

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** reducing declaration visibility, responding to explicit-API diagnostics, moving a
> caller across modules, or conducting a whole-project visibility audit.
>
> **Skip when:** changing behavior with no declaration-access change.
>
> **Status:** working rules, replacement audit design, and lessons from the 2026-10-09 experiment.

## Goal

Every Kotlin declaration should have the narrowest effective visibility compatible with:

1. all statically resolved callers and type references;
2. Kotlin language constraints such as overrides and exposed signature types;
3. runtime discovery such as test runners, `main`, JMH, reflection, and JavaScript exports; and
4. deliberately supported cross-module APIs.

The containing declaration matters. A public member of a private class is already private to
outside callers, so adding a redundant `private` modifier does not improve the effective API.

- Use `private` when every reference is legal under the declaration's private scope: the containing
  declaration for a member, or the file for a top-level declaration.
- Use `internal` when references cross the private scope but remain within the Kotlin module or an
  intended friend test compilation.
- Use `protected` when subclass access is required and ordinary module-wide access is not.
- Use `public` for cross-module use, required public overrides or exposed types, runtime-discovered
  entrypoints, and deliberate public contracts.

Current references establish the minimum needed by current code. They do not alone define a good
public API. Review intentionally public abstractions as families: constructors, factories,
structural properties, and paired queries should not acquire arbitrary holes merely because a
member currently has no repository caller.

## Configuration and established contracts

- [`solarnet.kotlin-base.gradle.kts`](../../gradle/build-logic/src/main/kotlin/solarnet.kotlin-base.gradle.kts)
  configures explicit API policy; search it for `explicitApi`.
- Each module's `build.gradle.kts` and actual Kotlin compilations define module and source-set
  relationships. Directory names are not sufficient evidence.
- Test classes and test-runner methods remain at least `internal`; the runner must discover them.
- Values in the test `cardnames` package deliberately remain public.
- Application `main` functions and JMH-discovered classes and methods remain public.
- Pets AST node types, their structural and construction surface, corresponding `PetTransformer`
  entrypoints, and string-to-AST entrypoints are a deliberate public API. Parsers must be able to
  construct nodes, attach source locations, and resolve lexical scope from another module. Helpers
  that do not belong to that contract can still be internal or private.
- `TfmTest.World.testAgent` must remain protected. Calls in subclasses resolve to that member
  extension. Making it private leaves the source compilable but silently rebinds those calls to an
  imported extension with different behavior.

Strict explicit-API mode prevents accidental production API growth. It does not establish that an
existing public declaration is necessary, and it does not cover implicit test visibility.

## Primary method: collect resolved usages while compiling normally

The audit must begin with semantic usage collection, not speculative source mutation.

Instrument ordinary Kotlin compilations temporarily, using the compiler's resolved FIR or IR rather
than text search. Compile each production compilation normally and emit a declaration/reference
graph. Then do the same for tests and other consumers. A reference record must identify the exact
resolved symbol, not merely a matching name; overloads, member extensions, imports, generated
accessors, constructors, and same-named declarations make textual counts unsafe.

The collector needs two kinds of records:

### Declaration inventory

For every authored and generated declaration, record:

- a stable identity based on module, source declaration, kind, and signature—not a byte offset;
- source file and containing declarations;
- kind, current written visibility, and effective visibility;
- every Kotlin compilation and source set containing the source;
- override/expect/actual relationships and language-imposed visibility constraints;
- whether the declaration is generated and, if so, the owning generator or template; and
- discovery signals and annotations, without treating an unfamiliar annotation as proof either way.

Include class-like declarations, objects, type aliases, constructors, constructor properties,
functions, properties, getters, setters, companion members, and implicit declarations for which
Kotlin permits explicit visibility. Do not propose a modifier where the container already supplies
the same effective restriction.

### Resolved reference edges

Record every resolved use that can impose an access requirement:

- calls, constructor calls, callable references, property reads and writes;
- type references in bodies, supertypes, generic arguments, annotations, and public signatures;
- inheritance and override relationships;
- generated code references; and
- references from production, test, benchmark, JVM, JavaScript, common, and platform compilations.

If Java or another compiled language is added later, its resolved references must produce compatible
edges or be covered by an explicit external-consumer rule. The repository currently has no authored
Java sources.

Each edge must retain the caller's module, compilation, file, containing declaration, and whether
the access occurs through subclass inheritance. Shared common sources may appear in several
compilations; merge identical edges without losing their distinct target memberships.

Compile failures are not the data source. A normal successful compilation already resolved the
symbols, and that resolution is exactly the information the audit needs.

## Deriving a proposed visibility

For each declaration, evaluate every visibility Kotlin legally permits against every edge. Use the
compiler's own access rules where possible rather than reimplementing Kotlin's details. `protected`
and `internal` are not a simple wider/narrower pair: a subclass in another module can use a
protected member but not an internal member, while an unrelated caller in the same module has the
opposite access. Select the first candidate that admits every required use in this policy order:
`private`, `internal`, `protected`, then `public`, except where an explicit contract such as
`testAgent` requires protected semantics.

Apply the following constraints:

1. Apply language constraints: overrides, expect/actual pairs, exposed types, inline visibility
   rules, constructor rules, and accessor rules.
2. Apply explicit discovery and contract requirements.
3. Examine every resolved edge:
   - keep `private` if every caller is within the applicable private scope;
   - require `protected` for intended subclass-only access outside that scope;
   - require at least `internal` for another caller in the same module or intended friend test;
   - require `public` for a caller in another module.
4. Cap the result by containing declarations. Do not add a modifier that changes no effective
   access.
5. Treat zero-edge declarations as private candidates, not automatic edits. First check runtime
   discovery and the surrounding public abstraction.

The report should explain each result with concrete evidence. For example:

```text
TfmTest.World.testAgent
current: protected
minimum: protected
reason: resolved calls from subclasses; private would hide the member and expose an imported
        extension with the same call syntax
```

A raw usage count is a useful summary, but caller location determines visibility. Five callers in
the same class permit `private`; a single cross-module caller requires `public`.

## Findings that the semantic graph cannot establish

Source references do not represent every caller. Maintain a small, reviewed set of general rules
for externally discovered declarations:

- test-framework discovery;
- JMH-generated benchmark discovery;
- JVM launchers and `main`;
- service loading, reflection, or serialization configured outside resolvable Kotlin references;
- JavaScript exports and host-page entrypoints; and
- deliberate APIs for clients not present in this repository.

These rules must be based on annotations, declaration roles, configuration, or documented API
families—not long lists of declaration IDs. An annotation is a signal to investigate and classify.
Tests should cover each discovery rule.

## Required feedback loop for rejected findings

Never merely suppress a finding and continue. For every proposal judged incorrect:

1. State the general reason it is incorrect.
2. Decide whether the cause is a missing reference edge, an incorrect scope/module model, a Kotlin
   language constraint, a discovery rule, or a deliberate API contract.
3. Fix that category in the collector or classifier and add a small semantic fixture proving it.
4. Remove the declaration-specific suppression and rerun the report.
5. Confirm that all findings of the same kind disappear or are reclassified with the correct reason.

A declaration-specific exception is allowed only for a genuinely unique, deliberate contract that
cannot be represented structurally. Put that decision near the declaration or in the owning design
document. Do not use an opaque state file as an API manifest.

## Audit procedure

### 1. Establish a reproducible baseline

1. Work from the exact current tree. Put temporary compiler instrumentation and outputs under
   `$TMPDIR`; never use another linked working copy as scratch space.
2. Run the normal formatting and build checks, `:repl:shadowJar`, and the enabled browser suite.
   Capture exact discovered test identifiers per test task, not only totals.
3. Generate required Kotlin sources and map them back to their generators.
4. Record the revision and starting diff so pre-existing edits are not attributed to the audit.

### 2. Collect and review evidence

5. Attach the temporary semantic collector to every production Kotlin compilation and compile each
   normally. Collect declarations and resolved references.
6. Repeat for tests, benchmarks, and other repository consumers. This is another normal compilation
   pass, not a per-declaration mutation loop.
7. Merge the reports by stable symbol identity, model friend compilations explicitly, and classify
   the narrowest legal visibility.
8. Review discovery rules and deliberate public API families. Every retained public/protected
   declaration and every proposed reduction from an intentional public family needs a stated reason.
9. Resolve every rejected proposal through the feedback loop above before accepting the report.

### 3. Apply changes once

10. Apply reviewed reductions in coherent module-sized groups. Prefer `private`; use `internal` or
    `protected` only when the report identifies the callers requiring it.
11. Change generators or templates rather than generated output, then regenerate.
12. Constructors and accessors are independent candidates where Kotlin provides visibility syntax.
    A primary-constructor `var` cannot acquire a setter modifier without restructuring the property;
    such restructuring is outside a visibility-only audit.

### 4. Verify the completed patch

13. Compile the complete changed project. Compiler failures now indicate collector/classifier gaps;
    fix the general rule before applying further edits.
14. Compare symbol resolution for affected call sites before and after, not merely compilation
    success. Any call resolving to a different declaration requires explicit review. This check is
    essential for imports, overloads, and member extensions such as `testAgent`.
15. Run the baseline build, test, browser, and generated-source checks again. Require the same exact
    discovered test identifiers.
16. Mechanically verify that authored Kotlin changed only reviewed visibility syntax and necessary
    formatting. Inspect the ordinary diff and apply [`REVIEW_CRITERIA.md`](REVIEW_CRITERIA.md).

Mutation may be used after semantic analysis to validate the complete reviewed patch, or to explore
a small ambiguous language case. It must not perform hundreds of full compilation probes as the
primary usage-discovery algorithm.

## Lessons from the 2026-10-09 experiment

The experiment accepted 48 decisions. Two merely spelled `internal` on overrides that already
inherited internal visibility; removing those no-op edits leaves 46 actual visibility reductions.
Those reductions remain useful, but the methodology is not acceptable for another audit.

Concrete results:

- It ran 731 mutation/compilation probes to accept 48 decisions, only 46 of which changed effective
  or written visibility after review. Most compiler phases were short, but repeated Pets phases
  sometimes took 17–32 minutes.
- A probe could invoke six JVM/JavaScript production and test compilation tasks. Compilation was
  being used as a very expensive approximation of a usage graph.
- Shared sources were reconsidered in separate source-set passes, and rejected candidates were not
  reliably remembered across those passes.
- The final decision state contained 146 declaration-specific `original` entries. Some general
  exclusions were added, but many rejected findings were stored as individual answers instead of
  producing better rules.
- Compiler success was too weak: privatizing protected `testAgent` changed which function subclass
  calls resolved to without causing a compile error. Behavioral testing found the problem; a
  before/after resolved-symbol comparison would have identified it directly.
- Byte offsets made convenient mutation keys but poor durable symbol identities. Formatting or
  unrelated edits can invalidate them.
- The parsed inventory and visibility-only diff checker demonstrated useful supporting techniques.
  They do not redeem the compile-by-bisection driver and should be reimplemented only as needed
  around the semantic usage graph.

The main lesson is simple: the compiler already knows which declaration every Kotlin reference
uses. Capture that information during ordinary compilation, classify callers by scope and module,
and compile the proposed reductions as a final check.
