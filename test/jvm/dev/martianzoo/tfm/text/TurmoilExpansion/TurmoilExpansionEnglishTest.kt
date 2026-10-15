package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class TurmoilExpansionEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesRepeatedDelegatePlacements() {
    english.describe(parse<InstructionTree>("PartyDelegate, PartyDelegate")) shouldBe
        "Place a delegate. Place a delegate."
  }

  @Test
  internal fun preservesSharedAndIndependentPartyChoices() {
    english.describe(
        parse<InstructionTree>("PartyDelegate<@Party> THEN PartyDelegate<@Party>")
    ) shouldBe "Place a delegate in a party, then place a delegate in that party."
    english.describe(
        parse<InstructionTree>("PartyDelegate<Party> THEN PartyDelegate<Party>")
    ) shouldBe "Place a delegate in a party, then place a delegate in a party."
    english.describe(
        parse<InstructionTree>("PartyDelegate<Scientists> THEN PartyDelegate<Unity>")
    ) shouldBe "Place a delegate in the Scientists party, then place a delegate in Unity."
    english.describe(parse<InstructionTree>("PartyDelegate<Anyone>")) shouldContain "["
  }

  @Test
  internal fun preservesOwnershipWhenRemovingAndReplacingDelegates() {
    english.describe(parse<InstructionTree>("-PartyFollower<Party, Anyone>")) shouldBe
        "Remove a non-leader delegate in a party."
    english.describe(parse<InstructionTree>("-PartyFollower<Party>")) shouldBe
        "Remove a non-leader delegate of your own in a party."
    english.describe(parse<InstructionTree>("-PartyFollower<Party, Neutral>")) shouldBe
        "Remove a neutral non-leader delegate in a party."
    english.describe(
        parse<InstructionTree>("PartyDelegate<@Party> FROM PartyFollower<@Party, Neutral>")
    ) shouldBe
        "Change a neutral non-leader delegate in any party into a delegate of your own in that party."
    english.describe(
        parse<InstructionTree>("PartyDelegate<@Party, Neutral> FROM PartyFollower<@Party>")
    ) shouldBe
        "Change a non-leader delegate of your own in any party into a neutral delegate in that party."
    english.describe(
        parse<InstructionTree>("PartyDelegate<Party> FROM PartyFollower<Party, Neutral>")
    ) shouldBe
        "Change a neutral non-leader delegate in any party into a delegate of your own in a party."
  }

  @Test
  internal fun distinguishesOwnedAndUnrestrictedDelegateRequirements() {
    english.describe(parse<Requirement>("PartyFollower<Party, Anyone>")) shouldBe
        "Requires that there is a non-leader delegate in any party."
    english.describe(parse<Requirement>("PartyFollower<Party>")) shouldBe
        "Requires that you have a non-leader delegate in any party."
    english.describe(parse<Requirement>("PartyFollower<Party, Neutral>")) shouldBe
        "Requires that there is a neutral non-leader delegate in any party."
    english.describe(parse<Requirement>("MAX 0 PartyFollower<Party, Anyone>")) shouldBe
        "Requires that there are 0 non-leader delegates in any party."
    english.describe(parse<Requirement>("MAX 0 PartyFollower<Party>")) shouldBe
        "Requires that you have 0 non-leader delegates in any party."
    english.describe(parse<Requirement>("MAX 0 PartyFollower<Party, Neutral>")) shouldBe
        "Requires that there are 0 neutral non-leader delegates in any party."
  }

  @Test
  internal fun preservesLeaderRolesAcrossOwnership() {
    english.describe(parse<Requirement>("2 PartyLeader")) shouldBe
        "Requires that you lead 2 parties."
    english.describe(parse<Requirement>("PartyLeader<Anyone>")) shouldBe
        "Requires that there is a party leader."
    english.describe(parse<Requirement>("PartyLeader<Neutral>")) shouldBe
        "Requires that there is a neutral party leader."
    english.describe(parse<InstructionTree>("-PartyLeader<Party, Anyone>")) shouldBe
        "Remove a party leader in a party."
  }

  @Test
  internal fun preservesExcludedDelegateKindsAndUnknownFilters() {
    english.describe(parse<Requirement>("7 Delegate(NOT LobbyDelegate)")) shouldBe
        "Requires that you have 7 delegates excluding lobby delegates."
    english.describe(parse<Requirement>("3 Delegate(NOT PartyFollower)")) shouldBe
        "Requires that you have 3 delegates excluding non-leader delegates."
    english.describe(parse<Requirement>("7 Delegate(NOT LobbyDelegate(HAS MC))")) shouldContain "["
  }

  @Test
  internal fun describesPartyRequirement() {
    english.describe(
        parse<Requirement>("Ruling<Scientists> OR 2 PartyDelegate<Scientists>")
    ) shouldBe
        "Requires that either the Scientists party is ruling or you have 2 delegates in the Scientists party."
  }

  @Test
  internal fun describesPreservationResources() {
    english.describe(parse<InstructionTree>("2 Preservation<This>")) shouldBe
        "Add 2 preservation resources to this card."
  }
}
