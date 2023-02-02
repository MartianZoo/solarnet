//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[Grid](index.md)

# Grid

[jvm]\
interface [Grid](index.md)&lt;[E](index.md)&gt; : [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[E](index.md)&gt; 

A fixed-size two-dimensional array of nullable elements, where the elements &quot;know&quot; their own row and column number (as opposed to their being placed there). Important: there is no distinction made between a null cell, a missing cell, and a cell that is &quot;off the edge&quot; of the grid!

This actually works equally well for a hex grid. Imagine a parallelogram- shaped section of the hex grid, slanted like `/ /`. That is, rows are still horizontal, but what this class considers to be &quot;columns&quot; will actually slant up and to the right. In this case, the *diagonals* of this grid represent the columns that slant the other way.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [column](column.md) | [jvm]<br>open fun [column](column.md)(columnIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt; |
| [columns](columns.md) | [jvm]<br>abstract fun [columns](columns.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [contains](index.md#607101300%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator override fun [contains](index.md#607101300%2FFunctions%2F-1461504660)(element: [E](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsAll](../-mutable-grid/index.md#1318510207%2FFunctions%2F-1461504660) | [jvm]<br>abstract override fun [containsAll](../-mutable-grid/index.md#1318510207%2FFunctions%2F-1461504660)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[E](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [diagonal](diagonal.md) | [jvm]<br>abstract fun [diagonal](diagonal.md)(columnMinusRow: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt; |
| [diagonals](diagonals.md) | [jvm]<br>abstract fun [diagonals](diagonals.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660) | [jvm]<br>open fun [forEach](../-mutable-multiset/index.md#1532301601%2FFunctions%2F-1461504660)(p0: [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)&lt;in [E](index.md)&gt;) |
| [get](get.md) | [jvm]<br>abstract operator fun [get](get.md)(rowIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), columnIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [E](index.md)? |
| [isEmpty](index.md#-477621106%2FFunctions%2F-1461504660) | [jvm]<br>abstract override fun [isEmpty](index.md#-477621106%2FFunctions%2F-1461504660)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [iterator](index.md#1758193627%2FFunctions%2F-1461504660) | [jvm]<br>abstract operator override fun [iterator](index.md#1758193627%2FFunctions%2F-1461504660)(): [Iterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html)&lt;[E](index.md)&gt; |
| [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660) | [jvm]<br>open fun [parallelStream](../-mutable-multiset/index.md#-1592339412%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [row](row.md) | [jvm]<br>open fun [row](row.md)(rowIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt; |
| [rows](rows.md) | [jvm]<br>abstract fun [rows](rows.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)?&gt;&gt; |
| [spliterator](../-mutable-grid/index.md#-989466892%2FFunctions%2F-1461504660) | [jvm]<br>open override fun [spliterator](../-mutable-grid/index.md#-989466892%2FFunctions%2F-1461504660)(): [Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)&lt;[E](index.md)&gt; |
| [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660) | [jvm]<br>open fun [stream](../-mutable-multiset/index.md#135225651%2FFunctions%2F-1461504660)(): [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)&lt;[E](index.md)&gt; |
| [toArray](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) | [jvm]<br>~~open~~ ~~fun~~ ~~&lt;~~[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)~~&gt;~~ [~~toArray~~](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)~~(~~p0: [IntFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/IntFunction.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt;&gt;~~)~~~~:~~ [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](../-mutable-multiset/index.md#-1215154575%2FFunctions%2F-1461504660)&gt; |

## Properties

| Name | Summary |
|---|---|
| [columnCount](column-count.md) | [jvm]<br>abstract val [columnCount](column-count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [rowCount](row-count.md) | [jvm]<br>abstract val [rowCount](row-count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [size](index.md#1578037672%2FProperties%2F-1461504660) | [jvm]<br>abstract override val [size](index.md#1578037672%2FProperties%2F-1461504660): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Inheritors

| Name |
|---|
| [MutableGrid](../-mutable-grid/index.md) |
