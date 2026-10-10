package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VastitasMapEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesLargestTileGroupMetric() {
    english.describe(parse<InstructionTree>("2 MC / TileInLargestGroup")) shouldBe
        "Gain 2 M€ per tile in your largest connected group of tiles."
  }
}
