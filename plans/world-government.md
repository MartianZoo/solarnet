# World Government Terraforming

## Decisions

- Add `SolarPhase`. Before World Government Terraforming, run the same game-end decision that can
  lead to `FinalGreeneryPhase`.
- `SolarPhase` is universal as described in [native-workflow.md](native-workflow.md). Colonies
  maintenance work happens during its Colonies-owned subphase when that expansion is present.
- World Government Terraforming happens only when the Venus expansion is in use. A
  `NoWgtVariant` option should skip it while retaining the Solar phase.
- If all global parameters are already maxed, skip World Government Terraforming. This also keeps
  the design ready for a future TR63 mode.
- Otherwise, increasing one global parameter is mandatory. A maxed parameter must not be a legal
  choice; this differs from the usual as-much-as-possible behavior of global-parameter gains.
- The Player owning `StartToken` chooses and completes the task, but the resulting changes are
  performed by `Engine` through instruction-level `BY`, without adding another task identity.
- The World Government operation should originate from Pets behavior triggered by the Solar Phase.
- Owned effects on ordinary unowned components use the icon-grammar default of responding only to
  their Owner. World Government changes are performed by `Engine`, so they do not trigger those
  effects; authored `BY Anyone` effects still do.
