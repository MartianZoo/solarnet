//[pets](../../../index.md)/[dev.martianzoo.tfm.data](../index.md)/[CardDefinition](index.md)/[idRaw](id-raw.md)

# idRaw

[jvm]\
val [idRaw](id-raw.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

This card's unique id string: its printed id if it has one; otherwise the one we made up. A number of id ranges, such as `"000"`-`"999"`, are reserved for canon (officially published) cards.

If a card (like Deimos Down) has multiple variants which are meant to never coexist, each variant still needs its own unique id. The `replacesId` property for the replacement card will identify the replaced card.

It is of course possible for non-canon cards to have colliding ids, which would prevent them from being used simultaneously.
