package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.GamePremise
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The one-stop catalog of user-facing configuration interactions. */
internal class ModuleSelectionTest {
  @Test
  internal fun `unmentioned choices select their defaults`() {
    resolvesToExactly("", defaultMultiplayer)
    resolvesToExactly("", defaultSolo, players = 1)
  }

  @Test
  internal fun `selecting A selects B by default but B may be excluded`() {
    defaultMayBeExcluded(
        default = "CorporateEraExpansion",
        whenSelecting = "TerraformingMars",
        selectsExactly = defaultMultiplayer,
        excludingItSelectsExactly =
            defaultMultiplayer - cn("CorporateEraExpansion") + cn("QuickStartVariant"),
    )
    defaultMayBeExcluded(
        default = "ExtendedGlobalParametersRule",
        whenSelecting = "AmazonisMap",
        selectsExactly = multiplayerOn("AmazonisMap", "ExtendedGlobalParametersRule"),
        excludingItSelectsExactly = multiplayerOn("AmazonisMap"),
    )
    defaultMayBeExcluded(
        default = "WorldGovernmentRule",
        whenSelecting = "VenusNextExpansion",
        selectsExactly = multiplayerWith("VenusNextExpansion", "WorldGovernmentRule"),
        excludingItSelectsExactly = multiplayerWith("VenusNextExpansion"),
    )
    defaultMayBeExcluded(
        default = "Prelude1CardPack",
        whenSelecting = "PreludeExpansion",
        selectsExactly = multiplayerWith("PreludeExpansion", "Prelude1CardPack"),
        excludingItSelectsExactly = multiplayerWith("PreludeExpansion"),
    )
    defaultMayBeExcluded(
        default = "QuickStartVariant",
        whenSelecting = "-CorporateEraExpansion",
        selectsExactly = defaultMultiplayer - cn("CorporateEraExpansion") + cn("QuickStartVariant"),
        excludingItSelectsExactly = defaultMultiplayer - cn("CorporateEraExpansion"),
    )
  }

  @Test
  internal fun `Corporate Era and Quick Start remain optional in solo`() {
    val soloQuickStart = defaultSolo - cn("CorporateEraExpansion") + cn("QuickStartVariant")
    defaultMayBeExcluded(
        default = "CorporateEraExpansion",
        whenSelecting = "TerraformingMars",
        selectsExactly = defaultSolo,
        excludingItSelectsExactly = soloQuickStart,
        players = 1,
    )
    defaultMayBeExcluded(
        default = "QuickStartVariant",
        whenSelecting = "-CorporateEraExpansion",
        selectsExactly = soloQuickStart,
        excludingItSelectsExactly = defaultSolo - cn("CorporateEraExpansion"),
        players = 1,
    )
  }

  @Test
  internal fun `selecting a competing choice prevents a fallback default`() {
    listOf("HellasMap", "ElysiumMap", "UtopiaMap", "CimmeriaMap").forEach { map ->
      selectionPreventsDefault(
          selection = map,
          default = "TharsisMap",
          selectsExactly = multiplayerOn(map),
      )
    }
    selectionPreventsDefault(
        selection = "Tr63SoloObjective",
        default = "StandardSoloObjective",
        selectsExactly = defaultSolo - cn("StandardSoloObjective") + cn("Tr63SoloObjective"),
        players = 1,
    )
  }

  @Test
  internal fun `related options remain independently selectable`() {
    resolvesToExactly(
        "ExtendedGlobalParametersRule",
        multiplayerWith("ExtendedGlobalParametersRule"),
    )
    resolvesToExactly("WorldGovernmentRule", multiplayerWith("WorldGovernmentRule"))

    resolvesToExactly("Prelude1CardPack", multiplayerWith("Prelude1CardPack"))
    resolvesToExactly("Prelude2CardPack", multiplayerWith("Prelude2CardPack"))
    resolvesToExactly(
        "Prelude1CardPack, Prelude2CardPack",
        multiplayerWith("Prelude1CardPack", "Prelude2CardPack"),
    )

    resolvesToExactly("TurmoilExpansion", multiplayerWith("TurmoilExpansion"))
    resolvesToExactly("QuickStartVariant", multiplayerWith("QuickStartVariant"))
  }

