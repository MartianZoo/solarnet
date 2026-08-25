package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.data.GameConfig
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
                    TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloVariant,
                    TharsisMap
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
                description = "a named milestone replaces only the map's milestone pool",
                config = "HellasMap, Landshaper",
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
                    VenusNextExpansion, WorldGovernmentOption
                    """,
            ),
            Configuration(
                description = "World Government can be explicitly disabled in a Venus game",
                config = "VenusNextExpansion, -WorldGovernmentOption",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    VenusNextExpansion
                    """,
            ),
            Configuration(
                description = "World Government can be selected without Venus Next",
                config = "WorldGovernmentOption",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    WorldGovernmentOption
                    """,
            ),
            Configuration(
                description = "solo Venus uses neither default milestone nor award pools",
                config = "VenusNextExpansion",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, StandardSoloVariant,
                    TharsisMap, VenusNextExpansion, WorldGovernmentOption
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
                    TharsisMap,
                    PreludeExpansion, Prelude1Deck
                    """,
            ),
            Configuration(
                description = "Prelude 2 implies the Prelude rules and original deck",
                config = "Prelude2Expansion",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude1Deck, Prelude2Expansion
                    """,
            ),
            Configuration(
                description = "Prelude 2 can replace rather than supplement the original deck",
                config = "Prelude2Expansion, -Prelude1Deck",
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, MultiplayerMode,
                    TharsisMap,
                    PreludeExpansion, Prelude2Expansion
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
                config = "Tr63SoloVariant",
                players = 1,
                selectsExactly =
                    """
                    TerraformingMars, CorporateEraExpansion, SoloMode, Tr63SoloVariant,
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
                    "VenusNextExpansion, Coastguard, Landshaper, Botanist, Founder",
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
                description = "the original Prelude deck requires the Prelude rules",
                config = "Prelude1Deck",
            ),
            Rejection(
                description = "Prelude 2 implies Prelude rules, so those rules cannot be excluded",
                config = "Prelude2Expansion, -PreludeExpansion",
            ),
            Rejection(
                description = "Prelude rules require at least one Prelude deck",
                config = "PreludeExpansion, -Prelude1Deck",
            ),
            Rejection(
                description = "Valley Trust requires a viable Prelude deck",
                config = "ValleyTrust",
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
                config = "Tr63SoloVariant",
            ),
            Rejection(
                description = "a solo game cannot select two solo objectives",
                config = "StandardSoloVariant, Tr63SoloVariant",
                players = 1,
            ),
            Rejection(
                description = "a solo game cannot exclude its only objective",
                config = "-StandardSoloVariant",
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
