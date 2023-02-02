//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[ClassName](index.md)

# ClassName

[jvm]\
data class [ClassName](index.md)(asString: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [PetNode](../-pet-node/index.md), [Comparable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparable/index.html)&lt;[ClassName](index.md)&gt;

## Constructors

| | |
|---|---|
| [ClassName](-class-name.md) | [jvm]<br>fun [ClassName](-class-name.md)(asString: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |
| [Parsing](-parsing/index.md) | [jvm]<br>object [Parsing](-parsing/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addArgs](add-args.md) | [jvm]<br>fun [addArgs](add-args.md)(vararg specs: [ClassName](index.md)): [TypeExpr](../-type-expr/index.md)<br>fun [addArgs](add-args.md)(vararg specs: [TypeExpr](../-type-expr/index.md)): [TypeExpr](../-type-expr/index.md)<br>@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;addArgsFromClassNames&quot;)<br>fun [addArgs](add-args.md)(specs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassName](index.md)&gt;): [TypeExpr](../-type-expr/index.md)<br>fun [addArgs](add-args.md)(specs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../-type-expr/index.md)&gt;): [TypeExpr](../-type-expr/index.md) |
| [compareTo](compare-to.md) | [jvm]<br>open operator override fun [compareTo](compare-to.md)(other: [ClassName](index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [matches](matches.md) | [jvm]<br>fun [matches](matches.md)(regex: [Regex](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [type](type.md) | [jvm]<br>val [type](type.md): [TypeExpr](../-type-expr/index.md) |
