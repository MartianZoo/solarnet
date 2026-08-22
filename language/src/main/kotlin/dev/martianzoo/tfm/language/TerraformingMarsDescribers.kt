package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.language.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.types.Class

private typealias RequirementCount = ComponentDescriber.Requirement.CountSyntax

/** Terraforming Mars component descriptions supplied to the structural English renderer. */
internal object TerraformingMarsDescribers {
  internal val descriptions: Map<Class, ComponentDescriber> by lazy {
    Canon.classTable.allClasses().associateWith { declarations[it] ?: ComponentDescriber() }
  }

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    uniqueDeclarations(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("HasRaisedTr") to
            ComponentDescriber(presenceCondition = "your terraform rating has been raised"),
        klass("StandardResource") to ComponentDescriber(changeFrame = Frame.Countable),
        klass("Metal") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("titanium or steel")),
        klass("Steel") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("Titanium") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("Megacredit") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("ProjectCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("card", "cards"),
                changeFrame = Frame.Deck,
            ),
        klass("PreludeCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("prelude card", "prelude cards"),
                changeFrame = Frame.Deck,
            ),
        klass("PlayedEvent") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun = ComponentDescriber.Noun.Counted("card", "cards"),
                        anyoneSuffix = "in all players' event piles",
                    )
            ),
        klass("GlobalParameter") to ComponentDescriber(requirementKind = "global parameter"),
        klass("CardResource") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("resource", "resources"),
                changeFrame = Frame.Held(suffixed = true),
                distinctKinds =
                    ComponentDescriber.Noun.Counted(
                        "different type of card resource",
                        "different types of card resources",
                    ),
            ),
        klass("CardFront") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Fixed("card"),
                countNoun = ComponentDescriber.Noun.Counted("card", "cards"),
                cardResourceHolder = ComponentDescriber.Noun.Counted("card", "cards"),
                playedCard =
                    ComponentDescriber.PlayedCard(
                        minimumProperties =
                            mapOf(
                                "cost" to
                                    ComponentDescriber.MinimumProperty.Threshold(
                                        "printed cost",
                                        "M€",
                                    ),
                                "requirement" to
                                    ComponentDescriber.MinimumProperty.Presence("requirement"),
                            )
                    ),
            ),
        klass("EventCard") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("MarsArea") to
            ComponentDescriber(
                metricLocation = "on Mars",
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Counted("area", "areas"),
                        forSubclasses = false,
                    ),
            ),
        klass("RemoteArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("reserved area outside Mars"),
                        article = "the",
                    ),
            ),
        klass("WaterArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("area reserved for ocean"),
                        article = "an",
                    ),
            ),
        klass("NoctisArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("reserved area"),
                        article = "the",
                    ),
            ),
        klass("VolcanicArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "volcanic area",
                                "volcanic areas",
                            ),
                        article = "a",
                    ),
            ),
        klass("LandArea") to
            ComponentDescriber(
                placementSite = ComponentDescriber.PlacementSite(ComponentDescriber.Noun.ClassName)
            ),
        klass("Neighbor") to
            ComponentDescriber(
                spatialRelation =
                    ComponentDescriber.SpatialRelation(
                        "adjacent to",
                        ComponentDescriber.Noun.Counted("other tile", "other tiles"),
                    )
            ),
        klass("Adjacency") to
            ComponentDescriber(
                spatialRelation =
                    ComponentDescriber.SpatialRelation(
                        phrase = "adjacent to",
                        countedPair = true,
                        eventNoun = "adjacency",
                    )
            ),
        klass("MapBonus") to
            ComponentDescriber(
                placementBonus =
                    ComponentDescriber.PlacementBonus(
                        ComponentDescriber.Noun.Counted("placement bonus", "placement bonuses")
                    )
            ),
        klass("Tile") to
            ComponentDescriber(
                changeFrame = Frame.Positioned("a", "tile", "tiles"),
            ),
        klass("OwnedTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        "a",
                        "tile",
                        "tiles",
                        unqualifiedMetricOwner = ComponentDescriber.MetricOwner.YOU,
                    )
            ),
        klass("SpecialTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        article = "this",
                        singular = "tile",
                        plural = "tiles",
                        allowsMultiple = false,
                    ),
            ),
        klass("Animal") to ComponentDescriber(changeFrame = Frame.Held(suffixed = false)),
        klass("Asteroid") to ComponentDescriber(changeFrame = Frame.Held(suffixed = false)),
        klass("Floater") to ComponentDescriber(changeFrame = Frame.Held(suffixed = false)),
        klass("Microbe") to ComponentDescriber(changeFrame = Frame.Held(suffixed = false)),
        klass("Tag") to
            ComponentDescriber(
                countNoun = ComponentDescriber.Noun.Counted("tag", "tags"),
                distinctKinds = ComponentDescriber.Noun.Counted("different tag", "different tags"),
            ),
        klass("Resource") to
            ComponentDescriber(
                distinctKinds =
                    ComponentDescriber.Noun.Counted(
                        "different type of resource",
                        "different types of resources",
                    )
            ),
        klass("BioTag") to ComponentDescriber(playedTagPhrase = "a bio tag"),
        klass("AnimalTag") to ComponentDescriber(playedTagPhrase = "an animal tag"),
        klass("PlantTag") to ComponentDescriber(playedTagPhrase = "a plant tag"),
        klass("MicrobeTag") to ComponentDescriber(playedTagPhrase = "a microbe tag"),
        klass("OxygenStep") to
            ComponentDescriber(
                changeFrame = Frame.Scale("oxygen"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "oxygen",
                                ComponentDescriber.Requirement.Value.PERCENT,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .REQUIRES_VALUE_SUBJECT,
                            ),
                        maximum =
                            threshold(
                                "Oxygen",
                                ComponentDescriber.Requirement.Value.PERCENT,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .SUBJECT_MUST_BE_VALUE_OR_LESS,
                            ),
                    ),
            ),
        klass("TemperatureStep") to
            ComponentDescriber(
                changeFrame = Frame.Scale("temperature"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "temperature",
                                ComponentDescriber.Requirement.Value.TEMPERATURE,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .REQUIRES_VALUE_OR_WARMER,
                            ),
                        maximum =
                            threshold(
                                "Temperature",
                                ComponentDescriber.Requirement.Value.TEMPERATURE,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .SUBJECT_MUST_BE_VALUE_OR_COLDER,
                            ),
                    ),
            ),
        klass("VenusStep") to
            ComponentDescriber(
                changeFrame = Frame.Scale("Venus"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "Venus",
                                ComponentDescriber.Requirement.Value.DOUBLE_PERCENT,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .REQUIRES_SUBJECT_VALUE,
                            ),
                        maximum =
                            threshold(
                                "Venus",
                                ComponentDescriber.Requirement.Value.DOUBLE_PERCENT,
                                ComponentDescriber.Requirement.ThresholdSyntax
                                    .SUBJECT_MUST_BE_VALUE_OR_LESS,
                            ),
                    ),
            ),
        klass("TerraformRating") to
            ComponentDescriber(
                changeFrame = Frame.Scale("your terraform rating"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "a terraform rating",
                                syntax =
                                    ComponentDescriber.Requirement.ThresholdSyntax
                                        .REQUIRES_HAVE_SUBJECT_OF_VALUE_OR_MORE,
                            )
                    ),
            ),
        klass("OceanTile") to
            ComponentDescriber(
                changeFrame = Frame.Positioned("an", "ocean tile", "ocean tiles"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            count("ocean tile", "ocean tiles", RequirementCount.REQUIRES_COUNT),
                        maximum =
                            count(
                                "ocean tile",
                                "ocean tiles",
                                RequirementCount.THERE_MUST_BE_COUNT_OR_FEWER,
                            ),
                    ),
            ),
        klass("GreeneryTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        "a",
                        "greenery tile",
                        "greenery tiles",
                        consequence = "and raise oxygen 1 step",
                        allowsMultiple = false,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            count(
                                "greenery tile",
                                "greenery tiles",
                                RequirementCount.REQUIRES_OWNED_COUNT,
                            )
                    ),
            ),
        klass("CityTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        "a",
                        "city tile",
                        "city tiles",
                        anyoneMetricOwner = ComponentDescriber.MetricOwner.ANY_PLAYER,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            count(
                                "city tile",
                                "city tiles",
                                RequirementCount.REQUIRES_OWNED_COUNT,
                                RequirementCount.REQUIRES_COUNT,
                            ),
                        ownedCount = ComponentDescriber.Noun.Counted("city tile", "city tiles"),
                    ),
            ),
        klass("Colony") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        "a",
                        "colony",
                        "colonies",
                        unqualifiedMetricOwner = ComponentDescriber.MetricOwner.YOU,
                        anyoneMetricOwner = ComponentDescriber.MetricOwner.ANY_PLAYER,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = count("colony", "colonies", RequirementCount.REQUIRES_COUNT),
                        maximum =
                            count(
                                "colony",
                                "colonies",
                                RequirementCount.YOU_MUST_HAVE_NO_MORE_THAN_COUNT,
                            ),
                        ownedCount = ComponentDescriber.Noun.Counted("colony", "colonies"),
                    ),
            ),
        klass("BuyCard") to
            ComponentDescriber(
                purchase =
                    ComponentDescriber.Purchase(
                        noun = ComponentDescriber.Noun.Counted("card", "cards")
                    ),
            ),
        klass("CopyPrelude") to
            ComponentDescriber(
                changeFrame = Frame.Procedure("copy", "your other Prelude's direct effect")
            ),
        klass("GiveColonyBonuses") to
            ComponentDescriber(changeFrame = Frame.Procedure("gain", "all your colony bonuses")),
        klass("Mandate") to ComponentDescriber(changeFrame = Frame.Wrapper("as your first action")),
        klass("Award") to
            ComponentDescriber(changeFrame = Frame.Procedure("fund", "an award for free")),
        klass("ReserveTradeFleet") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("Trade Fleet", "Trade Fleets"),
                changeFrame = Frame.Countable,
            ),
        klass("LowestProduction") to
            ComponentDescriber(productionSelection = "one of your lowest productions"),
        klass("ColonyProduction") to
            ComponentDescriber(changeFrame = Frame.Scale("colony tile track")),
        klass("Trade") to ComponentDescriber(changeFrame = Frame.Procedure("trade")),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("Die") to ComponentDescriber(deadEndSignal = true),
        klass("PlayCard") to
            ComponentDescriber(
                changeFrame = Frame.Play,
                playTrigger = ComponentDescriber.PlayTrigger.CARD,
            ),
        klass("PlayTag") to ComponentDescriber(playTrigger = ComponentDescriber.PlayTrigger.TAG),
        klass("UseAction") to ComponentDescriber(usedActionTrigger = true),
        klass("UseAction1") to ComponentDescriber(actionNumber = 1),
        klass("StandardProject") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        "a standard project",
                        minimumProperties =
                            mapOf(
                                "cost" to
                                    ComponentDescriber.MinimumProperty.Threshold(
                                        "printed cost",
                                        "M€",
                                    )
                            ),
                    )
            ),
        klass("ConvertPlantsSA") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "the Convert Plants standard action",
                        refundDiscountPredicate = "convert plants to greenery",
                    )
            ),
        klass("PowerPlantSP") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "the Power Plant standard project",
                        refundDiscountPredicate = "use the Power Plant standard project",
                    )
            ),
        klass("TradeSA") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "the Trade standard action",
                        refundDiscountPredicate = "use the Trade standard action",
                        refundDiscountNoun =
                            ComponentDescriber.Noun.Counted("resource", "resources"),
                    )
            ),
        klass("Pay") to ComponentDescriber(spentResourceTrigger = true),
        klass("PayFromCard") to ComponentDescriber(spentResourceTrigger = true),
        klass("Owed") to
            ComponentDescriber(
                paymentRole = ComponentDescriber.PaymentRole.OWED,
                implicitPaymentResource = ComponentDescriber.Noun.Fixed("M€"),
            ),
        klass("Accept") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.ACCEPTANCE),
        klass("AcceptFromCard") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.ACCEPTANCE),
        klass("Barrier") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.BARRIER),
        klass("Required") to ComponentDescriber(requirementShortfall = true),
    )
  }

  private fun uniqueDeclarations(
      vararg entries: Pair<Class, ComponentDescriber>,
  ): Map<Class, ComponentDescriber> = buildMap {
    entries.forEach { (componentClass, describer) ->
      check(put(componentClass, describer) == null) {
        "Duplicate English component declaration for ${componentClass.className}"
      }
    }
  }

  private fun klass(name: String): Class = Canon.classTable.getClass(cn(name))

  private fun threshold(
      subject: String,
      value: ComponentDescriber.Requirement.Value = ComponentDescriber.Requirement.Value.PLAIN,
      syntax: ComponentDescriber.Requirement.ThresholdSyntax,
  ): ComponentDescriber.Requirement.Bound =
      ComponentDescriber.Requirement.Bound.Threshold(subject, value, syntax)

  private fun count(
      singular: String,
      plural: String,
      syntax: RequirementCount,
      anyoneSyntax: RequirementCount? = null,
  ): ComponentDescriber.Requirement.Bound =
      ComponentDescriber.Requirement.Bound.Count(
          ComponentDescriber.Noun.Counted(singular, plural),
          syntax,
          anyoneSyntax,
      )
}
