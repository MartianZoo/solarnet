package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame
import dev.martianzoo.tfm.text.ComponentDescriber.RequirementCondition as Condition
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as Trigger
import dev.martianzoo.tfm.text.coloniesexpansion.coloniesEnglishDeclarations
import dev.martianzoo.tfm.text.corporateeraexpansion.corporateEraEnglishDeclarations
import dev.martianzoo.tfm.text.prelude1cardpack.prelude1EnglishDeclarations
import dev.martianzoo.tfm.text.prelude2cardpack.prelude2EnglishDeclarations
import dev.martianzoo.tfm.text.preludecommon.preludeEnglishDeclarations
import dev.martianzoo.tfm.text.promocardpack.promoEnglishDeclarations
import dev.martianzoo.tfm.text.turmoilcardpack.turmoilCardPackEnglishDeclarations
import dev.martianzoo.tfm.text.turmoilexpansion.turmoilEnglishDeclarations
import dev.martianzoo.tfm.text.vastitasmap.vastitasEnglishDeclarations
import dev.martianzoo.tfm.text.venusnextexpansion.venusNextEnglishDeclarations

/** Terraforming Mars component descriptions supplied to the structural English renderer. */
internal object TerraformingMarsDescribers {
  private val declarations: Map<ClassName, ComponentDescriber> = run {
    val tileNoun = counted("tile", "tiles")
    val oceanTileNoun = counted("ocean tile", "ocean tiles")
    val greeneryTileNoun = counted("greenery tile", "greenery tiles")
    val cityTileNoun = counted("city tile", "city tiles")
    uniqueDeclarations(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("HasRaisedTr") to
            ComponentDescriber(presenceCondition = "your terraform rating has been raised"),
        klass("SoloMode") to ComponentDescriber(presenceCondition = "this is a solo game"),
        klass("Pass") to
            ComponentDescriber(
                requirementCondition = Condition.OwnerState("has passed"),
                changeFrame = Frame.Procedure("pass"),
            ),
        klass("StandardResource") to
            ComponentDescriber(numericSingularChange = true, changeFrame = Frame.Countable),
        klass("Metal") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("titanium or steel")),
        klass("Steel") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
                basePaymentValue = true,
            ),
        klass("Titanium") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
                basePaymentValue = true,
            ),
        klass("MC") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("ProjectCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("card", "cards"),
                countNoun = ComponentDescriber.Noun.Counted("card", "cards"),
                numericSingularChange = true,
                changeFrame = Frame.Deck,
            ),
        klass("CorporationCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("corporation card", "corporation cards"),
                numericSingularChange = true,
                changeFrame = Frame.Deck,
            ),
        klass("PlayedEvent") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun = ComponentDescriber.Noun.Counted("card", "cards"),
                        unqualifiedSuffix = "in your event pile",
                        anyoneSuffix = "in all players' event piles",
                    )
            ),
        klass("GlobalParameter") to ComponentDescriber(requirementKind = "global parameter"),
        klass("GpComplete") to
            ComponentDescriber(
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "completed global parameter",
                                "completed global parameters",
                            ),
                        unqualifiedSuffix = "",
                    )
            ),
        klass("ResourceHolder") to
            ComponentDescriber(
                cardResourceHolder = ComponentDescriber.Noun.Counted("card", "cards")
            ),
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
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "card resource",
                                "card resources",
                            ),
                        unqualifiedSuffix = "",
                        forSubclasses = false,
                    ),
            ),
        klass("CardFront") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Fixed("card"),
                countNoun = ComponentDescriber.Noun.Counted("card", "cards"),
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun = ComponentDescriber.Noun.Counted("card", "cards"),
                        unqualifiedSuffix = "in play",
                        forSubclasses = false,
                    ),
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
        klass("ActiveCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("active card", "active cards"),
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun = ComponentDescriber.Noun.Counted("active card", "active cards"),
                        unqualifiedSuffix = "in play",
                    ),
            ),
        klass("AutomatedCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("automated card", "automated cards"),
                metricCount =
                    ComponentDescriber.MetricCount(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "automated card",
                                "automated cards",
                            ),
                        unqualifiedSuffix = "in play",
                    ),
            ),
        klass("EventCard") to ComponentDescriber(noun = ComponentDescriber.Noun.ClassName),
        klass("MarsArea") to
            ComponentDescriber(
                metricLocation = "on Mars",
                metricCount =
                    ComponentDescriber.MetricCount(
                        counted("area", "areas"),
                        unqualifiedSuffix = "on Mars",
                        forSubclasses = false,
                    ),
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Counted("area", "areas"),
                        forSubclasses = false,
                    ),
            ),
        klass("Placement") to
            ComponentDescriber(
                triggerFrame = Trigger.Place(tileNoun),
            ),
        klass("Hand") to ComponentDescriber(metricLocation = "in hand"),
        klass("RemoteArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("reserved area outside Mars"),
                        determiner = Determiner.THE,
                    ),
            ),
        klass("WaterArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("area reserved for ocean"),
                        determiner = Determiner.INDEFINITE,
                    ),
            ),
        klass("NoctisArea") to
            ComponentDescriber(
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun = ComponentDescriber.Noun.Fixed("reserved area"),
                        determiner = Determiner.THE,
                    ),
            ),
        klass("VolcanicArea") to
            ComponentDescriber(
                metricLocation = "on volcanic areas",
                placementSite =
                    ComponentDescriber.PlacementSite(
                        noun =
                            ComponentDescriber.Noun.Counted(
                                "volcanic area",
                                "volcanic areas",
                            ),
                        determiner = Determiner.INDEFINITE,
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
                        defaultTarget = tileNoun,
                    )
            ),
        klass("Adjacency") to
            ComponentDescriber(
                spatialRelation =
                    ComponentDescriber.SpatialRelation(
                        countedPair = true,
                        eventNoun = "adjacency",
                    )
            ),
        klass("PlacementBonus") to
            ComponentDescriber(
                placementBonus =
                    ComponentDescriber.PlacementBonus(
                        ComponentDescriber.Noun.Counted("placement bonus", "placement bonuses")
                    )
            ),
        klass("OwnedOccupant") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        determiner = Determiner.INDEFINITE,
                        noun = counted("tile or community", "tiles or communities"),
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.ANYONES,
                    )
            ),
        klass("Tile") to
            ComponentDescriber(
                changeFrame = Frame.Positioned(Determiner.INDEFINITE, tileNoun),
            ),
        klass("OwnedTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        tileNoun,
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.ANYONES,
                    )
            ),
        klass("SpecialTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        determiner = Determiner.THIS,
                        noun = tileNoun,
                        referenceNoun = counted("special tile", "special tiles"),
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
            ),
        klass("Animal") to ComponentDescriber(noun = counted("animal", "animals")),
        klass("Asteroid") to ComponentDescriber(noun = counted("asteroid", "asteroids")),
        klass("Floater") to ComponentDescriber(noun = counted("floater", "floaters")),
        klass("Microbe") to ComponentDescriber(noun = counted("microbe", "microbes")),
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
        klass("BioTag") to
            ComponentDescriber(triggerFrame = Trigger.PlayTag(counted("bio tag", "bio tags"))),
        klass("PlanetaryTag") to
            ComponentDescriber(
                triggerFrame = Trigger.PlayTag(counted("planetary tag", "planetary tags")),
                capitalizeTagName = true,
            ),
        klass("AnimalTag") to
            ComponentDescriber(
                triggerFrame = Trigger.PlayTag(counted("animal tag", "animal tags"))
            ),
        klass("PlantTag") to
            ComponentDescriber(triggerFrame = Trigger.PlayTag(counted("plant tag", "plant tags"))),
        klass("MicrobeTag") to
            ComponentDescriber(
                triggerFrame = Trigger.PlayTag(counted("microbe tag", "microbe tags"))
            ),
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
        klass("TerraformRating") to
            ComponentDescriber(
                changeFrame = Frame.Scale("your terraform rating"),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum =
                            threshold(
                                "terraform rating",
                            )
                    ),
            ),
        klass("OceanTile") to
            ComponentDescriber(
                numericSingularChange = true,
                changeFrame = Frame.Positioned(Determiner.INDEFINITE, oceanTileNoun),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = count(oceanTileNoun),
                        maximum = count(oceanTileNoun),
                    ),
            ),
        klass("GreeneryTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        greeneryTileNoun,
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                requirement = ComponentDescriber.Requirement(minimum = count(greeneryTileNoun)),
            ),
        klass("DefaultGreeneryTile") to
            ComponentDescriber(changeFrame = Frame.Procedure("place", "a greenery tile")),
        klass("CityTile") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        cityTileNoun,
                        unqualifiedOwnership = ComponentDescriber.OwnershipPhrase.YOURS,
                        anyoneOwnership = ComponentDescriber.OwnershipPhrase.IMPLICIT,
                    ),
                placementSite = ComponentDescriber.PlacementSite(cityTileNoun),
                requirement =
                    ComponentDescriber.Requirement(
                        minimum = count(cityTileNoun),
                        ownedCount = cityTileNoun,
                    ),
            ),
        klass("CapitalMarker") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        counted("capital marker", "capital markers"),
                    )
            ),
        klass("Community") to
            ComponentDescriber(
                changeFrame =
                    Frame.Positioned(
                        Determiner.INDEFINITE,
                        counted("community marker", "community markers"),
                    )
            ),
        klass("PayingFor") to
            ComponentDescriber(
                triggerFrame =
                    Trigger.PayingFor(
                        purchaseClass = klass("ProjectCard"),
                        purchaseNoun = counted("card", "cards"),
                    )
            ),
        klass("WorldGovernmentTerraforming") to
            ComponentDescriber(
                changeFrame =
                    Frame.Procedure(
                        "raise",
                        "1 global parameter without gaining terraform rating or other bonuses",
                    )
            ),
        klass("RequiredAction") to ComponentDescriber(changeFrame = Frame.RequiredAction),
        klass("NextCardEffect") to ComponentDescriber(changeFrame = Frame.NextCardEffect),
        klass("CopyProductionBox") to
            ComponentDescriber(
                changeFrame =
                    Frame.Procedure(
                        "copy",
                        "the immediate production box",
                        cardTargetRelation = "of",
                    )
            ),
        klass("ActionUsedMarker") to
            ComponentDescriber(
                noun =
                    ComponentDescriber.Noun.Counted(
                        "action-used marker",
                        "action-used markers",
                    )
            ),
        klass("Award") to
            ComponentDescriber(changeFrame = Frame.Procedure("fund", "an award for free")),
        klass("ProdOffset") to ComponentDescriber(productionOffset = true),
        klass("QuickStartVariant") to ComponentDescriber(productionOffset = true),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("Die") to ComponentDescriber(deadEndSignal = true),
        klass("PlayCard") to
            ComponentDescriber(
                changeFrame = Frame.Play,
                triggerFrame = Trigger.PlayCard(),
            ),
        klass("CheckRequirement") to ComponentDescriber(triggerFrame = Trigger.PlayCard()),
        klass("UseAction") to ComponentDescriber(triggerFrame = Trigger.UseAction),
        klass("StandardProject") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        ComponentDescriber.ActionUse.Reference.Fixed("a standard project"),
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
        klass("ConvertPlantsAction") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference =
                            ComponentDescriber.ActionUse.Reference.Fixed(
                                "the Convert Plants standard action"
                            ),
                        paymentDiscount =
                            ComponentDescriber.PaymentDiscount(
                                "convert plants to greenery",
                                objectPronoun = false,
                            ),
                    )
            ),
        klass("PowerPlantProject") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference =
                            ComponentDescriber.ActionUse.Reference.Fixed(
                                "the Power Plant standard project"
                            ),
                        paymentDiscount =
                            ComponentDescriber.PaymentDiscount(
                                "use the Power Plant standard project"
                            ),
                    )
            ),
        klass("ClaimMilestoneAction") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference =
                            ComponentDescriber.ActionUse.Reference.Fixed(
                                "the Claim Milestone standard action"
                            ),
                        paymentDiscount = ComponentDescriber.PaymentDiscount("claim a milestone"),
                    )
            ),
        klass("FundAwardAction") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference =
                            ComponentDescriber.ActionUse.Reference.Fixed(
                                "the Fund Award standard action"
                            ),
                        paymentDiscount = ComponentDescriber.PaymentDiscount("fund an award"),
                    )
            ),
        klass("HasActions") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference = ComponentDescriber.ActionUse.Reference.AnyAction,
                        paymentDiscount = ComponentDescriber.PaymentDiscount("use an action"),
                    )
            ),
        klass("CardPurchase") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference = ComponentDescriber.ActionUse.Reference.Fixed("a card"),
                        paymentDiscount = ComponentDescriber.PaymentDiscount("buy a card"),
                    )
            ),
        klass("CardPlay") to
            ComponentDescriber(
                actionUse =
                    ComponentDescriber.ActionUse(
                        reference = ComponentDescriber.ActionUse.Reference.Fixed("a card"),
                        paymentDiscount = ComponentDescriber.PaymentDiscount("play a card"),
                    )
            ),
        klass("Pay") to ComponentDescriber(triggerFrame = Trigger.SpendResource),
        klass("PayFromCard") to ComponentDescriber(triggerFrame = Trigger.SpendResource),
        klass("Owed") to
            ComponentDescriber(
                paymentRole = ComponentDescriber.PaymentRole.OWED,
                implicitPaymentResource = ComponentDescriber.Noun.Fixed("M€"),
            ),
        klass("Accepting") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.ACCEPTANCE),
        klass("AcceptingFromCard") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.ACCEPTANCE),
        klass("Barrier") to
            ComponentDescriber(paymentRole = ComponentDescriber.PaymentRole.BARRIER),
        klass("Required") to ComponentDescriber(requirementShortfall = true),
    )
  }

  internal val descriptions: Map<ClassName, ComponentDescriber> =
      uniqueDeclarations(
          *(declarations.toList() +
                  coloniesEnglishDeclarations +
                  corporateEraEnglishDeclarations +
                  prelude1EnglishDeclarations +
                  prelude2EnglishDeclarations +
                  preludeEnglishDeclarations +
                  promoEnglishDeclarations +
                  turmoilCardPackEnglishDeclarations +
                  turmoilEnglishDeclarations +
                  vastitasEnglishDeclarations +
                  venusNextEnglishDeclarations)
              .toTypedArray()
      )

  private fun uniqueDeclarations(
      vararg entries: Pair<ClassName, ComponentDescriber>,
  ): Map<ClassName, ComponentDescriber> = buildMap {
    entries.forEach { (className, describer) ->
      check(put(className, describer) == null) {
        "Duplicate English component declaration for $className"
      }
    }
  }

  private fun klass(name: String): ClassName = cn(name)

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

  private fun count(noun: ComponentDescriber.Noun.Counted): ComponentDescriber.Requirement.Bound =
      ComponentDescriber.Requirement.Bound.Count(noun)
}
