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
                  { "id": "BM1", "requirement": "35 TerraformRating" },
                ],
                "groups": [{
                  "setupRequirement": "VenusNextExpansion",
                  "milestones": [{ "id": "VM1", "requirement": "3 VenusTag" }],
                }],
              }, {
                "setupRequirement": "UtopiaPlanitiaMapOption",
                "milestones": [
                {
                  "id": "UM1",
                  "setupRequirement": "ColoniesExpansion",
                  "requirement": "3 Colony",
                },
                ],
              }],
            }
            """
        )

    milestones.single { it.className == cn("MilestoneBM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption"
    milestones.single { it.className == cn("MilestoneVM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption, VenusNextExpansion"
    milestones.single { it.className == cn("MilestoneUM1") }.setupRequirement.toString() shouldBe
        "UtopiaPlanitiaMapOption, ColoniesExpansion"
  }
}
