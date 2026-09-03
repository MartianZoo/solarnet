package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.CARDS

internal val prelude2ExpansionBundle: StandardFormBundle =
    StandardFormBundle(
        "Prelude2Expansion",
        moduleContentSelections =
            mapOf(
                cn("Prelude2Expansion") to emptySet(),
                cn("Prelude2CardPack") to
                    setOf(BundleContentSelection(cn("Prelude2Expansion"), setOf(CARDS))),
            ),
    )
