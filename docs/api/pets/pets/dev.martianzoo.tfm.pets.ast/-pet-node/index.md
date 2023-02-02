//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[PetNode](index.md)

# PetNode

[jvm]\
sealed class [PetNode](index.md)

An API object that can be represented as PETS source code.

## Types

| Name | Summary |
|---|---|
| [GenericTransform](-generic-transform/index.md) | [jvm]<br>interface [GenericTransform](-generic-transform/index.md)&lt;[P](-generic-transform/index.md) : [PetNode](index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](group-part-if-needed.md)(part: [PetNode](index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](precedence.md) | [jvm]<br>open fun [precedence](precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](should-group-inside.md)(container: [PetNode](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>abstract fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](kind.md) | [jvm]<br>abstract val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Inheritors

| Name |
|---|
| [Action](../-action/index.md) |
| [Cost](../-action/-cost/index.md) |
| [ClassName](../-class-name/index.md) |
| [Effect](../-effect/index.md) |
| [Trigger](../-effect/-trigger/index.md) |
| [From](../-from/index.md) |
| [Instruction](../-instruction/index.md) |
| [Requirement](../-requirement/index.md) |
| [ScalarAndType](../-scalar-and-type/index.md) |
| [TypeExpr](../-type-expr/index.md) |
