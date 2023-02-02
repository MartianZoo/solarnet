//[pets](../../../../index.md)/[dev.martianzoo.tfm.data](../../index.md)/[StateChange](../index.md)/[Cause](index.md)

# Cause

[jvm]\
data class [Cause](index.md)(val actor: [TypeExpr](../../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), val trigger: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

## Constructors

| | |
|---|---|
| [Cause](-cause.md) | [jvm]<br>fun [Cause](-cause.md)(actor: [TypeExpr](../../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), trigger: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [actor](actor.md) | [jvm]<br>val [actor](actor.md): [TypeExpr](../../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)<br>The concrete component that owns the instruction that caused this change. |
| [trigger](trigger.md) | [jvm]<br>val [trigger](trigger.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The ordinal of the previous change which triggered that instruction. |
