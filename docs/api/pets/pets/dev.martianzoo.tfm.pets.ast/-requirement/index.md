//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[Requirement](index.md)

# Requirement

[jvm]\
sealed class [Requirement](index.md) : [PetNode](../-pet-node/index.md)

## Types

| Name | Summary |
|---|---|
| [And](-and/index.md) | [jvm]<br>data class [And](-and/index.md)(val requirements: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Requirement](index.md)&gt;) : [Requirement](index.md) |
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |
| [Exact](-exact/index.md) | [jvm]<br>data class [Exact](-exact/index.md)(val sat: [ScalarAndType](../-scalar-and-type/index.md)) : [Requirement](index.md) |
| [Max](-max/index.md) | [jvm]<br>data class [Max](-max/index.md)(val sat: [ScalarAndType](../-scalar-and-type/index.md)) : [Requirement](index.md) |
| [Min](-min/index.md) | [jvm]<br>data class [Min](-min/index.md)(val sat: [ScalarAndType](../-scalar-and-type/index.md)) : [Requirement](index.md) |
| [Or](-or/index.md) | [jvm]<br>data class [Or](-or/index.md)(val requirements: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Requirement](index.md)&gt;) : [Requirement](index.md) |
| [Transform](-transform/index.md) | [jvm]<br>data class [Transform](-transform/index.md)(val requirement: [Requirement](index.md), val transform: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Requirement](index.md), [PetNode.GenericTransform](../-pet-node/-generic-transform/index.md)&lt;[Requirement](index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [requiresThis](requires-this.md) | [jvm]<br>open fun [requiresThis](requires-this.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [visitChildren](../-pet-node/visit-children.md) | [jvm]<br>abstract fun [visitChildren](../-pet-node/visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Inheritors

| Name |
|---|
| [Min](-min/index.md) |
| [Max](-max/index.md) |
| [Exact](-exact/index.md) |
| [Or](-or/index.md) |
| [And](-and/index.md) |
| [Transform](-transform/index.md) |
