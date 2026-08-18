package dev.martianzoo.tfm.language

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
}
