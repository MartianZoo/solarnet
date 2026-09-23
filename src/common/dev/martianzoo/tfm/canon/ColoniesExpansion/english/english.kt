package dev.martianzoo.tfm.text.coloniesexpansion

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.tfm.text.Determiner

private val colonyNoun = ComponentDescriber.Noun.Counted("colony", "colonies")

internal val coloniesEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("ColonyTile") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        ComponentDescriber.Noun.Counted("colony tile", "colony tiles")
                    )
            ),
        cn("Colony") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        colonyNoun,
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = ComponentDescriber.Requirement.Bound.Count(colonyNoun),
                        maximum = ComponentDescriber.Requirement.Bound.Count(colonyNoun),
                        ownedCount = colonyNoun,
                    ),
            ),
        cn("GainColonyBonuses") to
            ComponentDescriber(changeFrame = Frame.Procedure("gain", "all your colony bonuses")),
        cn("AdvanceColonyTracks") to
            ComponentDescriber(
                changeFrame =
                    Frame.Scale(
                        subject = "all colony tile tracks",
                        increaseVerb = "increase",
                        decreaseVerb = "decrease",
                    )
            ),
        cn("ColonyTileSelection") to
            ComponentDescriber(changeFrame = Frame.Procedure("add", "1 colony tile")),
        cn("Camp") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("camp resource", "camp resources")
            ),
        cn("TradeFleet") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("Trade Fleet", "Trade Fleets"),
                changeFrame = Frame.Countable,
            ),
        cn("ColonyProduction") to
            ComponentDescriber(changeFrame = Frame.Scale("colony tile track")),
        cn("Trade") to ComponentDescriber(changeFrame = Frame.Procedure("trade")),
        cn("TradeAction") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference =
                            ComponentDescriber.ActionUse.Reference.Fixed(
                                "the Trade standard action"
                            ),
                        paymentDiscount =
                            ComponentDescriber.PaymentDiscount(
                                "use the Trade standard action",
                                categoryNoun =
                                    ComponentDescriber.Noun.Counted("resource", "resources"),
                            ),
                    )
            ),
    )
