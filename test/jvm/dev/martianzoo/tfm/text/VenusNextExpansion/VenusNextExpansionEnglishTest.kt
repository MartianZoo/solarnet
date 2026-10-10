package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VenusNextExpansionEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesVenusSteps() {
    english.describe(listOf(parse<Action>("1 MC / (12 - VenusTag) -> VenusStep"))) shouldBe
        "Spend 12 M€ to raise Venus 1 step. This cost is reduced by 1 M€ per Venus tag you have."
    english.describe(parse<Requirement>("MAX 6 VenusStep")) shouldBe "Requires Venus 12% or lower."
  }
}
