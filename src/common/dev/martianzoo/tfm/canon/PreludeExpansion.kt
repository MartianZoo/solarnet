package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.CARDS

internal val preludeExpansionBundle: StandardFormBundle =
    StandardFormBundle(
        "PreludeExpansion",
        moduleContentSelections =
            mapOf(
                cn("PreludeExpansion") to emptySet(),
                cn("Prelude1CardPack") to
                    setOf(BundleContentSelection(cn("PreludeExpansion"), setOf(CARDS))),
            ),
    )