  @Test
  internal fun `expansions remain independently selectable without Corporate Era`() {
    val base = defaultMultiplayer - cn("CorporateEraExpansion") + cn("QuickStartVariant")
    mapOf(
            "VenusNextExpansion" to names("VenusNextExpansion, WorldGovernmentRule"),
            "PreludeExpansion" to names("PreludeExpansion, Prelude1CardPack"),
            "ColoniesExpansion" to names("ColoniesExpansion"),
            "TurmoilExpansion" to names("TurmoilExpansion"),
        )
        .forEach { (expansion, additions) ->
          resolvesToExactly("$expansion, -CorporateEraExpansion", base + additions)
        }
  }

  @Test
  internal fun `TR 63 solo composes with each expansion`() {
    val base = defaultSolo - cn("StandardSoloObjective") + cn("Tr63SoloObjective")
    mapOf(
            "VenusNextExpansion" to names("VenusNextExpansion, WorldGovernmentRule"),
            "PreludeExpansion" to names("PreludeExpansion, Prelude1CardPack"),
            "ColoniesExpansion" to names("ColoniesExpansion"),
            "TurmoilExpansion" to names("TurmoilExpansion"),
        )
        .forEach { (expansion, additions) ->
          resolvesToExactly("$expansion, Tr63SoloObjective", base + additions, players = 1)
        }
  }

  @Test
  internal fun `optional content accepts standard or nonstandard pools`() {
    resolvesToExactly(
        "PreludeExpansion, Prelude2CardPack",
        multiplayerWith("PreludeExpansion", "Prelude1CardPack", "Prelude2CardPack"),
    )
    resolvesToExactly(
        "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
        multiplayerWith("PreludeExpansion", "Prelude2CardPack"),
    )

    resolvesToExactly(
        "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede, Io",
        multiplayerWith("ColoniesExpansion"),
    )
    resolvesToExactly(
        "ColoniesExpansion, Callisto, Ceres, Europa, Ganymede",
        multiplayerWith("ColoniesExpansion"),
    )
  }

  @Test
  internal fun `explicit variants add only themselves`() {
    resolvesToExactly(
        "VenusNextExpansion, MandatoryVenusVariant",
        multiplayerWith(
            "VenusNextExpansion",
            "WorldGovernmentRule",
            "MandatoryVenusVariant",
        ),
    )
  }

  @Test
  internal fun `adding or excluding a redundant selection is harmless`() {
    addingIsHarmless("TerraformingMars", to = "")

    excludingIsHarmless("QuickStartVariant", from = "")
    excludingIsHarmless("MandatoryVenusVariant", from = "")
    excludingIsHarmless("MandatoryVenusVariant", from = "VenusNextExpansion")
    excludingIsHarmless(
        "MandatoryVenusVariant",
        from = "VenusNextExpansion",
        players = 1,
    )
  }

  @Test
  internal fun `named goals define exact pools for their own category`() {
    resolvesToExactly(
        "HellasMap, Landshaper, Builder, Coastguard, Terraformer",
        multiplayerOn("HellasMap"),
    )
    resolvesToExactly(
        "VenusNextExpansion, Coastguard, Landshaper, Builder, Terraformer, " +
            "Botanist, Founder, Administrator, Banker",
        multiplayerWith("VenusNextExpansion", "WorldGovernmentRule"),
    )

    val defaults = classTable("VenusNextExpansion")
    defaults.isInhabited(cn("Hoverlord")) shouldBe true
    defaults.isInhabited(cn("Venuphile")) shouldBe true

    val namedMilestones =
        classTable("VenusNextExpansion, Coastguard, Landshaper, Builder, Terraformer")
    namedMilestones.isInhabited(cn("Hoverlord")) shouldBe false
    namedMilestones.isInhabited(cn("Venuphile")) shouldBe true

    val namedAwards = classTable("VenusNextExpansion, Botanist, Founder, Administrator, Banker")
    namedAwards.isInhabited(cn("Hoverlord")) shouldBe true
    namedAwards.isInhabited(cn("Venuphile")) shouldBe false

    resolvesToExactly(
        "VenusNextExpansion",
        defaultSolo + cn("VenusNextExpansion") + cn("WorldGovernmentRule"),
        players = 1,
    )
    val solo = classTable("VenusNextExpansion", players = 1)
    solo.isInhabited(cn("Hoverlord")) shouldBe false
    solo.isInhabited(cn("Venuphile")) shouldBe false
  }

