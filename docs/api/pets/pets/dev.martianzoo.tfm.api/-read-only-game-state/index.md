//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[ReadOnlyGameState](index.md)

# ReadOnlyGameState

[jvm]\
interface [ReadOnlyGameState](index.md)

The read-only portions of [GameState](../-game-state/index.md).

## Functions

| Name | Summary |
|---|---|
| [countComponents](count-components.md) | [jvm]<br>abstract fun [countComponents](count-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getComponents](get-components.md) | [jvm]<br>abstract fun [getComponents](get-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Multiset](../../dev.martianzoo.util/-multiset/index.md)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; |
| [isMet](is-met.md) | [jvm]<br>abstract fun [isMet](is-met.md)(requirement: [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

## Properties

| Name | Summary |
|---|---|
| [authority](authority.md) | [jvm]<br>open val [authority](authority.md): [Authority](../-authority/index.md) |
| [map](map.md) | [jvm]<br>open val [map](map.md): [MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md) |
| [setup](setup.md) | [jvm]<br>abstract val [setup](setup.md): [GameSetup](../-game-setup/index.md) |

## Inheritors

| Name |
|---|
| [GameState](../-game-state/index.md) |
