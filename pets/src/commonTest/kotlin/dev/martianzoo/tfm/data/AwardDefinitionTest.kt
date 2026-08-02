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
                  { "id": "HA1", "metric": "TerraformRating" },
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

    awards.single { it.shortName == cn("HA1") }.setupRequirement.toString() shouldBe "DemoMapOption"
    awards.single { it.shortName == cn("HA2") }.setupRequirement.toString() shouldBe
        "DemoMapOption, VenusNextExpansion"
  }
}
