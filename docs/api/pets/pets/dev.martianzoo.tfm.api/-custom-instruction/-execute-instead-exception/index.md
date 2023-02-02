//[pets](../../../../index.md)/[dev.martianzoo.tfm.api](../../index.md)/[CustomInstruction](../index.md)/[ExecuteInsteadException](index.md)

# ExecuteInsteadException

[jvm]\
class [ExecuteInsteadException](index.md) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html)

For use by the engine, not custom implementations.

## Constructors

| | |
|---|---|
| [ExecuteInsteadException](-execute-instead-exception.md) | [jvm]<br>fun [ExecuteInsteadException](-execute-instead-exception.md)() |

## Functions

| Name | Summary |
|---|---|
| [addSuppressed](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#282858770%2FFunctions%2F-1461504660) | [jvm]<br>fun [addSuppressed](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#282858770%2FFunctions%2F-1461504660)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
| [fillInStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-1102069925%2FFunctions%2F-1461504660) | [jvm]<br>open fun [fillInStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-1102069925%2FFunctions%2F-1461504660)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [getLocalizedMessage](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1043865560%2FFunctions%2F-1461504660) | [jvm]<br>open fun [getLocalizedMessage](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1043865560%2FFunctions%2F-1461504660)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#2050903719%2FFunctions%2F-1461504660) | [jvm]<br>open fun [getStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#2050903719%2FFunctions%2F-1461504660)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt; |
| [getSuppressed](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#672492560%2FFunctions%2F-1461504660) | [jvm]<br>fun [getSuppressed](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#672492560%2FFunctions%2F-1461504660)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)&gt; |
| [initCause](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-418225042%2FFunctions%2F-1461504660) | [jvm]<br>open fun [initCause](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-418225042%2FFunctions%2F-1461504660)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [printStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-1769529168%2FFunctions%2F-1461504660) | [jvm]<br>open fun [printStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-1769529168%2FFunctions%2F-1461504660)()<br>open fun [printStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1841853697%2FFunctions%2F-1461504660)(p0: [PrintStream](https://docs.oracle.com/javase/8/docs/api/java/io/PrintStream.html))<br>open fun [printStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1175535278%2FFunctions%2F-1461504660)(p0: [PrintWriter](https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html)) |
| [setStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#2135801318%2FFunctions%2F-1461504660) | [jvm]<br>open fun [setStackTrace](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#2135801318%2FFunctions%2F-1461504660)(p0: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [cause](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-654012527%2FProperties%2F-1461504660) | [jvm]<br>open val [cause](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#-654012527%2FProperties%2F-1461504660): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [message](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1824300659%2FProperties%2F-1461504660) | [jvm]<br>open val [message](../../../dev.martianzoo.tfm.pets.ast/-instruction/-abstract-instruction-exception/index.md#1824300659%2FProperties%2F-1461504660): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
