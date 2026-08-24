package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestoneDefinitionTest {
  @Test
  internal fun definitionComesFromPetsPropertiesAndSuperclassGroup() {
    val declarations =
        parseClasses(
                """
                ABSTRACT CLASS Milestone { requirement = Requirement }
                ABSTRACT CLASS DemoMilestone : Milestone {
                  CLASS Planner {
                    HAS DemoMap
                    requirement = HAS "CARDS[16 ProjectCard<Hand>]"
                  }
                }
                """
                    .trimIndent()
            )
            .associateBy { it.className }
    val planner =
        MilestoneDefinition.fromClassDeclaration(
            declarations.getValue(cn("Planner")),
            cn("DemoMilestone"),
        )

    planner.selectionGroup shouldBe cn("DemoMilestone")
    planner.automaticSelectionRequirement.toString() shouldBe "DemoMap"
    planner.requirement.toString() shouldBe "CARDS[16 ProjectCard<Hand>]"
    planner.asClassDeclaration.properties[PropertyName("requirement")] shouldBe
        RequirementValue(parse<Requirement>("16 ProjectCard"))
  }
}
