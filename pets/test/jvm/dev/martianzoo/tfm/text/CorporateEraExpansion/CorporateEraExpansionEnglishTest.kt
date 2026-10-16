package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CorporateEraExpansionEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesFighterResources() {
    english.describe(parse<InstructionTree>("2 Fighter<This>")) shouldBe
        "Add 2 fighter resources to this card."
  }
}
