//[pets](../../../index.md)/[dev.martianzoo.tfm.pets.ast](../index.md)/[Instruction](index.md)

# Instruction

[jvm]\
sealed class [Instruction](index.md) : [PetNode](../-pet-node/index.md)

## Types

| Name | Summary |
|---|---|
| [AbstractInstructionException](-abstract-instruction-exception/index.md) | [jvm]<br>class [AbstractInstructionException](-abstract-instruction-exception/index.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html) |
| [Change](-change/index.md) | [jvm]<br>sealed class [Change](-change/index.md) : [Instruction](index.md) |
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) : [PetParser](../../dev.martianzoo.tfm.pets/-pet-parser/index.md) |
| [Custom](-custom/index.md) | [jvm]<br>data class [Custom](-custom/index.md)(val functionName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../-type-expr/index.md)&gt;) : [Instruction](index.md) |
| [Gain](-gain/index.md) | [jvm]<br>data class [Gain](-gain/index.md)(val sat: [ScalarAndType](../-scalar-and-type/index.md), val intensity: [Instruction.Intensity](-intensity/index.md)? = null) : [Instruction.Change](-change/index.md) |
| [Gated](-gated/index.md) | [jvm]<br>data class [Gated](-gated/index.md)(val gate: [Requirement](../-requirement/index.md), val instruction: [Instruction](index.md)) : [Instruction](index.md) |
| [Intensity](-intensity/index.md) | [jvm]<br>enum [Intensity](-intensity/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Instruction.Intensity](-intensity/index.md)&gt; |
| [Multi](-multi/index.md) | [jvm]<br>data class [Multi](-multi/index.md)(val instructions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Instruction](index.md)&gt;) : [Instruction](index.md) |
| [Or](-or/index.md) | [jvm]<br>data class [Or](-or/index.md)(val instructions: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Instruction](index.md)&gt;) : [Instruction](index.md) |
| [Per](-per/index.md) | [jvm]<br>data class [Per](-per/index.md)(val instruction: [Instruction](index.md), val sat: [ScalarAndType](../-scalar-and-type/index.md)) : [Instruction](index.md) |
| [Remove](-remove/index.md) | [jvm]<br>data class [Remove](-remove/index.md)(val sat: [ScalarAndType](../-scalar-and-type/index.md), val intensity: [Instruction.Intensity](-intensity/index.md)? = null) : [Instruction.Change](-change/index.md) |
| [Then](-then/index.md) | [jvm]<br>data class [Then](-then/index.md)(val instructions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Instruction](index.md)&gt;) : [Instruction](index.md) |
| [Transform](-transform/index.md) | [jvm]<br>data class [Transform](-transform/index.md)(val instruction: [Instruction](index.md), val transform: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Instruction](index.md), [PetNode.GenericTransform](../-pet-node/-generic-transform/index.md)&lt;[Instruction](index.md)&gt; |
| [Transmute](-transmute/index.md) | [jvm]<br>data class [Transmute](-transmute/index.md)(val from: [From](../-from/index.md), val scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null, val intensity: [Instruction.Intensity](-intensity/index.md)? = null) : [Instruction.Change](-change/index.md) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../-pet-node/group-part-if-needed.md)(part: [PetNode](../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](../-pet-node/precedence.md) | [jvm]<br>open fun [precedence](../-pet-node/precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](../-pet-node/should-group-inside.md) | [jvm]<br>open fun [shouldGroupInside](../-pet-node/should-group-inside.md)(container: [PetNode](../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [visitChildren](../-pet-node/visit-children.md) | [jvm]<br>abstract fun [visitChildren](../-pet-node/visit-children.md)(visitor: [PetVisitor](../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [kind](kind.md) | [jvm]<br>open override val [kind](kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Inheritors

| Name |
|---|
| [Change](-change/index.md) |
| [Per](-per/index.md) |
| [Gated](-gated/index.md) |
| [Custom](-custom/index.md) |
| [Then](-then/index.md) |
| [Or](-or/index.md) |
| [Multi](-multi/index.md) |
| [Transform](-transform/index.md) |
