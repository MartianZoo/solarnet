package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardDefinitionTest {
  @Test
  fun groupAndIndividualSetupRequirementsAreCombined() {
    val awards =
        JsonReader.readAwards(
            """
            {
              "groups": [{
                "setupRequirement": "DemoMapOption",
                "awards": [
                  { "id": "HA1", "replaces": "BA1", "metric": "TerraformRating" },
                  {
                    "id": "HA2",
                    "metric": "VenusTag",
                    "setupRequirement": "VenusNextExpansion",
                  },
                ],
              }],
            }
            """
        )

    val replacement = awards.single { it.className == cn("AwardHA1") }
    replacement.replaces shouldBe "BA1"
    replacement.setupRequirement.toString() shouldBe "MAX 0 SoloMode, DemoMapOption"
    awards.single { it.className == cn("AwardHA2") }.setupRequirement.toString() shouldBe
        "MAX 0 SoloMode, DemoMapOption, VenusNextExpansion"
  }
}
