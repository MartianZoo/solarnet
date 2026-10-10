package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilCardPackEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesPreservationResources() {
    english.describe(parse<InstructionTree>("2 Preservation<This>")) shouldBe
        "Add 2 preservation resources to this card."
  }
}
