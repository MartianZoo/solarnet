# Final review criteria for Pets

Apply these checks to the complete task diff before reporting completion or staging a commit.
Repository instructions in [AGENTS.md](../../AGENTS.md) remain authoritative.

- Preserve the requested scope and every accepted requirement. Remove unrelated cleanup and
  speculative abstractions. Prefer removal or existing Pets composition to new representations,
  processing stages, and component-specific APIs; use [VALUES.md](VALUES.md).
- Keep language, Catalog, Canon content, text, and static tools independent of runtime state,
  execution, player policy, and replays. A cross-project reference can document integration
  evidence without making it a build dependency.
- Keep rules with their owning component and retain a single semantic source. English realization
  is game-neutral; component identities and expansion-specific facts belong in supplied vocabulary.
- Do not report exposed dead ends, later failure, or caller rollback as a model defect without
  a separate game-rule failure. Protected Habitats may reject an attempted theft through its
  consequence; the client may prune that choice or expose it.
- Reject Canon `ActionScope` removal or dependent expiry that creates choices for the completed
  action. Review indirect effects too; fixed effects, cleanup, and offers for subsequent play
  are allowed. See [scope removal](GAME_FLOW.md#scope-removal-must-not-require-choices).
- Keep specifications, API contracts, and conformance evidence aligned. Apply
  [SPEC_FIDELITY.md](SPEC_FIDELITY.md); do not weaken intended semantics to match a known defect.
  Documentation work alone does not authorize behavior or visibility changes.
- Use meaningful tests appropriate to the changed contract, including affected browser behavior.
  Do not mirror production data or mistake a narrow passing assertion for complete verification.
  Follow [TESTING.md](TESTING.md), and identify known incorrect behavior explicitly.
- Update the smallest owning document when behavior, APIs, commands, or configuration change.
  Check moved documentation's incoming links, source paths, images, and section anchors.
- Preserve pre-existing work. Review conceptual cost and the complete diff; if committing is
  authorized, stage only the reviewed changes and ensure the staged state matches them.
