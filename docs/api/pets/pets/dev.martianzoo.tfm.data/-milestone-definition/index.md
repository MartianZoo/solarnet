//[pets](../../../index.md)/[dev.martianzoo.tfm.data](../index.md)/[MilestoneDefinition](index.md)

# MilestoneDefinition

[jvm]\
data class [MilestoneDefinition](index.md)(val id: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val replaces: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, @Json(name = &quot;requirement&quot;)val requirementText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Definition](../-definition/index.md)

## Constructors

| | |
|---|---|
| [MilestoneDefinition](-milestone-definition.md) | [jvm]<br>fun [MilestoneDefinition](-milestone-definition.md)(id: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), replaces: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, @Json(name = &quot;requirement&quot;)requirementText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [asClassDeclaration](as-class-declaration.md) | [jvm]<br>open override val [asClassDeclaration](as-class-declaration.md): [ClassDeclaration](../-class-declaration/index.md) |
| [bundle](bundle.md) | [jvm]<br>open override val [bundle](bundle.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [id](id.md) | [jvm]<br>open override val [id](id.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [name](name.md) | [jvm]<br>open override val [name](name.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [replaces](replaces.md) | [jvm]<br>val [replaces](replaces.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [requirement](requirement.md) | [jvm]<br>val [requirement](requirement.md): [Requirement](../../dev.martianzoo.tfm.pets.ast/-requirement/index.md) |
| [requirementText](requirement-text.md) | [jvm]<br>val [requirementText](requirement-text.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
