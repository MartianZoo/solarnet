//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[Effect](index.md)

# Effect

[jvm]\
data class [Effect](index.md)(val trigger: [Effect.Trigger](-trigger/index.md), val instruction: [Instruction](../-instruction/index.md), val automatic: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) : [PetNode](../-pet-node/index.md)

## Constructors

| | |
|---|---|
| [Effect](-effect.md) | [jvm]<br>fun [Effect](-effect.md)(trigger: [Effect.Trigger](-trigger/index.md), instruction: [Instruction](../-instruction/index.md), automatic: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |
| [Trigger](-trigger/index.md) | [jvm]<br>sealed class [Trigger](-trigger/index.md) : [PetNode](../-pet-node/index.md) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [automatic](automatic.md) | [jvm]<br>val [automatic](automatic.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [instruction](instruction.md) | [jvm]<br>val [instruction](instruction.md): [Instruction](../-instruction/index.md) |
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [trigger](trigger.md) | [jvm]<br>val [trigger](trigger.md): [Effect.Trigger](-trigger/index.md) |
