# Pets elaboration and effect specialization

> **Status:** current static transformation responsibilities.
> The [language specification](../pets-language-spec.md) owns the semantic rules.

Pets moves toward executable data without requiring a separate Kotlin type for each stage:

1. **Authored Pets** retains the rule-shaped AST, including omitted defaults, marked syntax, and
   contextual names.
2. **Elaborated Pets** applies defaults, action lowering, atomization, and registered marked-syntax
   transformations while preserving linked variable identity.
3. **Context-closed Pets** binds the Class, component, trigger, and Player-owned values supplied by
   the surrounding rule. It can still contain intentional choices.

`PetElaborator` owns the shared authored-to-elaborated transformations. Class effect compilation,
session input, stored metric expansion, and custom-instruction output must reenter through the
appropriate elaborator entry point rather than reproducing part of the pipeline.

Loaded declarations deliberately retain authored effects and actions separately from executable
effects. Consumers may inspect authored syntax as metadata or resubmit it as rule data, but
resubmitted data must traverse normal elaboration again. Do not treat an already specialized effect
as the declaration's source form.

`This` is contextual declaration syntax, not a free-standing runtime Type. By component-effect
compilation, ordinary `This` occurrences have become the exact effect-bearing Type. Bare `Owned`
expressions receive the nearest lexical `Me` where the rule supplies one; literal `Anyone` stays broad.

See [lexical ownership](OWNERSHIP.md) for `Me` and inference, and
[Catalogs and game premises](CATALOG.md) for compiled game-specific effects. The current
implementation is [PetElaborator.kt](../../src/common/dev/martianzoo/pets/PetElaborator.kt).

Live resolution reads the mutable World after task selection; that step belongs to
[Solarnet](../../../docs/agents/ENGINE.md#one-instruction-becomes-events-in-stages).

## Represented-tag bindings

`WildTagElection` uses the explicit `@Tag<WildTagElection<@WildTag, ActionScope>>` assignment.
The shorter `@Tag<This>` still fails in `recordTypeVariableScopes`: the represented tag's holder
is resolved while `This` is lexical, yielding `no class named This`. Changing the holder's lifetime
did not fix that elaboration limitation. Keep the explicit spelling rather than adding another
assignment representation to work around it.
