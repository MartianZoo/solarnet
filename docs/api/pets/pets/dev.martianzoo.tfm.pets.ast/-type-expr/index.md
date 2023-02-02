//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[TypeExpr](index.md)

# TypeExpr

[jvm]\
data class [TypeExpr](index.md)(val className: [ClassName](../-class-name/index.md), val arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](index.md)&gt; = listOf(), val refinement: [Requirement](../-requirement/index.md)? = null, val link: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null) : [PetNode](../-pet-node/index.md)

A noun expression. May be a simple type (`ClassName`), a parameterized type (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A refined type is the combination of a real type with various predicates.

## Constructors

| | |
|---|---|
| [TypeExpr](-type-expr.md) | [jvm]<br>fun [TypeExpr](-type-expr.md)(className: [ClassName](../-class-name/index.md), arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](index.md)&gt; = listOf(), refinement: [Requirement](../-requirement/index.md)? = null, link: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |
| [TypeParsers](-type-parsers/index.md) | [jvm]<br>object [TypeParsers](-type-parsers/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addArgs](add-args.md) | [jvm]<br>fun [addArgs](add-args.md)(vararg moreArgs: [ClassName](../-class-name/index.md)): [TypeExpr](index.md)<br>fun [addArgs](add-args.md)(vararg moreArgs: [TypeExpr](index.md)): [TypeExpr](index.md)<br>@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;addArgsFromClassNames&quot;)<br>fun [addArgs](add-args.md)(moreArgs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassName](../-class-name/index.md)&gt;): [TypeExpr](index.md)<br>fun [addArgs](add-args.md)(moreArgs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](index.md)&gt;): [TypeExpr](index.md) |
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [refine](refine.md) | [jvm]<br>fun [refine](refine.md)(ref: [Requirement](../-requirement/index.md)?): [TypeExpr](index.md) |
| [replaceArgs](replace-args.md) | [jvm]<br>fun [replaceArgs](replace-args.md)(vararg newArgs: [TypeExpr](index.md)): [TypeExpr](index.md)<br>fun [replaceArgs](replace-args.md)(newArgs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](index.md)&gt;): [TypeExpr](index.md) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [arguments](arguments.md) | [jvm]<br>val [arguments](arguments.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](index.md)&gt; |
| [className](class-name.md) | [jvm]<br>val [className](class-name.md): [ClassName](../-class-name/index.md) |
| [isTypeOnly](is-type-only.md) | [jvm]<br>val [isTypeOnly](is-type-only.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [link](link.md) | [jvm]<br>val [link](link.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null |
| [refinement](refinement.md) | [jvm]<br>val [refinement](refinement.md): [Requirement](../-requirement/index.md)? = null |
