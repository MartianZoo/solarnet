package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
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
    checkNotNull(
            renderRankedMetricPhrase(
                parse<Metric>("Production<Class<StandardResource>(NOT Class<MC>)>"),
                describers,
            )
        )
        .linearize() shouldBe "highest production excluding M€ production"
    checkNotNull(
            renderRankedMetricPhrase(
                parse<Metric>("Production<Class<StandardResource>(NOT Class<Heat>)>"),
                describers,
            )
        )
        .linearize() shouldBe "highest production excluding heat production"
  }

  @Test
  fun unsupportedHasRequirementsStayInsideTheMetricPhrase() {
    english.describe(parse<InstructionTree>("2 MC / Party(HAS PartyDelegate)")) shouldBe
        "Gain 2 M€ per party with [PartyDelegate]."
    english.describe(parse<Requirement>("3 OwnedTile<MarsArea(HAS 8 row)>")) shouldBe
        "Requires that you have 3 tiles on Mars areas that have [8 row]."
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
