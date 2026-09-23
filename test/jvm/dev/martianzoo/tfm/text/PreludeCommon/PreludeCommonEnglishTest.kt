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
        "Draw 1 card and 1 prelude card."
  }
}
