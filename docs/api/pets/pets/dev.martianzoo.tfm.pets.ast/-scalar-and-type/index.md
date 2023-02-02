//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[ScalarAndType](index.md)

# ScalarAndType

[jvm]\
data class [ScalarAndType](index.md)(val scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, val typeExpr: [TypeExpr](../-type-expr/index.md) = MEGACREDIT.type) : [PetNode](../-pet-node/index.md)

## Constructors

| | |
|---|---|
| [ScalarAndType](-scalar-and-type.md) | [jvm]<br>fun [ScalarAndType](-scalar-and-type.md)(scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, typeExpr: [TypeExpr](../-type-expr/index.md) = MEGACREDIT.type) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>fun [toString](to-string.md)(forceScalar: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, forceType: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [scalar](scalar.md) | [jvm]<br>val [scalar](scalar.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1 |
| [typeExpr](type-expr.md) | [jvm]<br>val [typeExpr](type-expr.md): [TypeExpr](../-type-expr/index.md) |