  @Test
  internal fun `expansion-sensitive milestones join only compatible default pools`() {
    val cimmeria = classTable("CimmeriaMap")
    cimmeria.isInhabited(cn("Planetologist")) shouldBe false
    cimmeria.isInhabited(cn("Hoverlord")) shouldBe false

    val cimmeriaWithVenus = classTable("CimmeriaMap, VenusNextExpansion")
    cimmeriaWithVenus.isInhabited(cn("Planetologist")) shouldBe true
    cimmeriaWithVenus.isInhabited(cn("Hoverlord")) shouldBe true
  }

  @Test
  internal fun `requirements and mutually exclusive choices reject configurations`() {
    configurationRejects("-TharsisMap")
    configurationRejects("-TerraformingMars")

    cannotSelectTogether("HellasMap", "ElysiumMap")
    cannotSelectTogether("TharsisMap", "HellasMap")
    cannotSelectTogether("SoloMode", "MultiplayerMode")
    cannotSelectTogether("StandardSoloObjective", "Tr63SoloObjective", players = 1)

    rejects("SoloMode, -MultiplayerMode")
    rejects("MultiplayerMode, -SoloMode", players = 1)
    configurationRejects("Tr63SoloObjective")
    rejects("-StandardSoloObjective", players = 1)

    rejects("Landlord", players = 1)
    rejects("VenusNextExpansion, MandatoryVenusVariant", players = 1)

    rejects("Callisto")
    configurationRejects("HellasMap, Geologist")
    configurationRejects("UtopiaMap, Geologist")
  }

  @Test
  internal fun `explicit milestones are selectable outside their default pools`() {
    classTable("TharsisMap, PolarExplorer").isInhabited(cn("PolarExplorer")) shouldBe true
    classTable("ElysiumMap, Generalist2").isInhabited(cn("Generalist2")) shouldBe true
    classTable("VenusNextExpansion, Hoverlord", players = 1).isInhabited(cn("Hoverlord")) shouldBe
        true
  }

  @Test
  internal fun `Amazonis excludes Mining Guild unless Unsafe is selected`() {
    classTable("AmazonisMap").isInhabited(cn("MiningGuild")) shouldBe false
    rejects("AmazonisMap, MiningGuild")
    classTable("AmazonisMap, Unsafe").isInhabited(cn("MiningGuild")) shouldBe true
  }

  @Test
  internal fun `Amazonis excludes Mining Rights unless Unsafe is selected`() {
    classTable("AmazonisMap").isInhabited(cn("MiningRights")) shouldBe false
    rejects("AmazonisMap, MiningRights")
    classTable("AmazonisMap, Unsafe").isInhabited(cn("MiningRights")) shouldBe true
  }

  @Test
  internal fun `Amazonis excludes Mining Area unless Unsafe is selected`() {
    classTable("AmazonisMap").isInhabited(cn("MiningArea")) shouldBe false
    rejects("AmazonisMap, MiningArea")
    classTable("AmazonisMap, Unsafe").isInhabited(cn("MiningArea")) shouldBe true
  }

