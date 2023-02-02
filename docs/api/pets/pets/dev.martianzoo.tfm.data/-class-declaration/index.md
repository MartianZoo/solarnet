//[pets](../../../index.md)/[dev.martianzoo.tfm.data](../index.md)/[ClassDeclaration](index.md)

# ClassDeclaration

[jvm]\
data class [ClassDeclaration](index.md)(val name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val id: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) = name, val abstract: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, val dependencies: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassDeclaration.DependencyDeclaration](-dependency-declaration/index.md)&gt; = listOf(), val supertypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; = setOf(), val topInvariant: [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)? = null, val otherInvariants: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)&gt; = setOf(), val effectsRaw: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Effect](../../dev.martianzoo.tfm.pets.ast/-effect/index.md)&gt; = setOf(), val defaultsDeclaration: [ClassDeclaration.DefaultsDeclaration](-defaults-declaration/index.md) = DefaultsDeclaration(), val extraNodes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; = setOf())

The declaration of a component class, such as GreeneryTile. Models the declaration textually as it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).

## Constructors

| | |
|---|---|
| [ClassDeclaration](-class-declaration.md) | [jvm]<br>fun [ClassDeclaration](-class-declaration.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), id: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) = name, abstract: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, dependencies: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassDeclaration.DependencyDeclaration](-dependency-declaration/index.md)&gt; = listOf(), supertypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; = setOf(), topInvariant: [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)? = null, otherInvariants: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)&gt; = setOf(), effectsRaw: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Effect](../../dev.martianzoo.tfm.pets.ast/-effect/index.md)&gt; = setOf(), defaultsDeclaration: [ClassDeclaration.DefaultsDeclaration](-defaults-declaration/index.md) = DefaultsDeclaration(), extraNodes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; = setOf()) |

## Types

| Name | Summary |
|---|---|
| [DefaultsDeclaration](-defaults-declaration/index.md) | [jvm]<br>data class [DefaultsDeclaration](-defaults-declaration/index.md)(val universalSpecs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; = listOf(), val gainOnlySpecs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; = listOf(), val gainIntensity: [Instruction.Intensity](../../dev.martianzoo.tfm.pets.ast/-instruction/-intensity/index.md)? = null) |
| [DependencyDeclaration](-dependency-declaration/index.md) | [jvm]<br>data class [DependencyDeclaration](-dependency-declaration/index.md)(val typeExpr: [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [validate](validate.md) | [jvm]<br>fun [validate](validate.md)() |

## Properties

| Name | Summary |
|---|---|
| [abstract](abstract.md) | [jvm]<br>val [abstract](abstract.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [allNodes](all-nodes.md) | [jvm]<br>val [allNodes](all-nodes.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; |
| [defaultsDeclaration](defaults-declaration.md) | [jvm]<br>val [defaultsDeclaration](defaults-declaration.md): [ClassDeclaration.DefaultsDeclaration](-defaults-declaration/index.md) |
| [dependencies](dependencies.md) | [jvm]<br>val [dependencies](dependencies.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassDeclaration.DependencyDeclaration](-dependency-declaration/index.md)&gt; |
| [effectsRaw](effects-raw.md) | [jvm]<br>val [effectsRaw](effects-raw.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Effect](../../dev.martianzoo.tfm.pets.ast/-effect/index.md)&gt; |
| [extraNodes](extra-nodes.md) | [jvm]<br>val [extraNodes](extra-nodes.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; |
| [id](id.md) | [jvm]<br>val [id](id.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [name](name.md) | [jvm]<br>val [name](name.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [otherInvariants](other-invariants.md) | [jvm]<br>val [otherInvariants](other-invariants.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)&gt; |
| [superclassNames](superclass-names.md) | [jvm]<br>val [superclassNames](superclass-names.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)&gt; |
| [supertypes](supertypes.md) | [jvm]<br>val [supertypes](supertypes.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)&gt; |
| [topInvariant](top-invariant.md) | [jvm]<br>val [topInvariant](top-invariant.md): [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md)? = null |
