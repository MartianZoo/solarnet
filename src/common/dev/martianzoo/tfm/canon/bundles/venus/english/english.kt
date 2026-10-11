package dev.martianzoo.tfm.text.venusnextexpansion

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame

internal val venusNextEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("VenusStep") to
            ComponentDescriber(
                changeFrame = Frame.Scale("Venus"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            ComponentDescriber.Requirement.Bound.Threshold(
                                "Venus",
                                ComponentDescriber.Requirement.Value.DOUBLE_PERCENT,
                            ),
                        maximum =
                            ComponentDescriber.Requirement.Bound.Threshold(
                                "Venus",
                                ComponentDescriber.Requirement.Value.DOUBLE_PERCENT,
                            ),
                    ),
            )
    )
