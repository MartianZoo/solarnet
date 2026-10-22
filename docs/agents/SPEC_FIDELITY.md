# API specifications, KDoc, and fidelity

> **Agent information:** This is an agent-maintained note recording documentation standards and
> user decisions.
>
> **Read when:** writing or reviewing API specifications or KDoc, or examining agreement among
> specifications, tests, and implementation.
>
> **Status:** current standards for the five priority runtime modules. Restored from the specification-fidelity
> guidance deleted in `e3fa40d08`, with the user's 2026-10-09 decisions incorporated. Historical audit
> leads and task-specific operating instructions are not current standards.

## Priority scope

The public APIs of these five modules are the priority for clear specifications. This scope does
not imply that their documentation or conformance audits are complete.

| Module | Contracts to explain |
| --- | --- |
| `state` | World observations, components, actors, tasks, events, recordings, and the lifetime and mutability of returned views. |
| `engine` | World construction, execution, tasks, effects, and invariant checks. |
| `agent` | Actor-scoped queries and mutations, task selection and narrowing, completion, delegated control, and forms. |
| `tfm-state` | Read-only Terraforming Mars queries, including resource names, printed production, and map lookup. |
| `tfm-engine` | Terraforming Mars game construction, gameplay calls, payments, and caller responsibilities for phase progression. |

## Shared documentation standard

Apply the [API documentation rules](../../pets/docs/agents/SPEC_FIDELITY.md) for audience,
authority, public coverage, KDoc style, and conformance evidence. That document also owns the
four priority static modules and the stronger Pets language/type specification requirement.
Together these lists preserve the nine-module priority scope.

Use [TESTING.md](TESTING.md) for runtime verification and [REVIEW_CRITERIA.md](REVIEW_CRITERIA.md)
for the complete final diff. Documentation work does not authorize behavior or visibility changes.
