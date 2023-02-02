//[pets](../../../../index.md)/[dev.martianzoo.util](../../index.md)/[ParserGroup](../index.md)/[Builder](index.md)

# Builder

[jvm]\
class [Builder](index.md)&lt;[B](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [ParserGroup](../index.md)&lt;[B](index.md)&gt;

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [jvm]<br>fun [Builder](-builder.md)() |

## Functions

| Name | Summary |
|---|---|
| [finish](finish.md) | [jvm]<br>fun [finish](finish.md)(): [ParserGroup](../index.md)&lt;[B](index.md)&gt; |
| [isValid](../is-valid.md) | [jvm]<br>inline fun &lt;[T](../is-valid.md) : [B](index.md)&gt; [isValid](../is-valid.md)(input: TokenMatchesSequence): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>open override fun &lt;[T](is-valid.md) : [B](index.md)&gt; [isValid](is-valid.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](is-valid.md)&gt;, input: TokenMatchesSequence): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [parse](../parse.md) | [jvm]<br>inline fun &lt;[T](../parse.md) : [B](index.md)&gt; [parse](../parse.md)(input: TokenMatchesSequence): [T](../parse.md)<br>open override fun &lt;[T](parse.md) : [B](index.md)&gt; [parse](parse.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](parse.md)&gt;, input: TokenMatchesSequence): [T](parse.md) |
| [parser](../parser.md) | [jvm]<br>inline fun &lt;[T](../parser.md) : [B](index.md)&gt; [parser](../parser.md)(): Parser&lt;[T](../parser.md)&gt;<br>open override fun &lt;[T](parser.md) : [B](index.md)&gt; [parser](parser.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](parser.md)&gt;): Parser&lt;[T](parser.md)&gt; |
| [publish](publish.md) | [jvm]<br>inline fun &lt;[T](publish.md) : [B](index.md)&gt; [publish](publish.md)(parser: Parser&lt;[T](publish.md)&gt;): Parser&lt;[T](publish.md)&gt;<br>fun &lt;[T](publish.md) : [B](index.md)&gt; [publish](publish.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](publish.md)&gt;, parser: Parser&lt;[T](publish.md)&gt;): Parser&lt;[T](publish.md)&gt; |
