//[pets](../../../index.md)/[dev.martianzoo.tfm.data](../index.md)/[StateChange](index.md)

# StateChange

[jvm]\
data class [StateChange](index.md)(val count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, val gaining: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, val removing: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, val cause: [StateChange.Cause](-cause/index.md)? = null)

## Constructors

| | |
|---|---|
| [StateChange](-state-change.md) | [jvm]<br>fun [StateChange](-state-change.md)(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, gaining: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, removing: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null, cause: [StateChange.Cause](-cause/index.md)? = null) |

## Types

| Name | Summary |
|---|---|
| [Cause](-cause/index.md) | [jvm]<br>data class [Cause](-cause/index.md)(val actor: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), val trigger: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [jvm]<br>val [cause](cause.md): [StateChange.Cause](-cause/index.md)? = null<br>Information about what caused this state change, if we have it. |
| [count](count.md) | [jvm]<br>val [count](count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1<br>How many of the component were gained/removed/transmuted. A positive integer. Often 1, since many component types don't admit duplicates. |
| [gaining](gaining.md) | [jvm]<br>val [gaining](gaining.md): [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null<br>The concrete component that was gained, or `null` if this was a remove. |
| [removing](removing.md) | [jvm]<br>val [removing](removing.md): [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)? = null<br>The concrete component that was removed, or `null` if this was a gain. Can't be the same as `gained` (e.g. both can't be null). |
