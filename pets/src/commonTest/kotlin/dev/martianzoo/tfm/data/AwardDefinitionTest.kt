package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
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
                  { "name": "Magnate", "replaces": "Landlord", "metric": "TerraformRating" },
                  {
                    "name": "SpaceBaron",
                    "metric": "VenusTag",
                    "setupRequirement": "VenusNextExpansion",
                  },
                ],
              }],
            }
            """
        )

    val replacement = awards.single { it.className == cn("Magnate") }
    replacement.replaces shouldBe cn("Landlord")
    replacement.setupRequirement.toString() shouldBe "MAX 0 SoloMode, DemoMapOption"
    replacement.asClassDeclaration.properties[PropertyName("metric")] shouldBe
        MetricValue(replacement.metric)
    awards.single { it.className == cn("SpaceBaron") }.setupRequirement.toString() shouldBe
        "MAX 0 SoloMode, DemoMapOption, VenusNextExpansion"
  }
}
