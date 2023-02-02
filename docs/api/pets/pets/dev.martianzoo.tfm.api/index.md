//[pets](../../index.md)/[dev.martianzoo.tfm.api](index.md)

# Package dev.martianzoo.tfm.api

## Types

| Name | Summary |
|---|---|
| [Authority](-authority/index.md) | [jvm]<br>abstract class [Authority](-authority/index.md)<br>A source of data about Terraforming Mars components. This project provides one, called `Canon`, containing only officially published materials. |
| [CustomInstruction](-custom-instruction/index.md) | [jvm]<br>abstract class [CustomInstruction](-custom-instruction/index.md)(val functionName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>For instructions that can't be expressed in Pets, write `@functionName(Arg1, Arg2...)` and implement this interface. Any [Authority](-authority/index.md) providing a class declaration that uses that syntax will need to also return this instance from [Authority.customInstruction](-authority/custom-instruction.md). |
| [GameSetup](-game-setup/index.md) | [jvm]<br>data class [GameSetup](-game-setup/index.md)(val authority: [Authority](-authority/index.md), val bundles: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, val players: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>A specification of the starting conditions for game. This should determine exactly what to do to prepare the game up until the point of the first player decisions. |
| [GameState](-game-state/index.md) | [jvm]<br>interface [GameState](-game-state/index.md) : [ReadOnlyGameState](-read-only-game-state/index.md)<br>A game engine implements this interface so that [CustomInstruction](-custom-instruction/index.md)s can speak to it. |
| [ReadOnlyGameState](-read-only-game-state/index.md) | [jvm]<br>interface [ReadOnlyGameState](-read-only-game-state/index.md)<br>The read-only portions of [GameState](-game-state/index.md). |
| [StubGameState](-stub-game-state/index.md) | [jvm]<br>open class [StubGameState](-stub-game-state/index.md)(auth: [Authority](-authority/index.md) = Authority.Minimal()) : [GameState](-game-state/index.md)<br>A game state that does basically nothing; for tests. |

## Functions

| Name | Summary |
|---|---|
| [lookUpProductionLevels](look-up-production-levels.md) | [jvm]<br>fun [lookUpProductionLevels](look-up-production-levels.md)(game: [ReadOnlyGameState](-read-only-game-state/index.md), player: [TypeExpr](../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ClassName](../dev.martianzoo.tfm.pets.ast/-class-name/index.md), [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;<br>Returns a map with six entries, giving [player](look-up-production-levels.md)'s current production levels, adjusting megacredit product to account for our horrible hack. |
| [standardResourceNames](standard-resource-names.md) | [jvm]<br>fun [standardResourceNames](standard-resource-names.md)(game: [ReadOnlyGameState](-read-only-game-state/index.md)): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ClassName](../dev.martianzoo.tfm.pets.ast/-class-name/index.md)&gt;<br>Returns the name of every concrete class of type `StandardResource`. |
