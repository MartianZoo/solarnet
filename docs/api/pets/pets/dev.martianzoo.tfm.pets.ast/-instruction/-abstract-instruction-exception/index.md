//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Instruction](../index.md)/[AbstractInstructionException](index.md)

# AbstractInstructionException

[jvm]\
class [AbstractInstructionException](index.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html)

## Constructors

| | |
|---|---|
| [AbstractInstructionException](-abstract-instruction-exception.md) | [jvm]<br>fun [AbstractInstructionException](-abstract-instruction-exception.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [addSuppressed](index.md#282858770%2FFunctions%2F-1461504660) | [jvm]<br>fun [addSuppressed](index.md#282858770%2FFunctions%2F-1461504660)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
| [fillInStackTrace](index.md#-1102069925%2FFunctions%2F-1461504660) | [jvm]<br>open fun [fillInStackTrace](index.md#-1102069925%2FFunctions%2F-1461504660)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [getLocalizedMessage](index.md#1043865560%2FFunctions%2F-1461504660) | [jvm]<br>open fun [getLocalizedMessage](index.md#1043865560%2FFunctions%2F-1461504660)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getStackTrace](index.md#2050903719%2FFunctions%2F-1461504660) | [jvm]<br>open fun [getStackTrace](index.md#2050903719%2FFunctions%2F-1461504660)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt; |
| [getSuppressed](index.md#672492560%2FFunctions%2F-1461504660) | [jvm]<br>fun [getSuppressed](index.md#672492560%2FFunctions%2F-1461504660)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)&gt; |
| [initCause](index.md#-418225042%2FFunctions%2F-1461504660) | [jvm]<br>open fun [initCause](index.md#-418225042%2FFunctions%2F-1461504660)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [printStackTrace](index.md#-1769529168%2FFunctions%2F-1461504660) | [jvm]<br>open fun [printStackTrace](index.md#-1769529168%2FFunctions%2F-1461504660)()<br>open fun [printStackTrace](index.md#1841853697%2FFunctions%2F-1461504660)(p0: [PrintStream](https://docs.oracle.com/javase/8/docs/api/java/io/PrintStream.html))<br>open fun [printStackTrace](index.md#1175535278%2FFunctions%2F-1461504660)(p0: [PrintWriter](https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html)) |
| [setStackTrace](index.md#2135801318%2FFunctions%2F-1461504660) | [jvm]<br>open fun [setStackTrace](index.md#2135801318%2FFunctions%2F-1461504660)(p0: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [cause](index.md#-654012527%2FProperties%2F-1461504660) | [jvm]<br>open val [cause](index.md#-654012527%2FProperties%2F-1461504660): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [message](index.md#1824300659%2FProperties%2F-1461504660) | [jvm]<br>open val [message](index.md#1824300659%2FProperties%2F-1461504660): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
