package dev.martianzoo.engine

import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

/**
 * The one-stop catalog of user-facing configuration interactions.
 *
 * Each valid row shows the exact set of Modules selected after defaults and implications are
 * resolved. Anything omitted from `selectsExactly` is not selected. The rejection table below it
 * records requirements, mutually exclusive choices, and conflicts with explicit exclusions.
 */
internal class ModuleSelectionTest {
  @Test
  fun `valid configurations resolve to exactly these Modules`() {
    val configurations =
        listOf(
            // CORE DEFAULTS
            Configuration(
                description = "default two-player game",
                config = "",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards
                    """,
            ),
            Configuration(
                description = "default solo game",
                config = "",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloVariant,
                    TharsisMap, TharsisDefaultMilestones
                    """,
            ),

            // MAPS AND GOAL POOLS
            Configuration(
                description = "Hellas replaces Tharsis and supplies its goals",
                config = "HellasMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    HellasMap, HellasDefaultMilestones, HellasDefaultAwards
                    """,
            ),
            Configuration(
                description = "Elysium replaces Tharsis and supplies its goals",
                config = "ElysiumMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    ElysiumMap, ElysiumDefaultMilestones, ElysiumDefaultAwards
                    """,
            ),
            Configuration(
                description = "Utopia replaces Tharsis and supplies its goals",
                config = "UtopiaMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    UtopiaMap, UtopiaDefaultMilestones, UtopiaDefaultAwards
                    """,
            ),
            Configuration(
                description = "Cimmeria replaces Tharsis and supplies its goals",
                config = "CimmeriaMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    CimmeriaMap, CimmeriaDefaultMilestones, CimmeriaDefaultAwards
                    """,
            ),
            Configuration(
                description = "a named milestone replaces only the map's milestone pool",
                config = "HellasMap, Landshaper",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    HellasMap, HellasDefaultAwards
                    """,
            ),

            // VENUS NEXT
            Configuration(
                description = "Venus adds its goals and World Government by default",
                config = "VenusNextExpansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    VenusNextExpansion, VenusDefaultMilestones, VenusDefaultAwards,
                    WorldGovernmentOption
                    """,
            ),
            Configuration(
                description = "World Government can be explicitly disabled in a Venus game",
                config = "VenusNextExpansion, -WorldGovernmentOption",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    VenusNextExpansion, VenusDefaultMilestones, VenusDefaultAwards
                    """,
            ),
            Configuration(
                description = "named goals replace every default goal pool",
                config = "VenusNextExpansion, Coastguard, Landshaper, Botanist, Founder",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, VenusNextExpansion, WorldGovernmentOption
                    """,
            ),

            // PRELUDE CARD POOLS
            Configuration(
                description = "Prelude rules include the original Prelude deck by default",
                config = "PreludeExpansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    PreludeExpansion, Prelude1Deck
                    """,
            ),
            Configuration(
                description = "Prelude rules can omit the original Prelude deck",
                config = "PreludeExpansion, -Prelude1Deck",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    PreludeExpansion
                    """,
            ),
            Configuration(
                description = "Prelude 2 implies the Prelude rules and original deck",
                config = "Prelude2Expansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    PreludeExpansion, Prelude1Deck, Prelude2Expansion
                    """,
            ),
            Configuration(
                description = "Prelude 2 can replace rather than supplement the original deck",
                config = "Prelude2Expansion, -Prelude1Deck",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    PreludeExpansion, Prelude2Expansion
                    """,
            ),

            // INDEPENDENT EXPANSIONS AND BASE-GAME VARIANTS
            Configuration(
                description = "Valley Trust does not imply Prelude rules",
                config = "ValleyTrust",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards
                    """,
            ),
            Configuration(
                description = "promotional and Turmoil card packs are independent",
                config = "TurmoilCardPack, PromoCardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    TurmoilCardPack, PromoCardPack
                    """,
            ),
            Configuration(
                description = "Colonies is valid with the required two-player tile pool",
                config = "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede, Io",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards,
                    ColoniesExpansion
                    """,
            ),
            Configuration(
                description = "Corporate Era is a removable base-game default",
                config = "-CorporateEraExpansion",
                selectsExactly =
                    """
                    TerraformingMars, MultiplayerMode,
                    TharsisMap, TharsisDefaultMilestones, TharsisDefaultAwards
                    """,
            ),
            Configuration(
                description = "63 TR replaces the standard solo objective",
                config = "Tr63SoloVariant",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, Tr63SoloVariant,
                    TharsisMap, TharsisDefaultMilestones
                    """,
            ),
        )

    configurations.forEach { configuration ->
      withClue(configuration.description) {
        val premise = premise(configuration.config, configuration.players)
        premise.modules.shouldContainExactlyInAnyOrder(configuration.selectsExactly)
        Engine.newGame(premise)
      }
    }
  }

  @Test
  fun `requirements exclusions and mutually exclusive choices reject these configurations`() {
    val rejections =
        listOf(
            Rejection(
                description = "World Government requires Venus Next",
                config = "WorldGovernmentOption",
            ),
            Rejection(
                description = "the original Prelude deck requires the Prelude rules",
                config = "Prelude1Deck",
            ),
            Rejection(
                description = "Prelude 2 implies Prelude rules, so those rules cannot be excluded",
                config = "Prelude2Expansion, -PreludeExpansion",
            ),
            Rejection(
                description = "a two-player Colonies game requires five colony tiles",
                config = "ColoniesExpansion",
            ),
            Rejection(
                description =
                    "Terraforming Mars implies a default map when no other map is selected",
                config = "-TharsisMap",
            ),
            Rejection(
                description = "player-count modes require Terraforming Mars",
                config = "-TerraformingMars",
            ),
            Rejection(
                description = "a game cannot select two maps",
                config = "HellasMap, ElysiumMap",
            ),
            Rejection(
                description = "a game cannot select both player-count modes",
                config = "SoloMode, MultiplayerMode",
            ),
            Rejection(
                description = "a solo game cannot select two solo objectives",
                config = "StandardSoloVariant, Tr63SoloVariant",
                players = 1,
            ),
        )

    rejections.forEach { rejection ->
      withClue(rejection.description) {
        shouldThrow<IllegalArgumentException> {
          Engine.newGame(premise(rejection.config, rejection.players))
        }
      }
    }
  }

  private data class Configuration(
      val description: String,
      val config: String,
      val players: Int = 2,
      val selectsExactly: Set<ClassName>,
  ) {
    constructor(
        description: String,
        config: String,
        players: Int = 2,
        selectsExactly: String,
    ) : this(description, config, players, names(selectsExactly))
  }

  private data class Rejection(
      val description: String,
      val config: String,
      val players: Int = 2,
  )

  private fun premise(config: String, players: Int) =
      Canon.gamePremise(GameConfig(config, *(1..players).map { "Player$it" }.toTypedArray()))

  private companion object {
    fun names(source: String): Set<ClassName> =
        source.split(Regex("[,\\s]+")).filter(String::isNotEmpty).mapTo(linkedSetOf(), ::cn)
  }
}
