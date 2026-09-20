package dev.martianzoo.tfm.text

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTextTest {
  @Test
  internal fun keepsCardRegionsStructuredUntilLinearization() {
    val action = EnglishText.Labeled("Action: ", Sentence(clause("gain", "1 plant")).asText().value)
    val effect =
        EnglishText.Labeled(
            "Effect: ",
            Sentence(clause("gain", "1 heat"), punctuation = "!").asText().value,
        )

    EnglishText.join(listOf(action, effect), " / ").linearize() shouldBe
        "Action: Gain 1 plant. / Effect: Gain 1 heat!"
  }

  private fun clause(verb: String, noun: String): Clause.Simple =
      Clause.Simple(Predicate(Verb(verb), Coordination.one(NounPhrase.text(noun))))
}
