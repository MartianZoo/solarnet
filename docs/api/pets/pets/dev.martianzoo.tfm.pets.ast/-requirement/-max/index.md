//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Requirement](../index.md)/[Max](index.md)

# Max

[jvm]\
data class [Max](index.md)(val sat: [ScalarAndType](../../-scalar-and-type/index.md)) : [Requirement](../index.md)

## Constructors

| | |
|---|---|
| [Max](-max.md) | [jvm]<br>fun [Max](-max.md)(sat: [ScalarAndType](../../-scalar-and-type/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [requiresThis](../requires-this.md) | [jvm]<br>open fun [requiresThis](../requires-this.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [shouldGroupInside](../../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../../-pet-node/should-group-inside.md)(container: [PetNode](../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [sat](sat.md) | [jvm]<br>val [sat](sat.md): [ScalarAndType](../../-scalar-and-type/index.md) |
