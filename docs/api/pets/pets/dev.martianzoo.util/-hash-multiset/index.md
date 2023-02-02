//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[HashMultiset](index.md)

# HashMultiset

[jvm]\
class [HashMultiset](index.md)&lt;[E](index.md)&gt; : [MutableMultiset](../-mutable-multiset/index.md)&lt;[E](index.md)&gt;

## Constructors

| | |
|---|---|
| [HashMultiset](-hash-multiset.md) | [jvm]<br>fun [HashMultiset](-hash-multiset.md)() |

## Functions

| Name | Summary |
|---|---|
| [add](add.md) | [jvm]<br>open override fun [add](add.md)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>open override fun [add](add.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [addAll](add-all.md) | [jvm]<br>open override fun [addAll](add-all.md)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [clear](clear.md) | [jvm]<br>open override fun [clear](clear.md)() |
| [contains](contains.md) | [jvm]<br>open operator override fun [contains](contains.md)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsAll](contains-all.md) | [jvm]<br>open override fun [containsAll](contains-all.md)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [count](count.md) | [jvm]<br>open override fun [count](count.md)(element: [E](index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660) | [jvm]<br>open fun [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660)(p0: [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)&lt;in [E](index.md)&gt;) |
| [isEmpty](is-empty.md) | [jvm]<br>open override fun [isEmpty](is-empty.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [iterator](iterator.md) | [jvm]<br>open operator override fun [iterator](iterator.md)(): [MutableIterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)&lt;[E](index.md)&gt; |
| [mustRemove](must-remove.md) | [jvm]<br>open override fun [mustRemove](must-remove.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660) | [jvm]<br>open fun [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [remove](remove.md) | [jvm]<br>open override fun [remove](remove.md)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [removeAll](remove-all.md) | [jvm]<br>open override fun [removeAll](remove-all.md)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [removeIf](../-mutable-multiset/index.md#1655623621%2FFunctions%2F-1461504660) | [jvm]<br>open fun [removeIf](../-mutable-multiset/index.md#1655623621%2FFunctions%2F-1461504660)(p0: [Predicate](https://docs.oracle.com/javase/8/docs/api/java/util/function/Predicate.html)&lt;in [E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [retainAll](retain-all.md) | [jvm]<br>open override fun [retainAll](retain-all.md)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [setCount](set-count.md) | [jvm]<br>open override fun [setCount](set-count.md)(element: [E](index.md), newCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [spliterator](../-mutable-multiset/index.md#1956926474%2FFunctions%2F-1461504660) | [jvm]<br>open override fun [spliterator](../-mutable-multiset/index.md#1956926474%2FFunctions%2F-1461504660)(): [Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)&lt;[E](index.md)&gt; |
| [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660) | [jvm]<br>open fun [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [toArray](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) | [jvm]<br>~~open~~ ~~fun~~ ~~&lt;~~[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)~~&gt;~~ [~~toArray~~](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)~~(~~p0: [IntFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/IntFunction.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt;&gt;~~)~~~~:~~ [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt; |
| [tryRemove](try-remove.md) | [jvm]<br>open override fun [tryRemove](try-remove.md)(element: [E](index.md), occurrences: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Properties

| Name | Summary |
|---|---|
| [elements](elements.md) | [jvm]<br>open override val [elements](elements.md): [MutableSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)&lt;[E](index.md)&gt; |
| [size](size.md) | [jvm]<br>open override val [size](size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
