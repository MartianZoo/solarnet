package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestoneDefinitionTest {
  @Test
  internal fun nestedGroupAndIndividualAutomaticSelectionRequirementsAreCombined() {
    val milestones =
        JsonReader.readMilestones(
            """
            {
              "groups": [{
                "group": "DemoDefaultMilestones",
                "automaticSelectionRequirement": "DemoMap",
                "milestones": [
                  { "name": "Terraformer35", "requirement": "35 TerraformRating" },
                ],
                "groups": [{
                  "automaticSelectionRequirement": "VenusNextExpansion",
                  "milestones": [{ "name": "Hoverlord", "requirement": "3 VenusTag" }],
                }],
              }, {
                "automaticSelectionRequirement": "Utopia",
                "milestones": [
                {
                  "name": "Pioneer3",
                  "automaticSelectionRequirement": "ColoniesExpansion",
                  "requirement": "3 Colony",
                },
                ],
              }],
            }
            """
        )

    val terraformer = milestones.single { it.className == cn("Terraformer35") }
    terraformer.selectionGroup shouldBe cn("DemoDefaultMilestones")
    terraformer.automaticSelectionRequirement.toString() shouldBe "DemoMap"
    terraformer.asClassDeclaration.properties[PropertyName("requirement")] shouldBe
        RequirementValue(terraformer.requirement)
    milestones
        .single { it.className == cn("Hoverlord") }
        .also { it.selectionGroup shouldBe cn("DemoDefaultMilestones") }
        .automaticSelectionRequirement
        .toString() shouldBe "DemoMap, VenusNextExpansion"
    milestones
        .single { it.className == cn("Pioneer3") }
        .automaticSelectionRequirement
        .toString() shouldBe "Utopia, ColoniesExpansion"
  }
}
