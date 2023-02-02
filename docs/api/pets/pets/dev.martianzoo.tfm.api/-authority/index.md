//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[Authority](index.md)

# Authority

[jvm]\
abstract class [Authority](index.md)

A source of data about Terraforming Mars components. This project provides one, called `Canon`, containing only officially published materials.

## Constructors

| | |
|---|---|
| [Authority](-authority.md) | [jvm]<br>fun [Authority](-authority.md)() |

## Types

| Name | Summary |
|---|---|
| [Empty](-empty/index.md) | [jvm]<br>open class [Empty](-empty/index.md) : [Authority](index.md)<br>An authority providing nothing; intended for tests. Subclass it to supply any needed declarations and definitions. |
| [Minimal](-minimal/index.md) | [jvm]<br>open class [Minimal](-minimal/index.md) : [Authority.Empty](-empty/index.md)<br>An authority providing almost nothing, just a single (empty) Mars map, which is in some code paths required. |

## Functions

| Name | Summary |
|---|---|
| [action](action.md) | [jvm]<br>fun [action](action.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)): [StandardActionDefinition](../../dev.martianzoo.tfm.data/-standard-action-definition/index.md) |
| [card](card.md) | [jvm]<br>fun [card](card.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)): [CardDefinition](../../dev.martianzoo.tfm.data/-card-definition/index.md)<br>Returns the card definition having the full name [name](card.md). If there are multiple, one must be marked as `replaces` the other. (TODO) |
| [classDeclaration](class-declaration.md) | [jvm]<br>fun [classDeclaration](class-declaration.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)): [ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)<br>Returns the class declaration having the full name [name](class-declaration.md). |
| [customInstruction](custom-instruction.md) | [jvm]<br>fun [customInstruction](custom-instruction.md)(functionName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [CustomInstruction](../-custom-instruction/index.md) |
| [marsMap](mars-map.md) | [jvm]<br>fun [marsMap](mars-map.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)): [MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md) |
| [milestone](milestone.md) | [jvm]<br>fun [milestone](milestone.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)): [MilestoneDefinition](../../dev.martianzoo.tfm.data/-milestone-definition/index.md) |

## Properties

| Name | Summary |
|---|---|
| [allBundles](all-bundles.md) | [jvm]<br>open val [allBundles](all-bundles.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [allClassDeclarations](all-class-declarations.md) | [jvm]<br>val [allClassDeclarations](all-class-declarations.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), [ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)&gt;<br>Every class declarations this authority knows about, including explicit ones and those converted from [Definition](../../dev.martianzoo.tfm.data/-definition/index.md)s. |
| [allClassNames](all-class-names.md) | [jvm]<br>val [allClassNames](all-class-names.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)&gt; |
| [allDefinitions](all-definitions.md) | [jvm]<br>val [allDefinitions](all-definitions.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Definition](../../dev.martianzoo.tfm.data/-definition/index.md)&gt;<br>Everything implementing [Definition](../../dev.martianzoo.tfm.data/-definition/index.md) this authority knows about. |
| [cardDefinitions](card-definitions.md) | [jvm]<br>abstract val [cardDefinitions](card-definitions.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[CardDefinition](../../dev.martianzoo.tfm.data/-card-definition/index.md)&gt;<br>Every card definition this authority knows about. |
| [cardsByClassName](cards-by-class-name.md) | [jvm]<br>val [cardsByClassName](cards-by-class-name.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), [CardDefinition](../../dev.martianzoo.tfm.data/-card-definition/index.md)&gt;<br>A map from [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) to [CardDefinition](../../dev.martianzoo.tfm.data/-card-definition/index.md), containing all cards known to this authority. |
| [customInstructions](custom-instructions.md) | [jvm]<br>abstract val [customInstructions](custom-instructions.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[CustomInstruction](../-custom-instruction/index.md)&gt; |
| [explicitClassDeclarations](explicit-class-declarations.md) | [jvm]<br>abstract val [explicitClassDeclarations](explicit-class-declarations.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)&gt;<br>All class declarations that were provided directly in source form (i.e., `CLASS Foo...`) as opposed to being converted from [Definition](../../dev.martianzoo.tfm.data/-definition/index.md) objects. |
| [marsMapDefinitions](mars-map-definitions.md) | [jvm]<br>abstract val [marsMapDefinitions](mars-map-definitions.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md)&gt; |
| [milestoneDefinitions](milestone-definitions.md) | [jvm]<br>abstract val [milestoneDefinitions](milestone-definitions.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[MilestoneDefinition](../../dev.martianzoo.tfm.data/-milestone-definition/index.md)&gt; |
| [milestonesByClassName](milestones-by-class-name.md) | [jvm]<br>val [milestonesByClassName](milestones-by-class-name.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), [MilestoneDefinition](../../dev.martianzoo.tfm.data/-milestone-definition/index.md)&gt; |
| [standardActionDefinitions](standard-action-definitions.md) | [jvm]<br>abstract val [standardActionDefinitions](standard-action-definitions.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[StandardActionDefinition](../../dev.martianzoo.tfm.data/-standard-action-definition/index.md)&gt; |

## Inheritors

| Name |
|---|
| [Empty](-empty/index.md) |
