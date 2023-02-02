//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[From](index.md)

# From

[jvm]\
sealed class [From](index.md) : [PetNode](../-pet-node/index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |
| [ComplexFrom](-complex-from/index.md) | [jvm]<br>data class [ComplexFrom](-complex-from/index.md)(val className: [ClassName](../-class-name/index.md), val arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[From](index.md)&gt; = listOf(), val refinement: [Requirement](../-requirement/index.md)? = null) : [From](index.md) |
| [SimpleFrom](-simple-from/index.md) | [jvm]<br>data class [SimpleFrom](-simple-from/index.md)(val toType: [TypeExpr](../-type-expr/index.md), val fromType: [TypeExpr](../-type-expr/index.md)) : [From](index.md) |
| [TypeAsFrom](-type-as-from/index.md) | [jvm]<br>data class [TypeAsFrom](-type-as-from/index.md)(val typeExpr: [TypeExpr](../-type-expr/index.md)) : [From](index.md) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [visitChildren](../-pet-node/visit-children.md) | [jvm]<br>abstract fun [visitChildren](../-pet-node/visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [fromType](from-type.md) | [jvm]<br>abstract val [fromType](from-type.md): [TypeExpr](../-type-expr/index.md) |
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [toType](to-type.md) | [jvm]<br>abstract val [toType](to-type.md): [TypeExpr](../-type-expr/index.md) |

## Inheritors

| Name |
|---|
| [TypeAsFrom](-type-as-from/index.md) |
| [SimpleFrom](-simple-from/index.md) |
| [ComplexFrom](-complex-from/index.md) |
