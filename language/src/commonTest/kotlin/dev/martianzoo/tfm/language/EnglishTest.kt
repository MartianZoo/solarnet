package dev.martianzoo.tfm.language

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
  // This characterization is deliberately the sole wording test for every derivation shape. Do
  // not add shape-specific expected text: the data file is the oracle. The two tests below it only
  // prove that an absent region does not consult that oracle at all.
  @Test
  fun allCardTextMatchesDataFile() {
    EnglishCardTextData.byCardFront.forEach { (cardFront, expected) ->
      withClue(cardFront.toString()) {
        English.topText(cardFront) shouldBe expected.top
        English.bottomText(cardFront) shouldBe expected.bottom
      }
    }
  }

  @Test
  fun describesStandalonePetsElements() {
    English.describe(parse<Effect>("End: VictoryPoint / Animal<This>")) shouldBe
        "1 VP for each animal on this card."
    English.describe(listOf(parse<Action>("4 Energy -> 2 Steel, OxygenStep"))) shouldBe
        "Spend 4 energy to gain 2 steel and raise oxygen 1 step."
    English.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
    English.describe(parse<Requirement>("MAX 6 OxygenStep")) shouldBe "Oxygen must be 6% or less."

    val animalCard =
        CardDefinition(
            CardData(
                id = "AnimalHolder",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                resourceType = "Animal",
            )
        )
    English.describe(parse<InstructionTree>("Animal"), animalCard) shouldBe
        "Add 1 animal to ANY card."
  }

  @Test
  fun noTopTextElementsDoesNotConsultDataFile() {
    val requirementOnly =
        CardDefinition(
            CardData(
                id = "RequirementOnly",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                requirement = "OxygenStep",
            )
        )

    English.topText(requirementOnly) { error("consulted card-text data") } shouldBe ""
  }

  @Test
  fun noBottomTextElementsDoesNotConsultDataFile() {
    val actionOnly =
        CardDefinition(
            CardData(
                id = "ActionOnly",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions = listOf("-> ProjectCard"),
            )
        )

    English.bottomText(actionOnly) { error("consulted card-text data") } shouldBe ""
  }
}
