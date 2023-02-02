//[pets](../../index.md)/[dev.martianzoo.tfm.api](index.md)/[lookUpProductionLevels](look-up-production-levels.md)

# lookUpProductionLevels

[jvm]\
fun [lookUpProductionLevels](look-up-production-levels.md)(game: [ReadOnlyGameState](-read-only-game-state/index.md), player: [TypeExpr](../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ClassName](../dev.martianzoo.tfm.pets.ast/-class-name/index.md), [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;

Returns a map with six entries, giving [player](look-up-production-levels.md)'s current production levels, adjusting megacredit product to account for our horrible hack.
