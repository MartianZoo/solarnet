package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
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
  internal fun `valid configurations resolve to exactly these Modules`() {
    val configurations =
        listOf(
            // CORE DEFAULTS
            Configuration(
                description = "default two-player game",
                config = "",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap
                    """,
            ),
            Configuration(
                description = "default solo game",
                config = "",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloObjective,
                    TharsisMap
                    """,
            ),
            Configuration(
                description = "extended global parameters can be selected with a standard map",
                config = "ExtendedGlobalParametersRule",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, ExtendedGlobalParametersRule
                    """,
            ),

            // MAPS AND GOAL POOLS
            Configuration(
                description = "Hellas replaces Tharsis and supplies its goals",
                config = "HellasMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    HellasMap
                    """,
            ),
            Configuration(
                description = "Elysium replaces Tharsis and supplies its goals",
                config = "ElysiumMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    ElysiumMap
                    """,
            ),
            Configuration(
                description = "Utopia replaces Tharsis and supplies its goals",
                config = "UtopiaMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    UtopiaMap
                    """,
            ),
            Configuration(
                description = "Cimmeria replaces Tharsis and supplies its goals",
                config = "CimmeriaMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    CimmeriaMap
                    """,
            ),
            Configuration(
                description = "Amazonis enables extended global parameters by default",
                config = "AmazonisMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    AmazonisMap, ExtendedGlobalParametersRule
                    """,
            ),
            Configuration(
                description = "Amazonis allows its extended global parameters to be disabled",
                config = "AmazonisMap, -ExtendedGlobalParametersRule",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    AmazonisMap
                    """,
            ),
            Configuration(
                description = "Vastitas retains the standard global parameters",
                config = "VastitasMap",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    VastitasMap
                    """,
            ),
            Configuration(
                description = "a named milestone replaces only the map's milestone pool",
                config = "HellasMap, Landshaper, Builder, Coastguard",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    HellasMap
                    """,
            ),

            // VENUS NEXT
            Configuration(
                description = "Venus adds its goals and World Government by default",
                config = "VenusNextExpansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    VenusNextExpansion, WorldGovernmentRule
                    """,
            ),
            Configuration(
                description = "World Government can be explicitly disabled in a Venus game",
                config = "VenusNextExpansion, -WorldGovernmentRule",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    VenusNextExpansion
                    """,
            ),
            Configuration(
                description = "Mandatory Venus adds the multiplayer end variant",
                config = "VenusNextExpansion, MandatoryVenusVariant",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    VenusNextExpansion, WorldGovernmentRule, MandatoryVenusVariant
                    """,
            ),
            Configuration(
                description = "World Government can be selected without Venus Next",
                config = "WorldGovernmentRule",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    WorldGovernmentRule
                    """,
            ),
            Configuration(
                description = "solo Venus uses neither default milestone nor award pools",
                config = "VenusNextExpansion",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloObjective,
                    TharsisMap, VenusNextExpansion, WorldGovernmentRule
                    """,
            ),
            Configuration(
                description = "named goals replace every default goal pool",
                config =
                    "VenusNextExpansion, Coastguard, Landshaper, Builder, " +
                        "Botanist, Founder, Administrator",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, VenusNextExpansion, WorldGovernmentRule
                    """,
            ),

            // PRELUDE CARD PACKS AND RULES
            Configuration(
                description = "Prelude 1 rules include the Prelude 1 card pack by default",
                config = "PreludeExpansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude1CardPack
                    """,
            ),
            Configuration(
                description = "the Prelude 1 card pack alone does not enable Prelude 1 rules",
                config = "Prelude1CardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, Prelude1CardPack
                    """,
            ),
            Configuration(
                description = "the Prelude 2 card pack alone does not enable Prelude 1 rules",
                config = "Prelude2CardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, Prelude2CardPack
                    """,
            ),
            Configuration(
                description = "both Prelude card packs can be selected without Prelude 1 rules",
                config = "Prelude1CardPack, Prelude2CardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap, Prelude1CardPack, Prelude2CardPack
                    """,
            ),
            Configuration(
                description = "Prelude 1 rules can add the Prelude 2 card pack directly",
                config = "PreludeExpansion, Prelude2CardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude1CardPack, Prelude2CardPack
                    """,
            ),
            Configuration(
                description = "the Prelude 2 expansion includes its card pack",
                config = "Prelude2Expansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude1CardPack, Prelude2Expansion, Prelude2CardPack
                    """,
            ),
            Configuration(
                description =
                    "Prelude 2 can replace rather than supplement the Prelude 1 card pack",
                config = "Prelude2Expansion, -Prelude1CardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude2Expansion, Prelude2CardPack
                    """,
            ),

            // INDEPENDENT EXPANSIONS AND BASE-GAME VARIANTS
            Configuration(
                description = "promotional and Turmoil card packs are independent",
                config = "TurmoilCardPack, PromoCardPack",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    TurmoilCardPack, PromoCardPack
                    """,
            ),
            Configuration(
                description = "Colonies is valid with the required two-player tile pool",
                config = "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede, Io",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    ColoniesExpansion
                    """,
            ),
            Configuration(
                description = "removing Corporate Era selects Quick Start by default",
                config = "-CorporateEraExpansion",
                selectsExactly =
                    """
                    TerraformingMars, QuickStartVariant, MultiplayerMode,
                    TharsisMap
                    """,
            ),
            Configuration(
                description = "Quick Start can be removed with Corporate Era",
                config = "-CorporateEraExpansion, -QuickStartVariant",
                selectsExactly =
                    """
                    TerraformingMars, MultiplayerMode, TharsisMap
                    """,
            ),
            Configuration(
                description = "Quick Start can be combined with Corporate Era",
                config = "QuickStartVariant",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, QuickStartVariant, MultiplayerMode,
                    TharsisMap
                    """,
            ),
            Configuration(
                description = "63 TR replaces the standard solo objective",
                config = "Tr63SoloObjective",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, Tr63SoloObjective,
                    TharsisMap
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
  internal fun `Venus selects Hoverlord and Venuphile directly unless named goals replace them`() {
    val defaults = Engine.newGame(premise("VenusNextExpansion", 2)).classTable

    defaults.isActive(cn("Hoverlord")) shouldBe true
    defaults.isActive(cn("Venuphile")) shouldBe true

    val namedGoals =
        Engine.newGame(
                premise(
                    "VenusNextExpansion, Coastguard, Landshaper, Builder, " +
                        "Botanist, Founder, Administrator",
                    2,
                )
            )
            .classTable
    namedGoals.isActive(cn("Hoverlord")) shouldBe false
    namedGoals.isActive(cn("Venuphile")) shouldBe false
  }

  @Test
  internal fun `requirements exclusions and mutually exclusive choices reject these configurations`() {
    val rejections =
        listOf(
            Rejection(
                description = "the Prelude 2 expansion constructively includes its card pack",
                config = "Prelude2Expansion, -Prelude2CardPack",
            ),
            Rejection(
                description = "the Prelude 2 expansion constructively includes the Prelude 1 rules",
                config = "Prelude2Expansion, -PreludeExpansion",
            ),
            Rejection(
                description = "Prelude 1 rules require at least one Prelude card pack",
                config = "PreludeExpansion, -Prelude1CardPack",
            ),
            Rejection(
                description = "a two-player Colonies game rejects one fewer than five tiles",
                config = "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede",
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
                description = "two players cannot force solo mode by excluding multiplayer mode",
                config = "SoloMode, -MultiplayerMode",
            ),
            Rejection(
                description = "one player cannot force multiplayer mode by excluding solo mode",
                config = "MultiplayerMode, -SoloMode",
                players = 1,
            ),
            Rejection(
                description = "a multiplayer game cannot select a solo objective",
                config = "Tr63SoloObjective",
            ),
            Rejection(
                description = "mandatory Venus is multiplayer-only",
                config = "VenusNextExpansion, MandatoryVenusVariant",
                players = 1,
            ),
            Rejection(
                description = "a solo game cannot select two solo objectives",
                config = "StandardSoloObjective, Tr63SoloObjective",
                players = 1,
            ),
            Rejection(
                description = "a solo game cannot exclude its only objective",
                config = "-StandardSoloObjective",
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
