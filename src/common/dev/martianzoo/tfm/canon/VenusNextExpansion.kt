package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.AWARDS
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.CARDS
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.MILESTONES

internal val venusNextExpansionBundle: StandardFormBundle =
    StandardFormBundle(
        "VenusNextExpansion",
        moduleContentSelections =
            mapOf(
                cn("VenusNextExpansion") to
                    setOf(
                        BundleContentSelection(
                            cn("VenusNextExpansion"),
                            setOf(CARDS, MILESTONES, AWARDS),
                        )
                    )
            ),
    )

/** Namespace for Venus Next-specific implementations. */
private object VenusNextExpansion
