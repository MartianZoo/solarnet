//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Effect](../index.md)/[Trigger](index.md)

# Trigger

[jvm]\
sealed class [Trigger](index.md) : [PetNode](../../-pet-node/index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |
| [OnGainOf](-on-gain-of/index.md) | [jvm]<br>data class [OnGainOf](-on-gain-of/index.md) : [Effect.Trigger](index.md) |
| [OnRemoveOf](-on-remove-of/index.md) | [jvm]<br>data class [OnRemoveOf](-on-remove-of/index.md) : [Effect.Trigger](index.md) |
| [Transform](-transform/index.md) | [jvm]<br>data class [Transform](-transform/index.md)(val trigger: [Effect.Trigger](index.md), val transform: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Effect.Trigger](index.md), [PetNode.GenericTransform](../../-pet-node/-generic-transform/index.md)&lt;[Effect.Trigger](index.md)&gt; |
| [WhenGain](-when-gain/index.md) | [jvm]<br>object [WhenGain](-when-gain/index.md) : [Effect.Trigger](index.md) |
| [WhenRemove](-when-remove/index.md) | [jvm]<br>object [WhenRemove](-when-remove/index.md) : [Effect.Trigger](index.md) |

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
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Inheritors

| Name |
|---|
| [WhenGain](-when-gain/index.md) |
| [WhenRemove](-when-remove/index.md) |
| [OnGainOf](-on-gain-of/index.md) |
| [OnRemoveOf](-on-remove-of/index.md) |
| [Transform](-transform/index.md) |
