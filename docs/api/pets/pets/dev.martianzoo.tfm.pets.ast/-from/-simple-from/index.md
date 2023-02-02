//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[From](../index.md)/[SimpleFrom](index.md)

# SimpleFrom

[jvm]\
data class [SimpleFrom](index.md)(val toType: [TypeExpr](../../-type-expr/index.md), val fromType: [TypeExpr](../../-type-expr/index.md)) : [From](../index.md)

## Constructors

| | |
|---|---|
| [SimpleFrom](-simple-from.md) | [jvm]<br>fun [SimpleFrom](-simple-from.md)(toType: [TypeExpr](../../-type-expr/index.md), fromType: [TypeExpr](../../-type-expr/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../-pet-node/should-group-inside.md)(container: [PetNode](../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [fromType](from-type.md) | [jvm]<br>open override val [fromType](from-type.md): [TypeExpr](../../-type-expr/index.md) |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [toType](to-type.md) | [jvm]<br>open override val [toType](to-type.md): [TypeExpr](../../-type-expr/index.md) |
