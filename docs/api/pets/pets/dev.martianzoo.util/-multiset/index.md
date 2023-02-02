//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[Multiset](index.md)

# Multiset

[jvm]\
interface [Multiset](index.md)&lt;[E](index.md)&gt; : [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [contains](../-mutable-multiset/index.md#1825712522%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator fun [contains](../-mutable-multiset/index.md#1825712522%2FFunctions%2F-1461504660)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsAll](../-mutable-multiset/index.md#-348659435%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [containsAll](../-mutable-multiset/index.md#-348659435%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [count](count.md) | [jvm]<br>abstract fun [count](count.md)(element: [E](index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660) | [jvm]<br>open fun [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660)(p0: [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)&lt;in [E](index.md)&gt;) |
| [isEmpty](../-mutable-multiset/index.md#-719293276%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [isEmpty](../-mutable-multiset/index.md#-719293276%2FFunctions%2F-1461504660)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [iterator](index.md#-1438676347%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator override fun [iterator](index.md#-1438676347%2FFunctions%2F-1461504660)(): [Iterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html)&lt;[E](index.md)&gt; |
| [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660) | [jvm]<br>open fun [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [spliterator](../-mutable-multiset/index.md#1956926474%2FFunctions%2F-1461504660) | [jvm]<br>open override fun [spliterator](../-mutable-multiset/index.md#1956926474%2FFunctions%2F-1461504660)(): [Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)&lt;[E](index.md)&gt; |
| [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660) | [jvm]<br>open fun [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [toArray](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) | [jvm]<br>~~open~~ ~~fun~~ ~~&lt;~~[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)~~&gt;~~ [~~toArray~~](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)~~(~~p0: [IntFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/IntFunction.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt;&gt;~~)~~~~:~~ [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt; |

## Properties

| Name | Summary |
|---|---|
| [elements](elements.md) | [jvm]<br>abstract val [elements](elements.md): [MutableSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)&lt;[E](index.md)&gt; |
| [size](../-mutable-multiset/index.md#-113084078%2FProperties%2F-1461504660) | [jvm]<br>abstract val [size](../-mutable-multiset/index.md#-113084078%2FProperties%2F-1461504660): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Inheritors

| Name |
|---|
| [MutableMultiset](../-mutable-multiset/index.md) |

## Extensions

| Name | Summary |
|---|---|
| [filter](../filter.md) | [jvm]<br>fun &lt;[E](../filter.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [Multiset](index.md)&lt;[E](../filter.md)&gt;.[filter](../filter.md)(thing: ([E](../filter.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [Multiset](index.md)&lt;[E](../filter.md)&gt; |
| [map](../map.md) | [jvm]<br>fun &lt;[E](../map.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [T](../map.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [Multiset](index.md)&lt;[E](../map.md)&gt;.[map](../map.md)(thing: ([E](../map.md)) -&gt; [T](../map.md)): [Multiset](index.md)&lt;[T](../map.md)&gt; |
