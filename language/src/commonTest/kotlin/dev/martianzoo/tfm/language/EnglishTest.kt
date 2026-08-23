package dev.martianzoo.tfm.language

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
  private val english = English(TerraformingMarsDescribers.descriptions)
  private val cardsByClassName = Canon.cardDefinitions.associateBy { it.className }

  // This fallible golden characterization is deliberately the sole wording test for every
  // derivation shape. Do not add shape-specific expected text. The two tests below it only prove
  // that an absent region does not consult the golden data at all.
  @Test
  internal fun allCardTextMatchesDataFile() {
    EnglishCardTextData.byCardFront.forEach { (cardFront, expected) ->
      withClue(cardFront.toString()) {
        val card = cardsByClassName[cardFront]
        val top = card?.let { english.topText(it) { expected.top } } ?: expected.top
        val bottom = card?.let { english.bottomText(it) { expected.bottom } } ?: expected.bottom
        top shouldBe expected.top
        bottom shouldBe expected.bottom
      }
    }
  }

  @Test
  internal fun describesStandalonePetsElements() {
    english.describe(parse<Effect>("End: VictoryPoint / Animal<This>")) shouldBe
        "1 VP for each animal on this card."
    english.describe(listOf(parse<Action>("4 Energy -> 2 Steel, OxygenStep"))) shouldBe
        "Spend 4 energy to gain 2 steel and raise oxygen 1 step."
    english.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
    english.describe(parse<Requirement>("MAX 6 OxygenStep")) shouldBe "Oxygen must be 6% or less."

    val animalCard =
        CardDefinition(
            CardData(
                name = "AnimalHolder",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                resourceType = "Animal",
            )
        )
    english.describe(parse<InstructionTree>("Animal"), animalCard) shouldBe
        "Add 1 animal to any card."
  }

  @Test
  internal fun noTopTextElementsDoesNotConsultDataFile() {
    val requirementOnly =
        CardDefinition(
            CardData(
                name = "RequirementOnly",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                requirement = "OxygenStep",
            )
        )

    english.topText(requirementOnly) { error("consulted card-text data") } shouldBe ""
  }

  @Test
  internal fun noBottomTextElementsDoesNotConsultDataFile() {
    val actionOnly =
        CardDefinition(
            CardData(
                name = "ActionOnly",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions = listOf("-> ProjectCard"),
            )
        )

    english.bottomText(actionOnly) { error("consulted card-text data") } shouldBe ""
  }
}