  @Test
  internal fun `Terraforming Deal stays in the normal Prelude 2 pool`() {
    val safe = classTable("Prelude2CardPack, TurmoilExpansion")
    safe.isInhabited(cn("TerraformingDeal")) shouldBe true
    safe.isInhabited(cn("PreservationProgram")) shouldBe false

    rejects("Prelude2CardPack, PreservationProgram")
    rejects("TurmoilExpansion, PreservationProgram")
    classTable("Prelude2CardPack, PreservationProgram, -TerraformingDeal")
        .isInhabited(cn("PreservationProgram")) shouldBe true
    classTable("Prelude2CardPack, TurmoilExpansion, Unsafe")
        .isInhabited(cn("PreservationProgram")) shouldBe true
  }

  @Test
  internal fun `Merger takes precedence when Sagitta is also available`() {
    val normal = classTable("PromoCardPack, Prelude2CardPack")
    normal.isInhabited(cn("Merger")) shouldBe true
    normal.isInhabited(cn("SagittaFrontierServices")) shouldBe false

    val explicitSagitta = classTable("PromoCardPack, Prelude2CardPack, SagittaFrontierServices")
    explicitSagitta.isInhabited(cn("SagittaFrontierServices")) shouldBe true
    explicitSagitta.isInhabited(cn("Merger")) shouldBe false

    rejects("PromoCardPack, Prelude2CardPack, Merger, SagittaFrontierServices")
    val unsafe = classTable("PromoCardPack, Prelude2CardPack, Unsafe")
    unsafe.isInhabited(cn("Merger")) shouldBe true
    unsafe.isInhabited(cn("SagittaFrontierServices")) shouldBe true
  }

  @Test
  internal fun `Ecology Experts is absent from normal Prelude 1 selection`() {
    classTable("Prelude1CardPack").isInhabited(cn("EcologyExperts")) shouldBe false
    classTable("Prelude1CardPack, Unsafe").isInhabited(cn("EcologyExperts")) shouldBe false
    classTable("Prelude1CardPack, EcologyExperts, Unsafe")
        .isInhabited(cn("EcologyExperts")) shouldBe true
  }

  @Test
  internal fun `Ecology Experts and Viral Enhancers require Unsafe`() {
    rejects("Prelude1CardPack, EcologyExperts, -EcologicalZone, -Decomposers, -GmoContract")
  }

  @Test
  internal fun `Ecology Experts and Ecological Zone require Unsafe`() {
    rejects("Prelude1CardPack, EcologyExperts, -ViralEnhancers, -Decomposers, -GmoContract")
  }

  @Test
  internal fun `Ecology Experts and Decomposers require Unsafe`() {
    rejects("Prelude1CardPack, EcologyExperts, -ViralEnhancers, -EcologicalZone, -GmoContract")
  }

  @Test
  internal fun `Ecology Experts and GMO Contract require Unsafe`() {
    rejects(
        "Prelude1CardPack, TurmoilExpansion, EcologyExperts, -ViralEnhancers, " +
            "-EcologicalZone, -Decomposers"
    )
  }

  @Test
  internal fun `Ecology Experts is usable when all four listeners are absent`() {
    classTable(
            "Prelude1CardPack, EcologyExperts, -ViralEnhancers, -EcologicalZone, " +
                "-Decomposers, -GmoContract"
        )
        .isInhabited(cn("EcologyExperts")) shouldBe true
  }

  @Test
  internal fun `Thawer and Snow Cover are compatible in the normal pool`() {
    val normal = classTable("TurmoilExpansion, Thawer, Builder, Engineer")
    normal.isInhabited(cn("Thawer")) shouldBe true
    normal.isInhabited(cn("SnowCover")) shouldBe true
  }

  @Test
  internal fun `a copied premise still checks the completed content pool`() {
    val ordinary = premise("Prelude2CardPack", players = 2)
    val altered =
        ordinary.copy(
            classSelections = ordinary.classSelections + ClassSelection(cn("PreservationProgram"))
        )

    shouldThrow<InvalidGameConfigException> { Engine.newGame(altered) }
  }

  @Test
  internal fun `Unsafe does not override map exclusivity`() {
    rejects("Unsafe, HellasMap, ElysiumMap")
  }

