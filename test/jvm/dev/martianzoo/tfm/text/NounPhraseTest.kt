package dev.martianzoo.tfm.text

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class NounPhraseTest {
  @Test
  internal fun placesAttributiveModifiersBeforeTheHead() {
    NounPhrase("production")
        .withAttributiveModifier(NounPhrase.text("energy"))
        .withDeterminer(Determiner.INDEFINITE)
        .linearize() shouldBe "an energy production"
  }
}
