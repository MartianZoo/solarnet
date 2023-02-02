//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[MutableMultiset](index.md)

# MutableMultiset

[jvm]\
interface [MutableMultiset](index.md)&lt;[E](index.md)&gt; : [MutableCollection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html)&lt;[E](index.md)&gt; , [Multiset](../-multiset/index.md)&lt;[E](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [add](index.md#-336316080%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [add](index.md#-336316080%2FFunctions%2F-1461504660)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>abstract fun [add](add.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [addAll](index.md#1622835035%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [addAll](index.md#1622835035%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [clear](index.md#1405312578%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [clear](index.md#1405312578%2FFunctions%2F-1461504660)() |
| [contains](index.md#1825712522%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator fun [contains](index.md#1825712522%2FFunctions%2F-1461504660)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsAll](index.md#-348659435%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [containsAll](index.md#-348659435%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [count](../-multiset/count.md) | [jvm]<br>abstract fun [count](../-multiset/count.md)(element: [E](index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [forEach](index.md#1532301601%2FFunctions%2F-1461504660) | [jvm]<br>open fun [forEach](index.md#1532301601%2FFunctions%2F-1461504660)(p0: [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)&lt;in [E](index.md)&gt;) |
| [isEmpty](index.md#-719293276%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [isEmpty](index.md#-719293276%2FFunctions%2F-1461504660)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [iterator](index.md#1177836957%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator override fun [iterator](index.md#1177836957%2FFunctions%2F-1461504660)(): [MutableIterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)&lt;[E](index.md)&gt; |
| [mustRemove](must-remove.md) | [jvm]<br>abstract fun [mustRemove](must-remove.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [parallelStream](index.md#-1592339412%2FFunctions%2F-1461504660) | [jvm]<br>open fun [parallelStream](index.md#-1592339412%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [remove](index.md#-866564265%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [remove](index.md#-866564265%2FFunctions%2F-1461504660)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [removeAll](index.md#-1840690270%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [removeAll](index.md#-1840690270%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [removeIf](index.md#1655623621%2FFunctions%2F-1461504660) | [jvm]<br>open fun [removeIf](index.md#1655623621%2FFunctions%2F-1461504660)(p0: [Predicate](https://docs.oracle.com/javase/8/docs/api/java/util/function/Predicate.html)&lt;in [E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [retainAll](index.md#-1279972125%2FFunctions%2F-1461504660) | [jvm]<br>abstract fun [retainAll](index.md#-1279972125%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [setCount](set-count.md) | [jvm]<br>abstract fun [setCount](set-count.md)(element: [E](index.md), newCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [spliterator](index.md#1956926474%2FFunctions%2F-1461504660) | [jvm]<br>open override fun [spliterator](index.md#1956926474%2FFunctions%2F-1461504660)(): [Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)&lt;[E](index.md)&gt; |
| [stream](index.md#135225651%2FFunctions%2F-1461504660) | [jvm]<br>open fun [stream](index.md#135225651%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [toArray](index.md#-1215154575%2FFunctions%2F-1461504660) | [jvm]<br>~~open~~ ~~fun~~ ~~&lt;~~[T](index.md#-1215154575%2FFunctions%2F-1461504660) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)~~&gt;~~ [~~toArray~~](index.md#-1215154575%2FFunctions%2F-1461504660)~~(~~p0: [IntFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/IntFunction.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](index.md#-1215154575%2FFunctions%2F-1461504660)&gt;&gt;~~)~~~~:~~ [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](index.md#-1215154575%2FFunctions%2F-1461504660)&gt; |
| [tryRemove](try-remove.md) | [jvm]<br>abstract fun [tryRemove](try-remove.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Properties

| Name | Summary |
|---|---|
| [elements](../-multiset/elements.md) | [jvm]<br>abstract val [elements](../-multiset/elements.md): [MutableSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)&lt;[E](index.md)&gt; |
| [size](index.md#-113084078%2FProperties%2F-1461504660) | [jvm]<br>abstract val [size](index.md#-113084078%2FProperties%2F-1461504660): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Inheritors

| Name |
|---|
| [HashMultiset](../-hash-multiset/index.md) |
