//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[StubGameState](index.md)

# StubGameState

[jvm]\
open class [StubGameState](index.md)(auth: [Authority](../-authority/index.md) = Authority.Minimal()) : [GameState](../-game-state/index.md)

A game state that does basically nothing; for tests.

## Constructors

| | |
|---|---|
| [StubGameState](-stub-game-state.md) | [jvm]<br>fun [StubGameState](-stub-game-state.md)(auth: [Authority](../-authority/index.md) = Authority.Minimal()) |

## Functions

| Name | Summary |
|---|---|
| [applyChange](apply-change.md) | [jvm]<br>open override fun [applyChange](apply-change.md)(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), removing: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)?, gaining: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)?, cause: [StateChange.Cause](../../dev.martianzoo.tfm.data/-state-change/-cause/index.md)?, amap: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [countComponents](count-components.md) | [jvm]<br>open override fun [countComponents](count-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getComponents](get-components.md) | [jvm]<br>open override fun [getComponents](get-components.md)(typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Multiset](../../dev.martianzoo.util/-multiset/index.md)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; |
| [isMet](is-met.md) | [jvm]<br>open override fun [isMet](is-met.md)(requirement: [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

## Properties

| Name | Summary |
|---|---|
| [authority](../-read-only-game-state/authority.md) | [jvm]<br>open val [authority](../-read-only-game-state/authority.md): [Authority](../-authority/index.md) |
| [map](../-read-only-game-state/map.md) | [jvm]<br>open val [map](../-read-only-game-state/map.md): [MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md) |
| [setup](setup.md) | [jvm]<br>open override val [setup](setup.md): [GameSetup](../-game-setup/index.md) |
