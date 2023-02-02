//[pets](../../../index.md)/[dev.martianzoo.tfm.data](../index.md)/[MarsMapDefinition](index.md)

# MarsMapDefinition

[jvm]\
data class [MarsMapDefinition](index.md)(val name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val areas: [Grid](../../dev.martianzoo.util/-grid/index.md)&lt;[MarsMapDefinition.AreaDefinition](-area-definition/index.md)&gt;) : [Definition](../-definition/index.md)

## Constructors

| | |
|---|---|
| [MarsMapDefinition](-mars-map-definition.md) | [jvm]<br>fun [MarsMapDefinition](-mars-map-definition.md)(name: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), areas: [Grid](../../dev.martianzoo.util/-grid/index.md)&lt;[MarsMapDefinition.AreaDefinition](-area-definition/index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [AreaDefinition](-area-definition/index.md) | [jvm]<br>data class [AreaDefinition](-area-definition/index.md)(val mapName: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val row: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val kind: [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bonusText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Definition](../-definition/index.md) |

## Properties

| Name | Summary |
|---|---|
| [areas](areas.md) | [jvm]<br>val [areas](areas.md): [Grid](../../dev.martianzoo.util/-grid/index.md)&lt;[MarsMapDefinition.AreaDefinition](-area-definition/index.md)&gt; |
| [asClassDeclaration](as-class-declaration.md) | [jvm]<br>open override val [asClassDeclaration](as-class-declaration.md): [ClassDeclaration](../-class-declaration/index.md) |
| [bundle](bundle.md) | [jvm]<br>open override val [bundle](bundle.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [id](id.md) | [jvm]<br>open override val [id](id.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [name](name.md) | [jvm]<br>open override val [name](name.md): [ClassName](../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
