package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
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

    resolvesToExactly(
        "TurmoilCardPack, PromoCardPack",
        multiplayerWith("TurmoilCardPack", "PromoCardPack"),
    )
    resolvesToExactly("QuickStartVariant", multiplayerWith("QuickStartVariant"))
  }

  @Test
  internal fun `expansions remain independently selectable without Corporate Era`() {
    val base = defaultMultiplayer - cn("CorporateEraExpansion") + cn("QuickStartVariant")
    mapOf(
            "VenusNextExpansion" to names("VenusNextExpansion, WorldGovernmentRule"),
            "PreludeExpansion" to names("PreludeExpansion, Prelude1CardPack"),
            "ColoniesExpansion" to names("ColoniesExpansion"),
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

    rejects("Terraformer35", players = 1)
    rejects("Landlord", players = 1)
    rejects("VenusNextExpansion, MandatoryVenusVariant", players = 1)

    rejects("Callisto")
    configurationRejects("HellasMap, Geologist")
    configurationRejects("UtopiaMap, Geologist")
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
      Canon.gamePremise(GameConfig(config, *(1..players).map { "Player$it" }.toTypedArray()))

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
