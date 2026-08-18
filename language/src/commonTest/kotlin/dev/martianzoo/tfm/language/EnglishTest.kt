package dev.martianzoo.tfm.language

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
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
