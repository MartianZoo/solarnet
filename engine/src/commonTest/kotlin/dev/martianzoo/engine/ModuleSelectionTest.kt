package dev.martianzoo.engine

import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import kotlin.test.Test

internal class ModuleSelectionTest {
  @Test
  fun `interesting configurations resolve to the expected Modules`() {
    val scenarios =
        listOf(
            Scenario(
                "base multiplayer",
                "",
                players = 2,
                present =
                    "TerraformingMars, CorporateEraExpansion, MultiplayerMode, TharsisMap, " +
                        "TharsisDefaultMilestones, TharsisDefaultAwards",
                absent = "SoloMode, HellasMap, ElysiumMap, MilestonesAwardsExpansion",
            ),
            Scenario(
                "base solo",
                "",
                players = 1,
                present =
                    "TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloVariant, " +
                        "TharsisMap, TharsisDefaultMilestones",
                absent = "MultiplayerMode, TharsisDefaultAwards, Tr63SoloVariant",
            ),
            Scenario(
                "explicit Hellas map",
                "HellasMap",
                present = "HellasMap, HellasDefaultMilestones, HellasDefaultAwards",
                absent = "TharsisMap, ElysiumMap, TharsisDefaultMilestones, ElysiumDefaultAwards",
            ),
            Scenario(
                "explicit Cimmeria map",
                "CimmeriaMap",
                present = "CimmeriaMap, CimmeriaDefaultMilestones, CimmeriaDefaultAwards",
                absent = "TharsisMap, UtopiaMap, UtopiaDefaultMilestones, UtopiaDefaultAwards",
            ),
            Scenario(
                "Venus defaults",
                "VenusNextExpansion",
                present =
                    "VenusNextExpansion, WorldGovernmentOption, VenusDefaultMilestones, " +
                        "VenusDefaultAwards, TharsisMap",
                absent = "MilestonesAwardsExpansion",
            ),
            Scenario(
                "Venus without World Government Terraforming",
                "VenusNextExpansion, -WorldGovernmentOption",
                present = "VenusNextExpansion, VenusDefaultMilestones, VenusDefaultAwards",
                absent = "WorldGovernmentOption",
            ),
            Scenario(
                "Milestones and Awards exact-pool mode",
                "VenusNextExpansion, MilestonesAwardsExpansion, Coastguard, Landshaper, " +
                    "Botanist, Founder",
                present =
                    "VenusNextExpansion, WorldGovernmentOption, MilestonesAwardsExpansion, " +
                        "TharsisMap",
                absent =
                    "TharsisDefaultMilestones, TharsisDefaultAwards, VenusDefaultMilestones, " +
                        "VenusDefaultAwards",
            ),
            Scenario(
                "Prelude 1",
                "PreludeExpansion",
                present = "PreludeExpansion",
                absent = "Prelude2Expansion",
            ),
            Scenario(
                "Prelude 2 without Prelude 1",
                "Prelude2Expansion",
                present = "Prelude2Expansion",
                absent = "PreludeExpansion",
            ),
            Scenario(
                "Valley Trust without a Prelude deck",
                "ValleyTrust",
                present = "TerraformingMars, TharsisMap",
                absent = "PreludeExpansion, Prelude2Expansion",
            ),
            Scenario(
                "independent card packs",
                "TurmoilCardPack, PromoCardPack",
                present = "TurmoilCardPack, PromoCardPack",
                absent = "PreludeExpansion, VenusNextExpansion, ColoniesExpansion",
            ),
            Scenario(
                "Colonies with a two-player tile pool",
                "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede, Io",
                present = "ColoniesExpansion, TerraformingMars, TharsisMap",
                absent = "VenusNextExpansion, PreludeExpansion, PromoCardPack",
            ),
            Scenario(
                "base game without Corporate Era",
                "-CorporateEraExpansion",
                present = "TerraformingMars, MultiplayerMode, TharsisMap",
                absent = "CorporateEraExpansion, SoloMode",
            ),
            Scenario(
                "63 TR solo variant",
                "Tr63SoloVariant",
                players = 1,
                present = "SoloMode, Tr63SoloVariant, TharsisMap",
                absent = "StandardSoloVariant, MultiplayerMode, TharsisDefaultAwards",
            ),
        )

    scenarios.forEach { scenario ->
      val playerNames = (1..scenario.players).map { cn("Player$it") }
      val modules =
          Canon.gamePremise(GameConfig(scenario.config, *playerNames.map { "$it" }.toTypedArray()))
              .modules
      withClue(scenario.description) {
        modules.shouldContainAll(scenario.present)
        modules.shouldNotContainAnyOf(scenario.absent)
      }
    }
  }

  private data class Scenario(
      val description: String,
      val config: String,
      val players: Int = 2,
      val present: Set<ClassName>,
      val absent: Set<ClassName>,
  ) {
    constructor(
        description: String,
        config: String,
        players: Int = 2,
        present: String,
        absent: String,
    ) : this(description, config, players, names(present), names(absent))
  }

  private companion object {
    fun names(source: String): Set<ClassName> =
        source.split(',').map(String::trim).filter(String::isNotEmpty).mapTo(linkedSetOf(), ::cn)
  }
}
