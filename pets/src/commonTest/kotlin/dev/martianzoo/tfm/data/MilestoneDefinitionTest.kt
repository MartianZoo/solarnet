package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestoneDefinitionTest {
  @Test
  fun nestedGroupAndIndividualSetupRequirementsAreCombined() {
    val milestones =
        JsonReader.readMilestones(
            """
            {
              "groups": [{
                "setupRequirement": "DemoMapOption",
                "milestones": [
                  { "id": "MM1", "requirement": "35 TerraformRating" },
                ],
                "groups": [{
                  "setupRequirement": "VenusNextExpansion",
                  "milestones": [{ "id": "VM1", "requirement": "3 VenusTag" }],
                }],
              }, {
                "setupRequirement": "UtopiaPlanitiaMapOption",
                "milestones": [
                {
                  "id": "UM2",
                  "setupRequirement": "ColoniesExpansion",
                  "requirement": "3 Colony",
                },
                ],
              }],
            }
            """
        )

    milestones.single { it.shortName == cn("MM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption"
    milestones.single { it.shortName == cn("VM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption, VenusNextExpansion"
    milestones.single { it.shortName == cn("UM2") }.setupRequirement.toString() shouldBe
        "UtopiaPlanitiaMapOption, ColoniesExpansion"
  }
}
