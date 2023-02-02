//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[GameState](index.md)

# GameState

[jvm]\
interface [GameState](index.md) : [ReadOnlyGameState](../-read-only-game-state/index.md)

A game engine implements this interface so that [CustomInstruction](../-custom-instruction/index.md)s can speak to it.

## Functions

| Name | Summary |
|---|---|
| [applyChange](apply-change.md) | [jvm]<br>abstract fun [applyChange](apply-change.md)(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, removing: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, gaining: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, cause: [StateChange.Cause](../../dev.martianzoo.tfm.data/-state-change/-cause/index.md)? = null, amap: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |
| [countComponents](../-read-only-game-state/count-components.md) | [jvm]<br>abstract fun [countComponents](../-read-only-game-state/count-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getComponents](../-read-only-game-state/get-components.md) | [jvm]<br>abstract fun [getComponents](../-read-only-game-state/get-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Multiset](../../dev.martianzoo.util/-multiset/index.md)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; |
| [isMet](../-read-only-game-state/is-met.md) | [jvm]<br>abstract fun [isMet](../-read-only-game-state/is-met.md)(requirement: [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

## Properties

| Name | Summary |
|---|---|
| [authority](../-read-only-game-state/authority.md) | [jvm]<br>open val [authority](../-read-only-game-state/authority.md): [Authority](../-authority/index.md) |
| [map](../-read-only-game-state/map.md) | [jvm]<br>open val [map](../-read-only-game-state/map.md): [MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md) |
| [setup](../-read-only-game-state/setup.md) | [jvm]<br>abstract val [setup](../-read-only-game-state/setup.md): [GameSetup](../-game-setup/index.md) |

## Inheritors

| Name |
|---|
| [StubGameState](../-stub-game-state/index.md) |
