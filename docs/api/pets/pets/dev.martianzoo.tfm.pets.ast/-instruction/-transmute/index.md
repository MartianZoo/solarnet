//[pets](../../../../index.md)/[dev.martianzoo.tfm.pets.ast](../../index.md)/[Instruction](../index.md)/[Transmute](index.md)

# Transmute

[jvm]\
data class [Transmute](index.md)(val from: [From](../../-from/index.md), val scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null, val intensity: [Instruction.Intensity](../-intensity/index.md)? = null) : [Instruction.Change](../-change/index.md)

## Constructors

| | |
|---|---|
| [Transmute](-transmute.md) | [jvm]<br>fun [Transmute](-transmute.md)(from: [From](../../-from/index.md), scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null, intensity: [Instruction.Intensity](../-intensity/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md) | [jvm]<br>fun [groupPartIfNeeded](../../-pet-node/group-part-if-needed.md)(part: [PetNode](../../-pet-node/index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [precedence](precedence.md) | [jvm]<br>open override fun [precedence](precedence.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [shouldGroupInside](should-group-inside.md) | [jvm]<br>open override fun [shouldGroupInside](should-group-inside.md)(container: [PetNode](../../-pet-node/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [visitChildren](visit-children.md) | [jvm]<br>open override fun [visitChildren](visit-children.md)(visitor: [PetVisitor](../../../dev.martianzoo.tfm.pets/-pet-visitor/index.md))<br>Invokes visitor.visit for each direct child node of this [PetNode](../../-pet-node/index.md). |

## Properties

| Name | Summary |
|---|---|
| [count](count.md) | [jvm]<br>open override val [count](count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [from](from.md) | [jvm]<br>val [from](from.md): [From](../../-from/index.md) |
| [gaining](gaining.md) | [jvm]<br>open override val [gaining](gaining.md): [TypeExpr](../../-type-expr/index.md) |
| [intensity](intensity.md) | [jvm]<br>open override val [intensity](intensity.md): [Instruction.Intensity](../-intensity/index.md)? = null |
| [kind](../kind.md) | [jvm]<br>open override val [kind](../kind.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [removing](removing.md) | [jvm]<br>open override val [removing](removing.md): [TypeExpr](../../-type-expr/index.md) |
| [scalar](scalar.md) | [jvm]<br>val [scalar](scalar.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null |
