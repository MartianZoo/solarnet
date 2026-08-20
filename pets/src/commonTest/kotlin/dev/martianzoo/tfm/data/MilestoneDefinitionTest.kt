package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
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

    val terraformer = milestones.single { it.className == cn("MilestoneBM1") }
    terraformer.setupRequirement.toString() shouldBe "DemoMapOption"
    terraformer.asClassDeclaration.properties[PropertyName("requirement")] shouldBe
        RequirementValue(terraformer.requirement)
    milestones.single { it.className == cn("MilestoneVM1") }.setupRequirement.toString() shouldBe
        "DemoMapOption, VenusNextExpansion"
    milestones.single { it.className == cn("MilestoneUM1") }.setupRequirement.toString() shouldBe
        "UtopiaPlanitiaMapOption, ColoniesExpansion"
  }
}
