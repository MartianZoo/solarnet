//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Instruction](../index.md)/[Per](index.md)

# Per

[jvm]\
data class [Per](index.md)(val instruction: [Instruction](../index.md), val sat: [ScalarAndType](../../-scalar-and-type/index.md)) : [Instruction](../index.md)

## Constructors

| | |
|---|---|
| [Per](-per.md) | [jvm]<br>fun [Per](-per.md)(instruction: [Instruction](../index.md), sat: [ScalarAndType](../../-scalar-and-type/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](precedence.md) | [jvm]<br>open override fun [precedence](precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../-pet-node/should-group-inside.md)(container: [PetNode](../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [instruction](instruction.md) | [jvm]<br>val [instruction](instruction.md): [Instruction](../index.md) |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [sat](sat.md) | [jvm]<br>val [sat](sat.md): [ScalarAndType](../../-scalar-and-type/index.md) |
