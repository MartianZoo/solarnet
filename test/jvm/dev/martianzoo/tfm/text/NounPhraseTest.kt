package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
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

  @Test
  internal fun keepsPhraseLevelRefusalsOpaqueAndSelfReporting() {
    val unresolved =
        Unresolved(cn("UnsupportedMetric").expression, RefusalReason.UNSUPPORTED_METRIC)
    val phrase =
        NounPhrase.rawPets(unresolved)
            .withDeterminer(Determiner.MOST)
            .quantified(3)
            .withModifier(Modifier.Phrase("combined"))

    phrase.linearize() shouldBe "[UnsupportedMetric]"
    phrase.unresolved() shouldBe listOf(unresolved)
  }
}
