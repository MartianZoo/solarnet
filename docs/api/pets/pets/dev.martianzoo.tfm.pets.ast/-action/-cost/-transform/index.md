//[pets](../../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../../index.md)/[Action](../../index.md)/[Cost](../index.md)/[Transform](index.md)

# Transform

[jvm]\
data class [Transform](index.md)(val cost: [Action.Cost](../index.md), val transform: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Action.Cost](../index.md), [PetNode.GenericTransform](../../../-pet-node/-generic-transform/index.md)&lt;[Action.Cost](../index.md)&gt;

## Constructors

| | |
|---|---|
| [Transform](-transform.md) | [jvm]<br>fun [Transform](-transform.md)(cost: [Action.Cost](../index.md), transform: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [extract](extract.md) | [jvm]<br>open override fun [extract](extract.md)(): [Action.Cost](../index.md) |
| [groupPartIfNeeded](../../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../../../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../../../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../../-pet-node/should-group-inside.md)(container: [PetNode](../../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toInstruction](to-instruction.md) | [jvm]<br>open override fun [toInstruction](to-instruction.md)(): [Instruction.Transform](../../../-instruction/-transform/index.md) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [cost](cost.md) | [jvm]<br>val [cost](cost.md): [Action.Cost](../index.md) |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [transform](transform.md) | [jvm]<br>open override val [transform](transform.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
