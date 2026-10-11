package dev.martianzoo.tfm.text.vastitasmap

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber

internal val vastitasEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("TileInLargestGroup") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "tile in your largest connected group of tiles",
                                "tiles in your largest connected group of tiles",
                            ),
                        unqualifiedSuffix = "",
                    )
            )
    )