  private fun defaultMayBeExcluded(
      default: String,
      whenSelecting: String,
      selectsExactly: Set<ClassName>,
      excludingItSelectsExactly: Set<ClassName>,
      players: Int = 2,
  ) {
    resolvesToExactly(whenSelecting, selectsExactly, players)
    addingIsHarmless(default, to = whenSelecting, players)
    resolvesToExactly(
        withSelection(whenSelecting, "-$default"),
        excludingItSelectsExactly,
        players,
    )
  }

  private fun selectionPreventsDefault(
      selection: String,
      default: String,
      selectsExactly: Set<ClassName>,
      players: Int = 2,
  ) {
    val selected = resolvesToExactly(selection, selectsExactly, players)
    withClue("selecting $selection prevents $default from defaulting") {
      (cn(default) in selected.modules) shouldBe false
    }
  }

  private fun addingIsHarmless(
      addition: String,
      to: String,
      players: Int = 2,
  ) {
    withClue("adding $addition to [$to] is harmless") {
      resolvedPremise(withSelection(to, addition), players)
          .shouldHaveSameSelectionAs(resolvedPremise(to, players))
    }
  }

  private fun excludingIsHarmless(
      exclusion: String,
      from: String,
      players: Int = 2,
  ) {
    withClue("excluding $exclusion from [$from] is harmless") {
      resolvedPremise(withSelection(from, "-$exclusion"), players)
          .shouldHaveSameSelectionAs(resolvedPremise(from, players))
    }
  }

  private fun GamePremise.shouldHaveSameSelectionAs(expected: GamePremise) {
    modules shouldBe expected.modules
    classSelections shouldBe expected.classSelections
    initialComponentTypes shouldBe expected.initialComponentTypes
    playerNames shouldBe expected.playerNames
  }

  private fun resolvesToExactly(
      config: String,
      expectedModules: Set<ClassName>,
      players: Int = 2,
  ): GamePremise =
      resolvedPremise(config, players).also { premise ->
        withClue("[$config] with $players player(s)") {
          premise.modules.shouldContainExactlyInAnyOrder(expectedModules)
        }
      }

  private fun resolvedPremise(config: String, players: Int): GamePremise =
      premise(config, players).also(Engine::newGame)

  private fun classTable(config: String, players: Int = 2) =
      Engine.newGame(premise(config, players)).classTable

  private fun cannotSelectTogether(
      first: String,
      second: String,
      players: Int = 2,
  ) {
    rejects("$first, $second", players)
  }

  private fun rejects(config: String, players: Int = 2) {
    withClue("[$config] with $players player(s) is rejected") {
      shouldThrow<InvalidGameConfigException> { Engine.newGame(premise(config, players)) }
    }
  }

  private fun configurationRejects(config: String, players: Int = 2) {
    withClue("[$config] with $players player(s) is rejected as a game configuration") {
      shouldThrow<InvalidGameConfigException> { Engine.newGame(premise(config, players)) }
    }
  }

  private fun premise(config: String, players: Int) =
      Canon.gamePremise(gameConfig(extra = config, playerNames = (1..players).map { "Player$it" }))

  private fun multiplayerWith(vararg additions: String): Set<ClassName> =
      defaultMultiplayer + additions.map(::cn)

  private fun multiplayerOn(map: String, vararg additions: String): Set<ClassName> =
      defaultMultiplayer - cn("TharsisMap") + cn(map) + additions.map(::cn)

  private companion object {
    val defaultMultiplayer: Set<ClassName> =
        names("TerraformingMars, CorporateEraExpansion, MultiplayerMode, TharsisMap")
    val defaultSolo: Set<ClassName> =
        names(
            "TerraformingMars, CorporateEraExpansion, SoloMode, " +
                "StandardSoloObjective, TharsisMap"
        )

    fun withSelection(config: String, selection: String): String =
        listOf(config, selection).filter(String::isNotBlank).joinToString(", ")

    fun names(source: String): Set<ClassName> =
        source.split(Regex("[,\\s]+")).filter(String::isNotEmpty).mapTo(linkedSetOf(), ::cn)
  }
}
