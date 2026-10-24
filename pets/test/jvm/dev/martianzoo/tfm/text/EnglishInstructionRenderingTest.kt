package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class EnglishInstructionRenderingTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun omitsWildTagAllocationInstructions() {
    english.describe(
        parse<InstructionTree>("EACH Choice@WildTagElection { Tag<Choice@WildTagElection>? }")
    ) shouldBe ""
    english.describe(
        parse<InstructionTree>(
            "EACH @WildTagElection(HAS MAX 0 Tag) { ScienceTag<@WildTagElection> }"
        )
    ) shouldBe ""
    english.describe(
        parse<InstructionTree>(
            "EACH @WildTagElection { Tag<@WildTagElection>? }, PROD[MC / Class<@Tag>(HAS @Tag)]"
        )
    ) shouldBe "Increase your M€ production 1 step per different tag you have."
    english.describe(
        parse<Effect>(
            "UseAction<OrbitalCleanup, Action1>:: EACH @WildTagElection(HAS MAX 0 Tag) { ScienceTag<@WildTagElection> }"
        )
    ) shouldBe ""
    english.describe(parse<InstructionTree>("AllocateCloudTourismWildTags")) shouldBe ""
    english.describe(
        parse<InstructionTree>(
            "EACH @WildTagElection(HAS MAX 0 Tag) { 2 ScienceTag<@WildTagElection> }"
        )
    ) shouldContain "["
    english.describe(
        parse<InstructionTree>(
            "EACH @WildTagElection<Anyone>(HAS MAX 0 Tag) { Tag<@WildTagElection>? }"
        )
    ) shouldContain "["
  }

  @Test
  internal fun describesDistinctTagRequirementsWithUnusedWilds() {
    english.describe(
        parse<Requirement>("8 (Class<@Tag>(HAS @Tag) OR WildTagElection(HAS MAX 0 Tag))")
    ) shouldBe "Requires 8 different tags."
    english.describe(
        parse<Requirement>("5 (WildTagElection(HAS MAX 0 Tag) OR Class<@Tag>(HAS @Tag))")
    ) shouldBe "Requires 5 different tags."
    english.describe(
        parse<Requirement>("8 (Class<@Tag>(HAS @Tag) OR WildTagElection(HAS Tag))")
    ) shouldContain "["
    english.describe(
        parse<Requirement>("8 (Class<@Tag>(HAS @Tag) OR WildTagElection<Anyone>(HAS MAX 0 Tag))")
    ) shouldContain "["
  }

  @Test
  internal fun preservesCapOrderAndResourceRetention() {
    english.describe(parse<InstructionTree>("-2* MC / (BuildingTag MAX 3 - Influence)")) shouldBe
        "Lose 2 M€ per building tag you have (max 3) in excess of influence (to the extent possible)."
    english.describe(parse<InstructionTree>("-2* MC / (BuildingTag - Influence) MAX 3")) shouldBe
        "Lose 2 M€ per building tag you have in excess of influence (max 3) (to the extent possible)."
    english.describe(parse<InstructionTree>("-Steel! / (Steel - Influence - 2)")) shouldBe
        "Remove all steel except 2 plus influence."
    english.describe(parse<InstructionTree>("-2! Steel / Steel")) shouldBe
        "Lose 2 steel per steel you have."
    english.describe(parse<InstructionTree>("-Steel? / Steel")) shouldBe
        "You may remove up to 1 steel per steel you have."
    english.describe(parse<InstructionTree>("2 MC / (BuildingTag - (Influence - 2))")) shouldBe
        "Gain 2 M€ per [BuildingTag - (Influence - 2)]."
  }

  @Test
  internal fun distinguishesThresholdRewardsFromRepeatedSets() {
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 1")) shouldBe
        "If you have at least 3 Earth tags and influence combined, gain 5 M€."
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 2")) shouldBe
        "Gain 5 M€ per complete set of 3 Earth tags and influence combined (max 2)."
  }

  @Test
  internal fun rendersSelectionTransfersWithoutInventingCleanup() {
    english.describe(
        parse<InstructionTree>("4 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>")
    ) shouldBe "Look at 4 cards. Keep 2 cards from the selection."
    english.describe(parse<InstructionTree>("PreludeCard<Hand FROM Selecting>")) shouldBe
        "Keep 1 Prelude card from the selection."
    english.describe(
        parse<InstructionTree>("ProjectCard<Anyone, Hand> FROM ProjectCard<Selecting>")
    ) shouldContain "["
  }

  @Test
  internal fun linksVariableCardCountsToTheEnclosingActionCost() {
    english.describe(
        listOf(
            parse<Action>("X Energy -> X ProjectCard<Selecting>, ProjectCard<Hand FROM Selecting>")
        )
    ) shouldBe
        "Spend 1 or more energy to look at that many cards and keep 1 card from the selection."
    english.describe(
        listOf(
            parse<Action>("2X Energy -> X ProjectCard<Selecting>, ProjectCard<Hand FROM Selecting>")
        )
    ) shouldBe "Spend 2X energy to look at X cards and keep 1 card from the selection."
    english.describe(parse<InstructionTree>("X ProjectCard<Selecting>")) shouldContain "["
    english.describe(
        listOf(parse<Action>("X Energy -> X ProjectCard<Anyone, Selecting>"))
    ) shouldContain "["
  }

  @Test
  internal fun countsAllProductionInACategoryRatherThanChoosingAKind() {
    english.describe(parse<Requirement>("6 PROD[Metal]")) shouldBe
        "Requires that you have 6 steel and titanium production combined."
    english.describe(parse<Requirement>("6 PROD[Steel]")) shouldBe
        "Requires that you have 6 steel production."
  }

  @Test
  internal fun preservesUnsupportedConditionsInsideRenderedEffects() {
    english.describe(parse<Effect>("ProductionPhase IF MAX 0 Audit: 8 MC")) shouldBe
        "When the production phase begins, if [MAX 0 Audit], gain 8 M€."
    english.describe(parse<Effect>("ProductionPhase IF MAX 0 TrWasRaised: 8 MC")) shouldBe
        "When the production phase begins, if it is not the case that your terraform rating has been raised this generation, gain 8 M€."
    english.describe(parse<Effect>("ProductionPhase IF MAX 0 ActionPhase: 8 MC")) shouldBe
        "When the production phase begins, if it is not the case that the action phase is active, gain 8 M€."
  }

  @Test
  internal fun distinguishesEachFromPerAndUsesTheSelectedComponent() {
    english.describe(parse<InstructionTree>("EACH Colony { 2 PartyDelegate }")) shouldBe
        "For each colony you own: place 2 delegates."
    english.describe(parse<InstructionTree>("2 PartyDelegate / Colony")) shouldBe
        "Place 2 delegates per colony you own."
    english.describe(
        parse<InstructionTree>("EACH @ColonyTile { 2 ColonyProduction<@ColonyTile> }")
    ) shouldBe "For each colony tile: increase that production track 2 steps."
    english.describe(
        parse<InstructionTree>("EACH Colony<Anyone> { 2 PartyDelegate }")
    ) shouldContain "["
    english.describe(parse<InstructionTree>("EACH @Colony { MC<@Colony> }")) shouldContain "["
  }
}
