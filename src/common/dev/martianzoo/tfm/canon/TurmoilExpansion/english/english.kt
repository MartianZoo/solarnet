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
        cn("Ruling") to
            ComponentDescriber(
                requirementCondition = Condition.ArgumentState(rulingParty, "is ruling"),
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
        cn("PartyRequirement") to
            ComponentDescriber(
                requirementKind = "party",
            ),
        cn("PartyDelegate") to
            ComponentDescriber(
                changeFrame =
                    Frame.CountedProcedure(
                        "place",
                        delegateNoun,
                        singularDeterminer = Determiner.INDEFINITE,
                    ),
                requirementCondition =
                    Condition.OwnedCount(
                        delegateNoun,
                        qualifierDependency = party,
                        qualifierRelation = "in",
                        unboundQualifier = "any party",
                        ownerDependency = owner,
                        ownerAdjectives = mapOf(cn("Neutral") to "neutral"),
                        differences =
                            mapOf(
                                cn("PartyLeader") to
                                    counted("non-leader delegate", "non-leader delegates")
                            ),
                    ),
            ),
        cn("PartyLeader") to
            ComponentDescriber(
                triggerFrame = Trigger.Named("become", "a party leader"),
                requirementCondition =
                    Condition.OwnedCount(
                        counted("party", "parties"),
                        ownerDependency = owner,
                        ownerAdjectives = mapOf(cn("Neutral") to "neutral"),
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
                        ownerAdjectives = mapOf(cn("Neutral") to "neutral"),
                        singleOwnerState = "are chairman",
                    ),
            ),
        cn("Influence") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Fixed("influence"),
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
