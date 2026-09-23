package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude2CardPackEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesDirectorResources() {
    english.describe(parse<InstructionTree>("-Director<This>")) shouldBe
        "Remove 1 director resource from this card."
    english.describe(
        parse<InstructionTree>(
            "-12 MC THEN -Director<This> THEN PlayCard<Class<PreludeCard>, Hand>"
        )
    ) shouldBe "Pay 12 M€ and remove 1 director resource from this card to play a prelude card."
  }

  @Test
  internal fun describesFocusedOrganizationSignal() {
    english.describe(
        parse<InstructionTree>(
            "-ProjectCard THEN -StandardResource THEN FocusedOrganization_Signal"
        )
    ) shouldBe
        "Discard 1 card and pay 1 standard resource to draw 1 card and gain 1 standard resource."
  }

  @Test
  internal fun describesFrontierTownBonus() {
    english.describe(
        parse<InstructionTree>(
            "FrontierTownBonus, PROD[-Energy], CityTile<> THEN -FrontierTownBonus."
        )
    ) shouldBe
        "Decrease your energy production 1 step. Place a city tile and gain its placement bonus twice."
  }
}
