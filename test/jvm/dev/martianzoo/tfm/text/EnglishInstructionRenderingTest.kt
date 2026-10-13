package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishInstructionRenderingTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun preservesCapOrderAndResourceRetention() {
    english.describe(parse<InstructionTree>("-2 MC. / (BuildingTag MAX 3 - Influence)")) shouldBe
        "Remove 2 M€ per building tag you have (max 3) in excess of influence, or as much as possible."
    english.describe(parse<InstructionTree>("-2 MC. / (BuildingTag - Influence) MAX 3")) shouldBe
        "Remove 2 M€ per building tag you have in excess of influence (max 3), or as much as possible."
    english.describe(parse<InstructionTree>("-Steel! / (Steel - Influence - 2)")) shouldBe
        "Remove all steel except 2 plus influence."
    english.describe(parse<InstructionTree>("-2 Steel! / Steel")) shouldBe
        "Remove 2 steel per steel you have."
    english.describe(parse<InstructionTree>("-Steel? / Steel")) shouldBe "[-Steel? / Steel]."
    english.describe(parse<InstructionTree>("2 MC / (BuildingTag - (Influence - 2))")) shouldBe
        "[2 MC / BuildingTag - (Influence - 2)]."
  }

  @Test
  internal fun distinguishesThresholdRewardsFromRepeatedSets() {
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 1")) shouldBe
        "If you have at least 3 Earth tags and influence combined, gain 5 M€."
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 2")) shouldBe
        "Gain 5 M€ per complete set of 3 Earth tags and influence combined (max 2)."
  }
}
