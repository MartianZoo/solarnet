//[pets](../../../../index.md)/[dev.martianzoo.tfm.data](../../index.md)/[MarsMapDefinition](../index.md)/[AreaDefinition](index.md)

# AreaDefinition

[jvm]\
data class [AreaDefinition](index.md)(val mapName: [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val row: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val kind: [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), val bonusText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Definition](../../-definition/index.md)

## Constructors

| | |
|---|---|
| [AreaDefinition](-area-definition.md) | [jvm]<br>fun [AreaDefinition](-area-definition.md)(mapName: [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), bundle: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), row: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), kind: [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md), bonusText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [asClassDeclaration](as-class-declaration.md) | [jvm]<br>open override val [asClassDeclaration](as-class-declaration.md): [ClassDeclaration](../../-class-declaration/index.md) |
| [bonus](bonus.md) | [jvm]<br>val [bonus](bonus.md): [Instruction](../../../dev.martianzoo.tfm.pets.ast/-instruction/index.md)? |
| [bonusText](bonus-text.md) | [jvm]<br>val [bonusText](bonus-text.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>The pets instruction for this map area's bonus. |
| [bundle](bundle.md) | [jvm]<br>open override val [bundle](bundle.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [code](code.md) | [jvm]<br>val [code](code.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>A short code like `LPP` summarizing this map area. |
| [column](column.md) | [jvm]<br>val [column](column.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The column number of this area. Columns are slanted like `/`, and the leftmost column is numbered `1`. |
| [id](id.md) | [jvm]<br>open override val [id](id.md): [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [kind](kind.md) | [jvm]<br>val [kind](kind.md): [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)<br>The kind of area; standard kinds include &quot;LandArea&quot;, &quot;WaterArea&quot;, &quot;VolcanicArea&quot;, and &quot;NoctisArea&quot;. |
| [mapName](map-name.md) | [jvm]<br>val [mapName](map-name.md): [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md)<br>Mame of the MarsMapDefinition this area belongs to (e.g &quot;Tharsis&quot;). |
| [name](name.md) | [jvm]<br>open override val [name](name.md): [ClassName](../../../dev.martianzoo.tfm.pets.ast/-class-name/index.md) |
| [row](row.md) | [jvm]<br>val [row](row.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The row number of this area; the top row is row `1`. |
