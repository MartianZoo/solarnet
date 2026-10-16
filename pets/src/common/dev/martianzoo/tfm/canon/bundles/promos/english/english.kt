package dev.martianzoo.tfm.text.promocardpack

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as Trigger
import dev.martianzoo.tfm.text.Determiner

internal val promoEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("ResourceWasAttacked") to
            ComponentDescriber(
                requirementCondition =
                    ComponentDescriber.RequirementCondition.ArgumentState(
                        Key(cn("ResourceWasAttacked"), 1),
                        "removed your resources this generation",
                    ),
                triggerFrame =
                    Trigger.Named(
                        "has their resources removed by another player",
                        "any player",
                        passive = true,
                    ),
            ),
        cn("ProductionWasAttacked") to
            ComponentDescriber(
                requirementCondition =
                    ComponentDescriber.RequirementCondition.ArgumentState(
                        Key(cn("ProductionWasAttacked"), 1),
                        "decreased your production this generation",
                    ),
                triggerFrame =
                    Trigger.Named(
                        "has their production decreased by another player",
                        "any player",
                        passive = true,
                    ),
            ),
        cn("Disease") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("disease resource", "disease resources")
            ),
        cn("Graphene") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("graphene resource", "graphene resources")
            ),
        cn("Hydroelectric") to
            ComponentDescriber(
                noun =
                    ComponentDescriber.Noun.Counted(
                        "hydroelectric resource",
                        "hydroelectric resources",
                    )
            ),
        cn("NomadsMarker") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        ComponentDescriber.Noun.Counted("nomads marker", "nomads markers"),
                    )
            ),
        cn("CopyPrelude") to
            ComponentDescriber(
                changeFrame = Frame.Procedure("copy", "your other Prelude's direct effect")
            ),
        cn("ChooseOceanArea") to
            ComponentDescriber(changeFrame = Frame.Procedure("choose", "an ocean area")),
        cn("Cathedral") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        ComponentDescriber.Noun.Counted("cathedral", "cathedrals"),
                        unqualifiedSuffix = "in play",
                    )
            ),
    )
