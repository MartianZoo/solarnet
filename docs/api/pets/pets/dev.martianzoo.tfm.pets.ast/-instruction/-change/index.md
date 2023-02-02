//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Instruction](../index.md)/[Change](index.md)

# Change

[jvm]\
sealed class [Change](index.md) : [Instruction](../index.md)

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../-pet-node/should-group-inside.md)(container: [PetNode](../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [visitChildren](../../-pet-node/visit-children.md) | [jvm]<br>abstract fun [visitChildren](../../-pet-node/visit-children.md)(visitor: [PetVisitor](../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [count](count.md) | [jvm]<br>abstract val [count](count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [gaining](gaining.md) | [jvm]<br>abstract val [gaining](gaining.md): [TypeExpr](../../-type-expr/index.md)? |
| [intensity](intensity.md) | [jvm]<br>abstract val [intensity](intensity.md): [Instruction.Intensity](../-intensity/index.md)? |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [removing](removing.md) | [jvm]<br>abstract val [removing](removing.md): [TypeExpr](../../-type-expr/index.md)? |

## Inheritors

| Name |
|---|
| [Gain](../-gain/index.md) |
| [Remove](../-remove/index.md) |
| [Transmute](../-transmute/index.md) |
