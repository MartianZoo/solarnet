package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreludeCommonEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesPreludeCards() {
    english.describe(parse<InstructionTree>("ProjectCard, PreludeCard")) shouldBe
        "Draw 1 card and 1 Prelude card."
  }

  @Test
  internal fun describesPlayOrFizzleFromItsDeclaredChoice() {
    english.describe(parse<InstructionTree>("PlayOrFizzle")) shouldBe
        "Either play the selected Prelude card or discard it for 15 M€ if it cannot be played."
    english.describe(parse<InstructionTree>("PlayOrFizzle<Selecting>")) shouldBe
        "Either play the selected Prelude card or discard it for 15 M€ if it cannot be played."
  }
}
