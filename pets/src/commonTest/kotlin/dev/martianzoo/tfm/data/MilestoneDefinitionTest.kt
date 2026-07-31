package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestoneDefinitionTest {
  @Test
  fun groupAndIndividualSetupRequirementsAreCombined() {
    val milestones =
        JsonReader.readMilestones(
            """
            {
              "setupRequirement": "DemoMapOption",
              "milestones": [
                { "id": "MM1", "requirement": "35 TerraformRating" },
                {
                  "id": "UM2",
                  "setupRequirement": "ColoniesExpansion",
                  "requirement": "3 Colony",
                },
              ],
            }
            """
        )

    milestones.single { it.shortName == cn("MM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption"
    milestones.single { it.shortName == cn("UM2") }.setupRequirement.toString() shouldBe
        "DemoMapOption, ColoniesExpansion"
  }
}
