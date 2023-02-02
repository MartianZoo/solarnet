//[pets](../../../index.md)/[dev.martianzoo.tfm.api](../index.md)/[GameSetup](index.md)

# GameSetup

[jvm]\
data class [GameSetup](index.md)(val authority: [Authority](../-authority/index.md), val bundles: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, val players: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

A specification of the starting conditions for game. This should determine exactly what to do to prepare the game up until the point of the first player decisions.

## Constructors

| | |
|---|---|
| [GameSetup](-game-setup.md) | [jvm]<br>fun [GameSetup](-game-setup.md)(authority: [Authority](../-authority/index.md), bundles: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), players: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |
| [GameSetup](-game-setup.md) | [jvm]<br>fun [GameSetup](-game-setup.md)(authority: [Authority](../-authority/index.md), bundles: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, players: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [allDefinitions](all-definitions.md) | [jvm]<br>fun [allDefinitions](all-definitions.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Definition](../../dev.martianzoo.tfm.data/-definition/index.md)&gt;<br>All [Definition](../../dev.martianzoo.tfm.data/-definition/index.md) objects to use in this game. |

## Properties

| Name | Summary |
|---|---|
| [authority](authority.md) | [jvm]<br>val [authority](authority.md): [Authority](../-authority/index.md)<br>Where to pull class declarations, card definitions, etc. from. |
| [bundles](bundles.md) | [jvm]<br>val [bundles](bundles.md): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Which bundles of cards/milestones/maps/etc. to include. For example the officially published bundles are `"B"` for base `"R"` for corporate era, `"V"` for venus next, etc. This list must include `B` and exactly one map (the canon maps are `"M"` for the base map, `"H"` for Hellas, and `"E"` for Elysium). |
| [map](map.md) | [jvm]<br>val [map](map.md): [MarsMapDefinition](../../dev.martianzoo.tfm.data/-mars-map-definition/index.md)<br>The map to use for this game. |
| [players](players.md) | [jvm]<br>val [players](players.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Number of players. Only 2-5 are supported for now. Solo mode will take quite some work. |
