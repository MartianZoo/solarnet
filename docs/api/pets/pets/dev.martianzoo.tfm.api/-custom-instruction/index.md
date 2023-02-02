//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[CustomInstruction](index.md)

# CustomInstruction

[jvm]\
abstract class [CustomInstruction](index.md)(val functionName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

For instructions that can't be expressed in Pets, write `@functionName(Arg1, Arg2...)` and implement this interface. Any [Authority](../-authority/index.md) providing a class declaration that uses that syntax will need to also return this instance from [Authority.customInstruction](../-authority/custom-instruction.md).

Only one of [translate](translate.md) or [execute](execute.md) need be overridden.

## Constructors

| | |
|---|---|
| [CustomInstruction](-custom-instruction.md) | [jvm]<br>fun [CustomInstruction](-custom-instruction.md)(functionName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Types

| Name | Summary |
|---|---|
| [ExecuteInsteadException](-execute-instead-exception/index.md) | [jvm]<br>class [ExecuteInsteadException](-execute-instead-exception/index.md) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html)<br>For use by the engine, not custom implementations. |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [jvm]<br>open fun [execute](execute.md)(game: [GameState](../-game-state/index.md), arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt;) |
| [translate](translate.md) | [jvm]<br>open fun [translate](translate.md)(game: [ReadOnlyGameState](../-read-only-game-state/index.md), arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt;): [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md)<br>When possible override this method, and compute an [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md) that can be executed in place of this one. When this isn't possible, override [execute](execute.md) instead. |

## Properties

| Name | Summary |
|---|---|
| [functionName](function-name.md) | [jvm]<br>val [functionName](function-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The name, in lowerCamelCase, that this function will be addressable by (preceded by `@`) in Pets code. [Authority.customInstruction](../-authority/custom-instruction.md) must return this instance when passed [functionName](function-name.md). |
