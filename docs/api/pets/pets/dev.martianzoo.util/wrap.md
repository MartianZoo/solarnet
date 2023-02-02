//[pets](../../index.md)/[dev.martianzoo.util](index.md)/[wrap](wrap.md)

# wrap

[jvm]\
fun &lt;[T](wrap.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [T](wrap.md)?.[wrap](wrap.md)(prefix: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), suffix: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), transform: ([T](wrap.md)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = { it }): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

If the &quot;receiver&quot; is null, returns the empty string; otherwise transforms it with [transform](wrap.md), converts the result to a string, and returns it with [prefix](wrap.md) before it and [suffix](wrap.md) after it.
