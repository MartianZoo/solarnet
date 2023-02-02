//[pets](../../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../../index.md)/[Action](../../index.md)/[Cost](../index.md)/[Or](index.md)

# Or

[jvm]\
data class [Or](index.md)(var costs: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Action.Cost](../index.md)&gt;) : [Action.Cost](../index.md)

## Constructors

| | |
|---|---|
| [Or](-or.md) | [jvm]<br>fun [Or](-or.md)(vararg costs: [Action.Cost](../index.md)) |
| [Or](-or.md) | [jvm]<br>fun [Or](-or.md)(costs: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Action.Cost](../index.md)&gt;) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](precedence.md) | [jvm]<br>open override fun [precedence](precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../../-pet-node/should-group-inside.md)(container: [PetNode](../../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toInstruction](to-instruction.md) | [jvm]<br>open override fun [toInstruction](to-instruction.md)(): [Instruction.Or](../../../-instruction/-or/index.md) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [costs](costs.md) | [jvm]<br>var [costs](costs.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Action.Cost](../index.md)&gt; |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
