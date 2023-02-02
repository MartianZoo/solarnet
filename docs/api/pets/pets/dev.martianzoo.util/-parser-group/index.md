//[pets](../../../index.md)/[dev.martianzoo.util](../index.md)/[ParserGroup](index.md)

# ParserGroup

[jvm]\
abstract class [ParserGroup](index.md)&lt;[B](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

## Constructors

| | |
|---|---|
| [ParserGroup](-parser-group.md) | [jvm]<br>fun [ParserGroup](-parser-group.md)() |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [jvm]<br>class [Builder](-builder/index.md)&lt;[B](-builder/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [ParserGroup](index.md)&lt;[B](-builder/index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [isValid](is-valid.md) | [jvm]<br>inline fun &lt;[T](is-valid.md) : [B](index.md)&gt; [isValid](is-valid.md)(input: TokenMatchesSequence): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>abstract fun &lt;[T](is-valid.md) : [B](index.md)&gt; [isValid](is-valid.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](is-valid.md)&gt;, input: TokenMatchesSequence): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [parse](parse.md) | [jvm]<br>inline fun &lt;[T](parse.md) : [B](index.md)&gt; [parse](parse.md)(input: TokenMatchesSequence): [T](parse.md)<br>abstract fun &lt;[T](parse.md) : [B](index.md)&gt; [parse](parse.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](parse.md)&gt;, input: TokenMatchesSequence): [T](parse.md) |
| [parser](parser.md) | [jvm]<br>inline fun &lt;[T](parser.md) : [B](index.md)&gt; [parser](parser.md)(): Parser&lt;[T](parser.md)&gt;<br>abstract fun &lt;[T](parser.md) : [B](index.md)&gt; [parser](parser.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](parser.md)&gt;): Parser&lt;[T](parser.md)&gt; |

## Inheritors

| Name |
|---|
| [Builder](-builder/index.md) |
