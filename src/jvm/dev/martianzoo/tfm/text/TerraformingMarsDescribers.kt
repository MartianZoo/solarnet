package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as Trigger

/** Terraforming Mars component descriptions supplied to the structural English renderer. */
internal object TerraformingMarsDescribers {
  internal val descriptions: Map<Class, ComponentDescriber> by lazy {
    canonClassUniverse.allClasses().associateWith { declarations[it] ?: ComponentDescriber() }
  }

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    uniqueDeclarations(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("HasRaisedTr") to
            ComponentDescriber(presenceCondition = "your terraform rating has been raised"),
        klass("StandardResource") to
            ComponentDescriber(numericSingularChange = true, changeFrame = Frame.Countable),
        klass("Metal") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("titanium or steel")),
        klass("Steel") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("Titanium") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("MC") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("ProjectCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("card", "cards"),
                numericSingularChange = true,
                changeFrame = Frame.Deck,
            ),
        klass("PreludeCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("prelude card", "prelude cards"),
                numericSingularChange = true,
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
                numericSingularChange = true,
                changeFrame = Frame.Held,
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
                triggerFrame =
                    Trigger.PlayCard(
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
                        "next to",
                        ComponentDescriber.Noun.Counted("tile", "tiles"),
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
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.ANYONES,
                    )
            ),
        klass("SpecialTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        article = "this",
                        singular = "tile",
                        plural = "tiles",
                        referenceNoun = counted("special tile", "special tiles"),
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
            ),
        klass("Animal") to ComponentDescriber(noun = counted("animal", "animals")),
        klass("Asteroid") to ComponentDescriber(noun = counted("asteroid", "asteroids")),
        klass("Camp") to ComponentDescriber(noun = counted("camp resource", "camp resources")),
        klass("Director") to
            ComponentDescriber(noun = counted("director resource", "director resources")),
        klass("Disease") to
            ComponentDescriber(noun = counted("disease resource", "disease resources")),
        klass("Fighter") to
            ComponentDescriber(noun = counted("fighter resource", "fighter resources")),
        klass("Floater") to ComponentDescriber(noun = counted("floater", "floaters")),
        klass("Graphene") to
            ComponentDescriber(noun = counted("graphene resource", "graphene resources")),
        klass("Hydroelectric") to
            ComponentDescriber(noun = counted("hydroelectric resource", "hydroelectric resources")),
        klass("Microbe") to ComponentDescriber(noun = counted("microbe", "microbes")),
        klass("Preservation") to
            ComponentDescriber(noun = counted("preservation resource", "preservation resources")),
        klass("Science") to
            ComponentDescriber(noun = counted("science resource", "science resources")),
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
        klass("BioTag") to ComponentDescriber(triggerFrame = Trigger.PlayTag("a bio tag")),
        klass("PlanetaryTag") to
            ComponentDescriber(triggerFrame = Trigger.PlayTag("a planetary tag")),
        klass("AnimalTag") to ComponentDescriber(triggerFrame = Trigger.PlayTag("an animal tag")),
        klass("PlantTag") to ComponentDescriber(triggerFrame = Trigger.PlayTag("a plant tag")),
        klass("MicrobeTag") to ComponentDescriber(triggerFrame = Trigger.PlayTag("a microbe tag")),
        klass("OxygenStep") to
            ComponentDescriber(
                changeFrame = Frame.Scale("oxygen"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "oxygen",
                                ComponentDescriber.Requirement.Value.PERCENT,
                            ),
                        maximum =
                            threshold(
                                "oxygen",
                                ComponentDescriber.Requirement.Value.PERCENT,
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
                            ),
                        maximum =
                            threshold(
                                "temperature",
                                ComponentDescriber.Requirement.Value.TEMPERATURE,
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
                            ),
                        maximum =
                            threshold(
                                "Venus",
                                ComponentDescriber.Requirement.Value.DOUBLE_PERCENT,
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
                                "your terraform rating",
                            )
                    ),
            ),
        klass("OceanTile") to
            ComponentDescriber(
                numericSingularChange = true,
                changeFrame = Frame.Positioned("an", "ocean tile", "ocean tiles"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = count("ocean tile", "ocean tiles"),
                        maximum = count("ocean tile", "ocean tiles"),
                    ),
            ),
        klass("GreeneryTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        "a",
                        "greenery tile",
                        "greenery tiles",
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            count(
                                "greenery tile",
                                "greenery tiles",
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
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            count(
                                "city tile",
                                "city tiles",
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
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = count("colony", "colonies"),
                        maximum = count("colony", "colonies"),
                        ownedCount = ComponentDescriber.Noun.Counted("colony", "colonies"),
                    ),
            ),
        klass("BuyCard") to
            ComponentDescriber(
                triggerFrame =
                    Trigger.Purchase(noun = ComponentDescriber.Noun.Counted("card", "cards")),
            ),
        klass("CopyPrelude") to
            ComponentDescriber(
                changeFrame = Frame.Procedure("copy", "your other Prelude's direct effect")
            ),
        klass("GiveColonyBonuses") to
            ComponentDescriber(changeFrame = Frame.Procedure("gain", "all your colony bonuses")),
        klass("ColonyTileSelection") to
            ComponentDescriber(changeFrame = Frame.Procedure("add", "a colony tile")),
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
                triggerFrame = Trigger.PlayCard(),
            ),
        klass("RequirementCheck") to ComponentDescriber(triggerFrame = Trigger.PlayCard()),
        klass("PlayTag") to ComponentDescriber(triggerFrame = Trigger.PlayTag()),
        klass("UseAction") to ComponentDescriber(triggerFrame = Trigger.UseAction),
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
                                        positiveObjectPhrase =
                                            "a standard project, except selling patents",
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
        klass("ClaimMilestoneSA") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "the Claim Milestone standard action",
                        refundDiscountPredicate = "claim a milestone",
                    )
            ),
        klass("FundAwardSA") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "the Fund Award standard action",
                        refundDiscountPredicate = "fund an award",
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
        klass("HasActions") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "an action",
                        refundDiscountPredicate = "use an action",
                    )
            ),
        klass("BuyCards") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "a card",
                        refundDiscountPredicate = "buy a card",
                    )
            ),
        klass("PlayCards") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        objectPhrase = "a card",
                        refundDiscountPredicate = "play a card",
                    )
            ),
        klass("Pay") to ComponentDescriber(triggerFrame = Trigger.SpendResource),
        klass("PayFromCard") to ComponentDescriber(triggerFrame = Trigger.SpendResource),
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

  private fun klass(name: String): Class = canonClassUniverse.getClass(cn(name))

  private fun threshold(
      subject: String,
      value: ComponentDescriber.Requirement.Value = ComponentDescriber.Requirement.Value.PLAIN,
  ): ComponentDescriber.Requirement.Bound =
      ComponentDescriber.Requirement.Bound.Threshold(
          subject,
          value,
      )

  private fun counted(
      singular: String,
      plural: String,
  ): ComponentDescriber.Noun.Counted = ComponentDescriber.Noun.Counted(singular, plural)

  private fun count(
      singular: String,
      plural: String,
  ): ComponentDescriber.Requirement.Bound =
      ComponentDescriber.Requirement.Bound.Count(
          ComponentDescriber.Noun.Counted(singular, plural),
      )
}
