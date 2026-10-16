# Catalogs and game premises

> **Status:** current static-model contracts.

## Content classification

Terraforming Mars declares `ABSTRACT CLASS Content`. A content class is a nonabstract subclass
of it, independent of whether its types are inhabited in a particular game. `CardFront`, `MarsMap`,
`Milestone`, `Award`, `ColonyTile`, `GlobalEvent`, and `Party` inherit it. Abstract categories such
as `ActiveCard` are not content classes; card backs, standard projects, map areas, and card-local
resources do not inherit it. Maps also inherit `Module`; party selection is still intrinsic to
Turmoil's setup rather than individually configurable.

`TfmCatalog` uses this classification for independent item availability and for identifying
non-card content roots in card resources. A reference from another item must not make content
stop being a pool candidate. File membership still identifies the `name:cards` pool and supporting
declarations; reference roots still select otherwise unreferenced support. Abstract goal vocabulary,
map areas, and goal helpers retain their separate availability handling. `Content` does not imply
that an item works without the rules of an expansion, nor does it select or create components.
Card eligibility, exact goal pools, replacements, map areas, and colony setup keep their existing
category-specific policies. Generic Catalog and Pets type processing need no Content-specific rule.

## From configuration to a class table

`GameConfig` is unresolved intent. Catalog resolution produces an immutable `GamePremise`: a
single set of signed Class selections, seated Player Classes, additive setup-component adjustments,
and premise-local declarations. Modules are derived from the selected Classes; they are not stored
as a parallel premise choice. Unchosen catalog Classes have no selection entry; negative entries
record explicit or policy-imposed exclusions rather than listing every unavailable Module or goal.
Setup adjustments and game-specific setup become ordinary effects of the generated premise Class.
The premise retains one immutable game `ClassTable` view sharing its
Catalog's compiled master structure; separate Worlds from that premise share compiled class facts
but no mutable game state. See
[`GamePremise.kt`](../../src/common/dev/martianzoo/catalog/GamePremise.kt).

After its inclusion closure is complete, that game Class Table also derives immutable executable
effects for its included Classes from the shared declarations. Using only settled premise facts
before any World exists, it removes impossible `IF` effects and fixed-true trigger or instruction
gates. Its facts are structural `Class<T>` representatives plus selected Module counts supplied by
`GamePremise`; arbitrary `HAS =N` invariants and configured Player counts do not qualify. Module
conditions ask about the completed configuration, deliberately not whether those components have
already been gained during bootstrap. Other components remain dynamic. Effect-execution consumers
read `ClassTable.effects`; a client deliberately inspecting the reusable unspecialized form reads
`Class.declaration.effects`.

See [class-selection examples](../class-existence-scenarios-draft.md) and the
[Catalog API](../../src/common/dev/martianzoo/catalog/Catalog.kt).
Runtime construction from this premise belongs to [Solarnet](../../../docs/agents/ENGINE.md#from-a-premise-to-a-ready-world).
