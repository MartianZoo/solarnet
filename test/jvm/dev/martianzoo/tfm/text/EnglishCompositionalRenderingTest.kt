package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

internal class EnglishCompositionalRenderingTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  fun trackAvailabilityIsImplicitOnlyForASingleIncreaseOfThatTrack() {
    for (instruction in
        listOf(
            "TemperatureStep! / GpIncomplete<Class<TemperatureStep>>",
            "GpIncomplete<Class<TemperatureStep>>: TemperatureStep!",
        )) {
      english.describe(parse<InstructionTree>(instruction)) shouldBe "Raise temperature 1 step."
    }
    english.describe(
        parse<InstructionTree>("TemperatureStep! / GpComplete<Class<TemperatureStep>>")
    ) shouldBe "If temperature has reached its maximum, raise temperature 1 step."
    english.describe(
        parse<InstructionTree>("TemperatureStep! / GpIncomplete<Class<OxygenStep>>")
    ) shouldBe "If oxygen has not reached its maximum, raise temperature 1 step."
    english.describe(
        parse<InstructionTree>("2! TemperatureStep / GpIncomplete<Class<TemperatureStep>>")
    ) shouldBe "If temperature has not reached its maximum, raise temperature 2 steps."
  }

  @Test
  fun singletonMultipliersDescribeConditionsButRepeatedCountsRemainPer() {
    english.describe(parse<InstructionTree>("2 MC / GpIncomplete<Class<TemperatureStep>>")) shouldBe
        "If temperature has not reached its maximum, gain 2 M€."
    english.describe(parse<InstructionTree>("MC / GpComplete<Class<OxygenStep>>")) shouldBe
        "If oxygen has reached its maximum, gain 1 M€."
    val multipleTracks = english.describe(parse<InstructionTree>("MC / GpComplete"))
    multipleTracks shouldContain "per"
    multipleTracks shouldNotContain "If"
    val multipleTiles = english.describe(parse<InstructionTree>("MC / OceanTile"))
    multipleTiles shouldContain "per"
    multipleTiles shouldNotContain "If"
  }

  @Test
  fun productionSelectionsMatchPlacementBonusesWithoutDroppingOtherRestrictions() {
    english.describe(
        parse<InstructionTree>("PROD[-2 Metal(HAS PlacementBonus<WaterArea>)]")
    ) shouldBe
        "Decrease your titanium or steel production 2 steps matching a placement bonus on an area reserved for ocean."
    english.describe(
        parse<InstructionTree>("PROD[StandardResource(HAS PlacementBonus<LandArea>)]")
    ) shouldBe
        "Increase your standard resource production 1 step matching a placement bonus on a land area."
    english.describe(parse<InstructionTree>("PROD[Plant(HAS PlacementBonus<LandArea>)]")) shouldBe
        "Increase your plant production 1 step matching a placement bonus on a land area."
    for (selector in
        listOf(
            "Metal(HAS 2 PlacementBonus<LandArea>)",
            "Metal(HAS PlacementBonus<LandArea, Class<Steel>>)",
            "Metal(HAS PlacementBonus<LandArea>, HAS Tag)",
        )) {
      english.describe(parse<InstructionTree>("PROD[$selector]")) shouldContain "["
    }
  }

  @Test
  fun productionBonusUsesThePreviouslyChosenArea() {
    english.describe(
        parse<InstructionTree>(
            "OceanTile<@LandArea>! THEN PROD[Metal(HAS PlacementBonus<@LandArea>)]"
        )
    ) shouldBe
        "Place an ocean tile on a land area, then increase your titanium or steel production 1 step matching a placement bonus on that land area."
  }

  @Test
  fun thisTileMeansTheConcreteTilePlacedByTheCurrentCard() {
    val describers =
        Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)
            .forCard(Canon.classTable.getClass(cn("MiningArea")), null)
    for (occupant in listOf("SpecialTile", "MiningRights_SpecialTile")) {
      renderInstructionTree(
              parse<InstructionTree>("OceanTile<LandArea(HAS $occupant)>!"),
              describers,
          )
          .linearize() shouldContain "["
    }
    renderInstructionTree(
            parse<InstructionTree>("OceanTile<LandArea(HAS MiningArea_SpecialTile)>!"),
            describers,
        )
        .linearize() shouldBe "Place an ocean tile on a land area with this tile."
  }

  @Test
  fun siteOccupantFiltersPreserveOwnership() {
    english.describe(parse<InstructionTree>("OceanTile<LandArea(HAS CityTile)>!")) shouldBe
        "Place an ocean tile on a land area with a city tile of your own."
    english.describe(
        parse<InstructionTree>("OceanTile<LandArea(HAS CityTile<Anyone>)>!")
    ) shouldContain "["
  }

  @Test
  fun placementAndRemovalReferToTheSameChosenOwner() {
    english.describe(
        parse<InstructionTree>(
            "GreeneryTile<LandArea(HAS Neighbor<CityTile<Chosen@Anyone>>)>! THEN -2? Plant<Chosen@Anyone>"
        )
    ) shouldBe
        "Place a greenery tile on a land area next to a city tile a player owns, then you may remove up to 2 plants from that player."
    english.describe(
        parse<InstructionTree>(
            "-2? Plant<Chosen@Anyone> THEN GreeneryTile<LandArea(HAS Neighbor<CityTile<Chosen@Anyone>>)>!"
        )
    ) shouldBe
        "You may remove up to 2 plants from any player, then place a greenery tile on a land area next to a city tile that player owns."
    english.describe(
        parse<InstructionTree>(
            "GreeneryTile<LandArea(HAS Neighbor<CityTile<Anyone>>)>! THEN -2? Plant<Anyone>"
        )
    ) shouldNotContain "from that player"
  }

  @Test
  fun implicitOwnerIsNotPresentedAsAPlayerChoice() {
    val text =
        english.describe(
            parse<InstructionTree>(
                "OceanTile<LandArea(HAS Neighbor<OwnedTile<Me@Owner>>)>! THEN -2? Plant<Me@Owner>"
            )
        )
    text shouldNotContain "any player"
    text shouldNotContain "a player owns"
  }

  @Test
  fun sequencesInsideAlternativesKeepTheirGrouping() {
    english.describe(parse<InstructionTree>("Plant OR (Steel THEN MC)")) shouldBe
        "Gain 1 plant or (gain 1 steel, then gain 1 M€)."
    english.describe(parse<InstructionTree>("(Plant OR Steel) THEN MC")) shouldBe
        "Gain 1 plant or 1 steel, then gain 1 M€."
  }

  @Test
  fun amapIsTheSameModifierAcrossChanges() {
    english.describe(parse<InstructionTree>("-4* MC")) shouldBe
        "Lose 4 M€ (to the extent possible)."
    english.describe(parse<InstructionTree>("2* Animal<This>")) shouldBe
        "Add 2 animals to this card (to the extent possible)."
    english.describe(parse<InstructionTree>("2* Energy FROM Heat")) shouldBe
        "Convert 2 heat to 2 energy (to the extent possible)."
  }

  @Test
  fun includingThisFollowsTheNonNegativeIconCriterion() {
    val describers = Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)
    val trigger =
        parse<Effect>("CardFront(HAS NonNegativeIconsOf<Class<VictoryPoint>>): MC").trigger
    for ((name, matches) in listOf("Vitor" to true, "Asteroid" to false, "SearchForLife" to true)) {
      val card = Canon.classTable.getClass(dev.martianzoo.pets.ast.ClassName.cn(name))
      describers.forCard(card, null).currentCardMatches(trigger) shouldBe matches
    }
  }

  @Test
  fun cardSelectionsFollowTheirSharedVariable() {
    english.describe(
        parse<InstructionTree>(
            "(ScienceTag<Chosen@CardFront>: CopyProductionBox<Chosen@CardFront>) THEN CopyProductionBox<CardFront(HAS ScienceTag, NOT Chosen@CardFront)>"
        )
    ) shouldBe
        "Copy the production box in the direct effect of a science card, then copy the production box in the direct effect of a science card that is not that card."
    english.describe(
        parse<InstructionTree>(
            "CopyProductionBox<CardFront(HAS ScienceTag)> THEN CopyProductionBox<CardFront(HAS ScienceTag)>"
        )
    ) shouldBe
        "Copy the production box in the direct effect of a science card, then copy the production box in the direct effect of a science card."
  }

  @Test
  fun cardResourceTargetsIncludeTagsHeldByAnAssignmentHolder() {
    english.describe(
        parse<InstructionTree>("CardResource<CardFront(HAS VenusTag OR WildTag(HAS VenusTag))>")
    ) shouldBe "Add 1 resource to a Venus card."
  }

  @Test
  fun cardMovesRetainIdentityAndReadThePileOwner() {
    english.describe(parse<InstructionTree>("PlayedEvent<Class<This>> FROM This")) shouldBe
        "Move this card to your event pile."
    english.describe(parse<InstructionTree>("PlayedEvent<Class<Ants>> FROM This")) shouldContain "["
    english.describe(
        parse<InstructionTree>(
            "(MyResourceWasRemoved<Chosen@Player>: MC<Chosen@Player>) THEN PlayedEvent<Chosen@Player, Class<This>> FROM This"
        )
    ) shouldContain "move this card to that player's event pile"
  }

  @Test
  fun conversionReadsDirectionAmountAndPermission() {
    english.describe(parse<InstructionTree>("Energy FROM Heat?")) shouldBe
        "You may convert 1 heat to 1 energy."
    english.describe(parse<InstructionTree>("3! Heat FROM Energy")) shouldBe
        "Convert 3 energy to 3 heat."
    english.describe(parse<Effect>("-Heat IF ActionPhase: Heat FROM Energy?")) shouldBe
        "When 1 heat is removed from you, if the action phase is active, you may convert 1 energy to 1 heat."
  }

  @Test
  fun scoringAndConditionsReadTheirCountsWithoutATimingReminder() {
    english.describe(
        parse<Effect>("End IF 9 Animal<This>: EACH Me@Player { -2 VictoryPoint / CityTile }")
    ) shouldBe
        "If there are at least 9 animals on this card, have each player lose 2 VP per city tile they own."
    english.describe(parse<Effect>("ScienceTag: MAX 2 Disease<This>: 4 TerraformRating")) shouldBe
        "When you play a science tag, if there are at most 2 disease resources on this card, raise your terraform rating 4 steps."
    english.describe(parse<Effect>("End: 5 MC")) shouldBe "[End: 5 MC]."
  }

  @Test
  fun resourceThresholdsRequireCompleteCategoriesAndMatchingAmounts() {
    english.describe(
        parse<Requirement>(
            "PROD[7 (MC - ProdOffset<Class<MC>>) OR 7 Steel OR 7 Titanium OR 7 Plant OR 7 Energy OR 7 Heat]"
        )
    ) shouldBe "Requires 7 production of any standard resource type."
    english.describe(parse<Requirement>("PROD[7 Steel OR 7 Heat]")) shouldNotContain
        "any standard resource type"
    english.describe(
        parse<Requirement>(
            "PROD[7 (MC - ProdOffset<Class<MC>>) OR 7 Steel OR 7 Titanium OR 7 Plant OR 7 Energy OR 8 Heat]"
        )
    ) shouldNotContain "any standard resource type"
  }

  @Test
  fun filteredProductionCategoryNamesTheExcludedType() {
    val describers = Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)
    lexicalizeRankedMetricPhrase(
            parse<Metric>("Production<Class<StandardResource>(NOT Class<MC>)>"),
            describers,
        )
        .linearize() shouldBe "highest production excluding M€ production"
    lexicalizeRankedMetricPhrase(
            parse<Metric>("Production<Class<StandardResource>(NOT Class<Heat>)>"),
            describers,
        )
        .linearize() shouldBe "highest production excluding heat production"
  }

  @Test
  fun unsupportedHasRequirementsStayInsideTheMetricPhrase() {
    english.describe(parse<InstructionTree>("2 MC / Party(HAS PartyDelegate)")) shouldBe
        "Gain 2 M€ per party with [PartyDelegate]."
    english.describe(parse<Requirement>("3 OwnedTile<MarsArea(HAS 8 row)>")) shouldBe
        "Requires that you have 3 tiles on Mars areas whose row is at least 8."
    english.describe(parse<Requirement>("3 OwnedTile<MarsArea(HAS MAX 5 row)>")) shouldBe
        "Requires that you have 3 tiles on Mars areas whose row is at most 5."
    english.describe(
        parse<Requirement>("5 Class<@CardFront>(HAS @CardFront, HAS GainsOf<Class<VictoryPoint>>)")
    ) shouldBe "Requires that you have 5 cards in play with [GainsOf<Class<VictoryPoint>>]."
    english.describe(
        parse<Requirement>("5 Class<@CardFront>(HAS @CardFront, HAS 20 cost)")
    ) shouldBe "Requires that you have 5 cards in play with a printed cost of 20 M€ or more."
  }

  @Test
  fun classQualifiedCardsKeepTheirVisibleCardCount() {
    english.describe(parse<Requirement>("10 CardFront<Class<ProjectCard>>")) shouldBe
        "Requires that you have 10 project cards in play."
    english.describe(parse<InstructionTree>("GlobalParameter BY Admin")) shouldBe
        "Raise any global parameter (performed by Admin)."
    english.describe(parse<InstructionTree>("MC BY Admin")) shouldBe
        "Gain 1 M€ (performed by Admin)."
  }
}
