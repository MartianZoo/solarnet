package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardDefinitionTest {
  @Test
  internal fun groupAndIndividualAutomaticSelectionRequirementsAreCombined() {
    val awards =
        JsonReader.readAwards(
            """
            {
              "groups": [{
                "group": "DemoDefaultAwards",
                "automaticSelectionRequirement": "DemoMap",
                "awards": [
                  { "name": "Magnate", "replaces": "Landlord", "metric": "TerraformRating" },
                  {
                    "name": "SpaceBaron",
                    "metric": "VenusTag",
                    "automaticSelectionRequirement": "VenusNextExpansion",
                  },
                  { "name": "Visionary", "metric": "CARDS[ProjectCard<Hand>]" },
                  { "name": "Promoter", "metric": "CARDS[CardBack<EventPile>]" },
                ],
              }],
            }
            """
        )

    val replacement = awards.single { it.className == cn("Magnate") }
    replacement.replaces shouldBe cn("Landlord")
    replacement.selectionGroup shouldBe cn("DemoDefaultAwards")
    replacement.automaticSelectionRequirement.toString() shouldBe "MultiplayerMode, DemoMap"
    replacement.asClassDeclaration.properties[PropertyName("metric")] shouldBe
        MetricValue(replacement.metric)
    awards
        .single { it.className == cn("Visionary") }
        .also { it.metric.toString() shouldBe "CARDS[ProjectCard<Hand>]" }
        .asClassDeclaration
        .properties[PropertyName("metric")] shouldBe MetricValue(parse<Metric>("ProjectCard"))
    awards
        .single { it.className == cn("Promoter") }
        .asClassDeclaration
        .properties[PropertyName("metric")] shouldBe MetricValue(parse<Metric>("PlayedEvent"))
    awards
        .single { it.className == cn("SpaceBaron") }
        .automaticSelectionRequirement
        .toString() shouldBe "MultiplayerMode, (DemoMap, VenusNextExpansion)"
  }
}
