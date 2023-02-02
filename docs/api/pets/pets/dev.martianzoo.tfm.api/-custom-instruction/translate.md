//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[CustomInstruction](index.md)/[translate](translate.md)

# translate

[jvm]\
open fun [translate](translate.md)(game: [ReadOnlyGameState](../-read-only-game-state/index.md), arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt;): [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md)

When possible override this method, and compute an [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md) that can be executed in place of this one. When this isn't possible, override [execute](execute.md) instead.
