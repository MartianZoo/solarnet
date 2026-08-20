package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.Class

private typealias RequirementCount = ComponentDescriber.Requirement.CountSyntax

/** Terraforming Mars component descriptions supplied to the structural English renderer. */
internal object TerraformingMarsDescribers {
  internal val descriptions: Map<Class, ComponentDescriber> by lazy {
    Canon.classTable.allClasses().associateWith { declarations[it] ?: ComponentDescriber() }
  }

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    mapOf(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("StandardResource") to
            ComponentDescriber(
                standardResource = true,
                directChange = ComponentDescriber.DirectChange.GainChoice("any standard resource"),
            ),
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
                draw = true,
            ),
        klass("CardResource") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("resource", "resources"),
                cardResource = ComponentDescriber.CardResource.SUFFIXED,
                textNeutralSubclasses = true,
            ),
        klass("CardFront") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Fixed("card"),
                cardResourceHolder = ComponentDescriber.Noun.Counted("card", "cards"),
                playedCard =
                    ComponentDescriber.PlayedCard(
                        minimumProperties =
                            mapOf(
                                "cost" to
                                    ComponentDescriber.PlayedCard.MinimumProperty.Threshold(
                                        "printed cost",
                                        "M€",
                                    ),
                                "requirement" to
                                    ComponentDescriber.PlayedCard.MinimumProperty.Presence(
                                        "requirement"
                                    ),
                            )
                    ),
            ),
        klass("ProjectCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("card", "cards"),
                discardable = true,
            ),
        klass("EventCard") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("MarsArea") to ComponentDescriber(metricLocation = "on Mars"),
        klass("RemoteArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("reserved area outside Mars"),
                        article = "the",
                    ),
                textNeutralSubclasses = true,
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
                    )
            ),
        klass("SpecialTile") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        article = "this",
                        singular = "tile",
                        plural = "tiles",
                        allowsMultiple = false,
                    ),
                textNeutralSubclasses = true,
            ),
        klass("Animal") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Asteroid") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Floater") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Microbe") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Tag") to ComponentDescriber(tag = ComponentDescriber.Tag.ORDINARY),
        klass("PlanetTag") to ComponentDescriber(tag = ComponentDescriber.Tag.PLANET),
        klass("BioTag") to
            ComponentDescriber(playedTagPhrase = "an animal tag, a plant tag, or a microbe tag"),
        klass("AnimalTag") to ComponentDescriber(playedTagPhrase = "an animal tag"),
        klass("PlantTag") to ComponentDescriber(playedTagPhrase = "a plant tag"),
        klass("MicrobeTag") to ComponentDescriber(playedTagPhrase = "a microbe tag"),
        klass("OxygenStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("oxygen"),
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
                track = ComponentDescriber.Track("temperature"),
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
                track = ComponentDescriber.Track("Venus"),
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
                track = ComponentDescriber.Track("your terraform rating"),
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
                placement = ComponentDescriber.Placement("an", "ocean tile", "ocean tiles"),
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
                placement =
                    ComponentDescriber.Placement(
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
                placement =
                    ComponentDescriber.Placement(
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
                placement =
                    ComponentDescriber.Placement(
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
                directChange = ComponentDescriber.DirectChange.TopCardPurchase,
                purchasePrice =
                    ComponentDescriber.PurchasePrice(
                        subject = "buying cards to hand",
                        ordinaryCost = 3,
                        resource = ComponentDescriber.Noun.Fixed("M€"),
                        scope = "including your starting hand",
                    ),
            ),
        klass("CopyPrelude") to
            ComponentDescriber(
                directChange =
                    ComponentDescriber.DirectChange.Imperative(
                        "copy",
                        "your other Prelude's direct effect",
                    )
            ),
        klass("CopyProductionBox") to
            ComponentDescriber(
                directChange = ComponentDescriber.DirectChange.ProductionBoxCopy,
            ),
        klass("GiveColonyBonuses") to
            ComponentDescriber(
                directChange =
                    ComponentDescriber.DirectChange.Imperative(
                        "gain",
                        "all your colony bonuses",
                    )
            ),
        klass("NextCardEffect") to
            ComponentDescriber(
                directChange = ComponentDescriber.DirectChange.NextPlayedCardDiscount,
                directChangeForSubclasses = true,
            ),
        klass("Mandate") to
            ComponentDescriber(
                directChange = ComponentDescriber.DirectChange.FirstAction,
                directChangeForSubclasses = true,
            ),
        klass("ReserveTradeFleet") to
            ComponentDescriber(
                directChange = ComponentDescriber.DirectChange.Gain("Trade Fleet", 1)
            ),
        klass("Production") to ComponentDescriber(production = true),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("Die") to ComponentDescriber(deadEndSignal = true),
        klass("End") to ComponentDescriber(endTrigger = true),
        klass("PlayCard") to ComponentDescriber(playTrigger = ComponentDescriber.PlayTrigger.CARD),
        klass("PlayTag") to ComponentDescriber(playTrigger = ComponentDescriber.PlayTrigger.TAG),
        klass("Trade") to ComponentDescriber(operationTrigger = "trade"),
        klass("UseAction") to ComponentDescriber(usedActionTrigger = true),
        klass("UseAction1") to ComponentDescriber(actionNumber = 1),
        klass("StandardProject") to
            ComponentDescriber(actionUse = ComponentDescriber.ActionUse("a standard project")),
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
        klass("Owed") to ComponentDescriber(owedPayment = true),
    )
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
