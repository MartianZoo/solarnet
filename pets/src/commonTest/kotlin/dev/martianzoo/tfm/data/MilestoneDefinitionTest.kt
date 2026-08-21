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
                  { "name": "Terraformer35", "requirement": "35 TerraformRating" },
                ],
                "groups": [{
                  "setupRequirement": "VenusNextExpansion",
                  "milestones": [{ "name": "Hoverlord", "requirement": "3 VenusTag" }],
                }],
              }, {
                "setupRequirement": "UtopiaPlanitiaMapOption",
                "milestones": [
                {
                  "name": "Pioneer3",
                  "setupRequirement": "ColoniesExpansion",
                  "requirement": "3 Colony",
                },
                ],
              }],
            }
            """
        )

    val terraformer = milestones.single { it.className == cn("Terraformer35") }
    terraformer.setupRequirement.toString() shouldBe "DemoMapOption"
    terraformer.asClassDeclaration.properties[PropertyName("requirement")] shouldBe
        RequirementValue(terraformer.requirement)
    milestones.single { it.className == cn("Hoverlord") }.setupRequirement.toString() shouldBe
        "DemoMapOption, VenusNextExpansion"
    milestones.single { it.className == cn("Pioneer3") }.setupRequirement.toString() shouldBe
        "UtopiaPlanitiaMapOption, ColoniesExpansion"
  }
}
