package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilExpansionEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesRepeatedDelegatePlacements() {
    english.describe(parse<InstructionTree>("PartyDelegate, PartyDelegate")) shouldBe
        "Place a delegate. Place a delegate."
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
