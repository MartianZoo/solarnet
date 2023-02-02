//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[MutableGrid](index.md)

# MutableGrid

[jvm]\
class [MutableGrid](index.md)&lt;[E](index.md)&gt;(rows: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt;) : [AbstractSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-set/index.html)&lt;[E](index.md)&gt; , [Grid](../-grid/index.md)&lt;[E](index.md)&gt;

## Constructors

| | |
|---|---|
| [MutableGrid](-mutable-grid.md) | [jvm]<br>fun &lt;[E](index.md)&gt; [MutableGrid](-mutable-grid.md)(rows: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt;) |

## Functions

| Name | Summary |
|---|---|
| [column](column.md) | [jvm]<br>open override fun [column](column.md)(columnIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[E](index.md)?&gt; |
| [columns](columns.md) | [jvm]<br>open override fun [columns](columns.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [contains](contains.md) | [jvm]<br>open operator override fun [contains](contains.md)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsAll](index.md#1318510207%2FFunctions%2F-1461504660) | [jvm]<br>abstract override fun [containsAll](index.md#1318510207%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [diagonal](diagonal.md) | [jvm]<br>open override fun [diagonal](diagonal.md)(columnMinusRow: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[E](index.md)?&gt; |
| [diagonals](diagonals.md) | [jvm]<br>open override fun [diagonals](diagonals.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660) | [jvm]<br>open fun [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660)(p0: [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)&lt;in [E](index.md)&gt;) |
| [get](get.md) | [jvm]<br>open operator override fun [get](get.md)(rowIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), columnIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [E](index.md)? |
| [immutable](immutable.md) | [jvm]<br>fun [immutable](immutable.md)(): [Grid](../-grid/index.md)&lt;[E](index.md)&gt; |
| [isEmpty](is-empty.md) | [jvm]<br>open override fun [isEmpty](is-empty.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [iterator](iterator.md) | [jvm]<br>open operator override fun [iterator](iterator.md)(): [Iterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html)&lt;[E](index.md) &amp; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660) | [jvm]<br>open fun [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [row](row.md) | [jvm]<br>open override fun [row](row.md)(rowIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt; |
| [rows](rows.md) | [jvm]<br>open override fun [rows](rows.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [set](set.md) | [jvm]<br>fun [set](set.md)(rowIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), columnIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)): [E](index.md)? |
| [spliterator](index.md#-989466892%2FFunctions%2F-1461504660) | [jvm]<br>open override fun [spliterator](index.md#-989466892%2FFunctions%2F-1461504660)(): [Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)&lt;[E](index.md)&gt; |
| [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660) | [jvm]<br>open fun [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [toArray](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) | [jvm]<br>~~open~~ ~~fun~~ ~~&lt;~~[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)~~&gt;~~ [~~toArray~~](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)~~(~~p0: [IntFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/IntFunction.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt;&gt;~~)~~~~:~~ [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt; |

## Properties

| Name | Summary |
|---|---|
| [columnCount](column-count.md) | [jvm]<br>open override val [columnCount](column-count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [rowCount](row-count.md) | [jvm]<br>open override val [rowCount](row-count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [size](size.md) | [jvm]<br>open override val [size](size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
