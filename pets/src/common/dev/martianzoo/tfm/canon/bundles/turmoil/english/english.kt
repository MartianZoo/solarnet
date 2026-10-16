package dev.martianzoo.tfm.text.turmoilexpansion

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.tfm.text.ComponentDescriber.RequirementCondition as Condition
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as Trigger
import dev.martianzoo.tfm.text.Determiner

private val owner = Key(OWNED, 0)
private val party = Key(cn("PartyDelegate"), 0)
private val rulingParty = Key(cn("PartyStatus"), 0)
private val delegateNoun = counted("delegate", "delegates")

internal val turmoilEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("Preservation") to
            ComponentDescriber(
                noun =
                    ComponentDescriber.Noun.Counted(
                        "preservation resource",
                        "preservation resources",
                    )
            ),
        cn("Ruling") to
            ComponentDescriber(
                requirementCondition = Condition.ArgumentState(rulingParty, "is ruling"),
                requirementKind = "party",
            ),
        cn("Party") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(counted("party", "parties"), relation = "in"),
                metricCount =
                    ComponentDescriber.MetricCount(
                        counted("party", "parties"),
                        unqualifiedSuffix = "",
                    ),
            ),
        cn("MarsFirst") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("Mars First")),
        cn("Scientists") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("the Scientists party")),
        cn("Unity") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("Unity")),
        cn("Greens") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("the Greens party")),
        cn("Reds") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("the Reds party")),
        cn("Kelvinists") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("the Kelvinists party")),
        cn("Neutral") to ComponentDescriber(ownerAdjective = "neutral"),
        cn("Delegate") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        delegateNoun,
                        unqualifiedSuffix = "",
                        forSubclasses = false,
                    ),
            ),
        cn("LobbyDelegate") to
            ComponentDescriber(countNoun = counted("lobby delegate", "lobby delegates")),
        cn("PartyDelegate") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        delegateNoun,
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirementCondition =
                    Condition.OwnedCount(
                        delegateNoun,
                        qualifierDependency = party,
                        qualifierRelation = "in",
                        unboundQualifier = "any party",
                        ownerDependency = owner,
                    ),
            ),
        cn("PartyFollower") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        counted("non-leader delegate", "non-leader delegates"),
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                countNoun = counted("non-leader delegate", "non-leader delegates"),
                requirementCondition =
                    Condition.OwnedCount(
                        counted("non-leader delegate", "non-leader delegates"),
                        qualifierDependency = party,
                        qualifierRelation = "in",
                        unboundQualifier = "any party",
                        ownerDependency = owner,
                    ),
            ),
        cn("PartyLeader") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        counted("party leader", "party leaders"),
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                triggerFrame = Trigger.Named("become", "a party leader"),
                countNoun = counted("party leadership", "party leaderships"),
                metricCount =
                    ComponentDescriber.MetricCount(
                        counted("party leadership", "party leaderships"),
                        unqualifiedSuffix = "",
                    ),
                requirementCondition =
                    Condition.OwnedCount(
                        counted("party", "parties"),
                        ownerDependency = owner,
                        ownerVerb = "lead",
                    ),
            ),
        cn("Chairman") to
            ComponentDescriber(
                changeFrame =
                    Frame.State(
                        enter = Frame.Procedure("move", "a reserve delegate to the chair"),
                        leave = Frame.Procedure("return", "the chairman to its owner's reserve"),
                        ownershipTransfers =
                            mapOf(
                                cn("Neutral") to
                                    Frame.Procedure(
                                        "replace",
                                        "the neutral chairman with one of your delegates",
                                    )
                            ),
                    ),
                requirementCondition =
                    Condition.OwnedCount(
                        counted("chairman", "chairmen"),
                        ownerDependency = owner,
                        singleOwnerState = "are chairman",
                    ),
            ),
        cn("Influence") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Fixed("influence"),
                countNoun = counted("influence", "influence"),
                numericSingularChange = true,
                changeFrame = Frame.Countable,
            ),
        cn("MeasureInfluence") to
            ComponentDescriber(
                triggerFrame = Trigger.Named("is counted", "influence", passive = true)
            ),
    )

private fun counted(
    singular: String,
    plural: String,
): ComponentDescriber.Noun.Counted = ComponentDescriber.Noun.Counted(singular, plural)
