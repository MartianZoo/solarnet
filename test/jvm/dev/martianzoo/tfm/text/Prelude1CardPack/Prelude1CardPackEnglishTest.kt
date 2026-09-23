package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude1CardPackEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesPrintedIconRequirements() {
    english.describe(
        parse<Effect>("CardFront(HAS NonNegativeIconsOf<Class<VictoryPoint>>): 3 MC")
    ) shouldBe "When you play a card with a VP icon, gain 3 M€."
  }
}
