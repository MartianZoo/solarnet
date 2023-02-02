//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[KClassMultimap](index.md)

# KClassMultimap

[jvm]\
class [KClassMultimap](index.md)&lt;[B](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;(list: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[B](index.md)&gt; = listOf())

## Constructors

| | |
|---|---|
| [KClassMultimap](-k-class-multimap.md) | [jvm]<br>fun &lt;[B](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [KClassMultimap](-k-class-multimap.md)(list: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[B](index.md)&gt; = listOf()) |

## Functions

| Name | Summary |
|---|---|
| [get](get.md) | [jvm]<br>inline fun &lt;[T](get.md) : [B](index.md)&gt; [get](get.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[T](get.md)&gt;<br>fun &lt;[T](get.md) : [B](index.md)&gt; [get](get.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](get.md)&gt;): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[T](get.md)&gt; |
| [plusAssign](plus-assign.md) | [jvm]<br>inline operator fun &lt;[T](plus-assign.md) : [B](index.md)&gt; [plusAssign](plus-assign.md)(value: [T](plus-assign.md))<br>operator fun [plusAssign](plus-assign.md)(values: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[B](index.md)&gt;) |
| [put](put.md) | [jvm]<br>fun &lt;[T](put.md) : [B](index.md)&gt; [put](put.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](put.md)&gt;, value: [T](put.md)) |

## Properties

| Name | Summary |
|---|---|
| [map](map.md) | [jvm]<br>val [map](map.md): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;out [B](index.md)&gt;, [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[B](index.md)&gt;&gt; |
