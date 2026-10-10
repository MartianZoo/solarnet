# Canon map Pets generation

> **Agent information:** This is an agent-maintained procedure for agents.
>
> **Read when:** editing map diagrams, generated area declarations, map prefix naming, or the
> `regenerateMapAreas` tool.
>
> **Skip when:** changing tile-placement gameplay without changing map topology or metadata.

## Source map

- [Tharsis `tharsis.pets`](../../src/common/dev/martianzoo/tfm/canon/TharsisMap/tharsis.pets) — an
  example authored diagram followed by generated area declarations.
- [`MarsMapReader.kt`](../../src/common/dev/martianzoo/tfm/canon/MarsMapReader.kt) — diagram, area
  kind, and bonus-sigil decoding.
- [`MarsMapDefinition.kt`](../../src/common/dev/martianzoo/tfm/canon/MarsMapDefinition.kt) — parsed
  map and area representation.
- [`regenerateMapAreas.kt`](../../src/jvm/dev/martianzoo/tfm/petstools/regenerateMapAreas.kt) —
  declaration construction, round-trip validation, and file rewriting.

Map topology and bonuses are authored in the diagram comment immediately after the generated-area
marker in each map's `.pets` file. The first character of a cell selects the area kind; remaining
characters encode bonuses according to `MarsMapReader.BONUSES`. A digit before another sigil is a
multiplier; a final digit is itself a sigil, such as `O6` for an ocean and a -6 M€ placement cost.

Diagram indentation locates areas on slant-columns. The leftmost occupied slant-column is column 1.
Maps whose largest row or column is at least 10 use two digits for every generated coordinate;
smaller maps retain unpadded names.

Each map's `.pets` file keeps its Module, milestones, awards, diagram, and generated areas together.
Run:

```shell
./gradlew :pets-tools:regenerateMapAreas
```

The task rewrites the diagram and area block in every recognized map file and verifies that the
generated declarations round-trip through the Pets parser. Review the resulting source diff; do
not edit generated area declarations by hand.
Delegate sigils are emitted directly as
`OWN[Placement<This> IF Class<PartyDelegate>: PartyDelegate]` (or `2 PartyDelegate` for `DD`). The
trigger-side condition makes the bonus absent from games without party delegates while keeping it
mandatory when that class is present. Cimmeria's colony-and-cost bonus remains a named Signal
because its two consequences form one conditional package.
