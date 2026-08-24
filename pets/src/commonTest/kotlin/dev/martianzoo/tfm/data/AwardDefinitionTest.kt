package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardDefinitionTest {
  @Test
  internal fun definitionComesFromPetsPropertiesAndSuperclassGroup() {
    val declarations =
        parseClasses(
                """
                ABSTRACT CLASS Award { metric = Metric }
                ABSTRACT CLASS DemoAward : Award {
                  CLASS Visionary { metric = COUNT "CARDS[ProjectCard<Hand>]" }
                }
                """
                    .trimIndent()
            )
            .associateBy { it.className }
    val visionary =
        AwardDefinition.fromClassDeclaration(
            declarations.getValue(cn("Visionary")),
            cn("DemoAward"),
        )

    visionary.selectionGroup shouldBe cn("DemoAward")
    visionary.automaticSelectionRequirement.toString() shouldBe "MultiplayerMode"
    visionary.metric.toString() shouldBe "CARDS[ProjectCard<Hand>]"
    visionary.asClassDeclaration.properties[PropertyName("metric")] shouldBe
        MetricValue(parse<Metric>("ProjectCard"))
  }
}
