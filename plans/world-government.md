# World Government Terraforming

## Decisions

- Add `SolarPhase`. Before World Government Terraforming, run the same game-end decision that can
  lead to `FinalGreeneryPhase`.
- `SolarPhase` must happen only when the Venus or Colonies bundle is present. Colonies maintenance
  work happens during this phase when the Colonies bundle is present.
- World Government Terraforming happens only when the Venus expansion is in use. Supporting the
  option to skip it can come later.
- If all global parameters are already maxed, skip World Government Terraforming. This also keeps
  the design ready for a future TR63 mode.
- Otherwise, increasing one global parameter is mandatory. A maxed parameter must not be a legal
  choice; this differs from the usual as-much-as-possible behavior of global-parameter gains.
- The Player owning `StartToken` chooses and completes the task, but the resulting changes are
  performed by `Engine`. We need a small explicit "do it as" mechanism, either task metadata or
  Pets syntax such as `AS Engine { GlobalParameter! }`.
- The World Government operation should originate from Pets behavior, probably through a Solar
  Phase signal that `StartToken` or another suitable component responds to.
- Owned effects on ordinary unowned components use the icon-grammar default of responding only to
  their Owner. World Government changes are performed by `Engine`, so they do not trigger those
  effects; authored `BY Anyone` effects still do. Engine-only `System` components such as phases
  and generations are actor-independent instead.
