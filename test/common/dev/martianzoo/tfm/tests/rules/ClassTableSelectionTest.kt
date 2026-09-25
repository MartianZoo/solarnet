package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Verifies which Catalog Classes are selected and inhabited by each game premise. */
internal class ClassTableSelectionTest {
  // Module and associated-Content selection matrix

  @Test
  internal fun `modules normally select their intrinsic rules and associated Content`() {
    assertSoftly {
      examples.forEach { example ->
        assertValidView(example.module.toString()) { view ->
          assertSelected(
              view,
              setOf(example.module) +
                  listOfNotNull(example.contentPack) +
                  example.intrinsicRules +
                  example.moduleSupport +
                  example.associatedNonPackContent +
                  example.contentSupport +
                  example.standaloneContent +
                  example.moduleDependentContent,
          )
        }
      }
    }
  }

  @Test
  internal fun `Content packs select only Content usable without their module`() {
    assertSoftly {
      examples
          .filter { it.contentPack != null }
          .forEach { example ->
            val pack = requireNotNull(example.contentPack)
            assertValidView(pack.toString()) { view ->
              assertSelected(
                  view,
                  setOf(pack) + example.standaloneContent + example.contentSupport,
              )
              assertOmitted(
                  view,
                  setOf(example.module) +
                      example.intrinsicRules +
                      (example.moduleSupport - example.contentSupport) +
                      example.associatedNonPackContent +
                      example.moduleDependentContent,
              )
            }
          }
    }
  }

  @Test
  internal fun `a module can exclude its associated card Content pack`() {
    assertSoftly {
      examples
          .filter { it.contentPack != null }
          .forEach { example ->
            val pack = requireNotNull(example.contentPack)
            assertValidView("${example.module}, -$pack") { view ->
              assertSelected(
                  view,
                  setOf(example.module) +
                      example.intrinsicRules +
                      example.moduleSupport +
                      example.associatedNonPackContent,
              )
              assertOmitted(
                  view,
                  setOf(pack) +
                      (example.contentSupport - example.moduleSupport) +
                      example.standaloneContent +
                      example.moduleDependentContent,
              )
            }
          }
    }
  }

  @Test
  internal fun `explicitly excluded Content and its private support stay unselected`() {
    assertSoftly {
      examples.forEach { example ->
        (listOf(example.module.toString()) +
                listOfNotNull(example.contentPack?.toString()) +
                listOfNotNull(example.contentPack?.let { "${example.module}, -$it" }))
            .forEach { baseConfig ->
              assertValidView("$baseConfig, -${example.excludedContent}") { view ->
                assertOmitted(view, setOf(example.excludedContent) + example.privateSupport)
              }
            }
      }
    }
  }

  @Test
  internal fun `explicitly included compatible Content needs no module or bundle selection`() {
    assertSoftly {
      examples.forEach { example ->
        example.standaloneContent.forEach { content ->
          (listOf(content.toString(), "${example.module}, $content") +
                  listOfNotNull(example.contentPack?.let { "$it, $content" }) +
                  listOfNotNull(example.contentPack?.let { "${example.module}, -$it, $content" }))
              .forEach { config ->
                assertValidView(config) { view -> assertSelected(view, setOf(content)) }
              }
        }
      }
    }
  }

  @Test
  internal fun `shared promo card resource follows either card without the pack`() {
    listOf("PharmacyUnion", "Hospitals").forEach { card ->
      assertValidView(card) { view ->
        assertSelected(view, setOf(cn(card), cn("Disease")))
        assertOmitted(view, setOf(cn("PromoCardPack")))
      }
    }
  }

  @Test
  internal fun `card local instruction follows its card without the pack`() {
    assertValidView("IcyImpactors") { view ->
      assertSelected(view, setOf(cn("IcyImpactors"), cn("ChooseOceanArea")))
      assertOmitted(view, setOf(cn("PromoCardPack")))
    }
  }

  @Test
  internal fun `Mars Nomads marker follows its card without the pack`() {
    assertValidView("MarsNomads") { view ->
      assertSelected(view, setOf(cn("MarsNomads"), cn("NomadsMarker")))
      assertOmitted(view, setOf(cn("PromoCardPack")))
    }
    assertValidView("PromoCardPack, -MarsNomads") { view ->
      assertOmitted(view, setOf(cn("MarsNomads"), cn("NomadsMarker")))
    }
  }

  @Test
  internal fun `explicitly included dependent Content requires its module`() {
    assertSoftly {
      examples.forEach { example ->
        example.moduleDependentContent.forEach { content ->
          assertInvalidView(content.toString())
          example.contentPack?.let { assertInvalidView("$it, $content") }
          assertValidView("${example.module}, $content") { view ->
            assertSelected(view, setOf(content))
          }
          example.contentPack?.let { pack ->
            assertValidView("${example.module}, -$pack, $content") { view ->
              assertSelected(view, setOf(content))
            }
          }
        }
      }
    }
  }

  @Test
  internal fun `Turmoil global events are individual Content with a hard Module dependency`() {
    assertInvalidView("AquiferReleasedByPublicCouncil")
    assertValidView("TurmoilExpansion, -AquiferReleasedByPublicCouncil") { view ->
      assertSelected(view, setOf(cn("TurmoilExpansion"), cn("GlobalEvent")))
      assertOmitted(view, setOf(cn("AquiferReleasedByPublicCouncil")))
    }
    assertValidView("TurmoilExpansion, -DryDeserts") { view ->
      assertOmitted(
          view,
          setOf(cn("DryDeserts"), cn("RemoveOceanForGlobalEvent"), cn("ResolveDryDeserts")),
      )
    }
  }

  @Test
  internal fun `Turmoil rules and events remain when its cards are individually excluded`() {
    val bundle = cn("TurmoilExpansion")
    val declaredNames =
        Canon.bundles
            .single { it.bundleName == bundle }
            .explicitClassDeclarations
            .mapTo(hashSetOf()) { it.className }
    val cards = Canon.cards.map { it.className }.filterTo(linkedSetOf(), declaredNames::contains)
    cards.containsAll(setOf(cn("AerialLenses"), cn("LakefrontResorts"))) shouldBe true

    // A narrower pool choice is resolved into individual exclusions before premise creation.
    val config = (listOf(bundle.toString()) + cards.map { "-$it" }).joinToString(", ")
    assertValidView(config) { view ->
      assertSelected(
          view,
          setOf(bundle, cn("Party"), cn("GlobalEvent"), cn("AquiferReleasedByPublicCouncil")),
      )
      assertOmitted(view, cards)
    }
  }

  @Test
  internal fun `compatible Turmoil corporations can be selected together without its rules`() {
    val corporations = examples.single { it.module == cn("TurmoilExpansion") }.standaloneContent
    assertValidView(corporations.joinToString(", ")) { view ->
      assertSelected(view, corporations)
      assertOmitted(
          view,
          setOf(
              cn("TurmoilExpansion"),
              cn("Party"),
              cn("GlobalEvent"),
              cn("AerialLenses"),
              cn("AquiferReleasedByPublicCouncil"),
          ),
      )
    }
  }

  // Inclusion and inhabitance vocabulary

  @Test
  internal fun `known selected and inhabited are different questions`() {
    val base = baseMultiplayer.classTable
    val emptyAwardDomain = gameView("Award", "Me")

    // Known concrete Class omitted by this premise.
    base.findClass(cn("VenusStep")) shouldBe Canon.classTable.getClass(cn("VenusStep"))
    base.allClassNames.shouldNotContain(cn("VenusStep"))
    base.isInhabited(cn("VenusStep")) shouldBe false

    // Included abstract Class with no included concrete narrowing.
    emptyAwardDomain.classNames.contains(cn("Award")) shouldBe true
    emptyAwardDomain.classTable.isInhabited(cn("Award")) shouldBe false
    emptyAwardDomain.classTable
        .allSubclasses(emptyAwardDomain.classTable.getClass(cn("Award")))
        .filterNot { it.abstract }
        .shouldBeEmpty()

    // The same abstract Class becomes inhabited when its concrete children are selected.
    base.isInhabited(cn("Award")) shouldBe true
    base.allSubclasses(base.getClass(cn("Award"))).filterNot { it.abstract }.isEmpty() shouldBe
        false
  }

  @Test
  internal fun `guarded mode references do not force unavailable classes into selection`() {
    val solo = gameView("Prelude1CardPack", "Me")

    solo.classTable.isInhabited(cn("Vitor")) shouldBe true
    matchingClasses("award", solo).shouldBeEmpty()
    solo.classNames.shouldNotContain(cn("FirstPlace"))
    solo.classNames.shouldNotContain(cn("SecondPlace"))
  }

  // Other deliberate configuration omissions

  @Test
  internal fun `cross-bundle Colonies classes stay unselected without Colonies`() {
    // Promo has a Colonies-gated card; Utopia Planitia has a Colonies-gated milestone.
    val bundle = Canon.bundles.single { it.bundleName == cn("ColoniesExpansion") }
    fun contributedNames(catalog: TfmCatalog): Set<ClassName> = buildSet {
      catalog.explicitClassDeclarations.mapTo(this) { it.className }
      catalog.marsMapDefinitions.forEach { map ->
        add(map.className)
        map.areas.mapTo(this) { area -> area.className }
      }
    }
    val namesUniqueToColonies =
        contributedNames(bundle) -
            Canon.bundles.filterNot { it == bundle }.flatMapTo(linkedSetOf(), ::contributedNames)
    (promosUtopiaWithoutCorporateEra.classNames intersect namesUniqueToColonies).shouldBeEmpty()
  }

  @Test
  internal fun `cross-bundle prelude Content selects its card back without Prelude rules`() {
    assertSelected(promosUtopiaWithoutCorporateEra, setOf(cn("PreludeCard")))
    assertOmitted(promosUtopiaWithoutCorporateEra, setOf(cn("PreludePhase")))
  }

  @Test
  internal fun `cross-bundle Venus classes stay unselected without Venus Next`() {
    // Promo names VenusStep; Terra Cimmeria names VenusTag. Both definitions are Venus-gated.
    assertOmitted(promosCimmeriaWithoutCorporateEra, setOf(cn("VenusStep"), cn("VenusTag")))
  }

  @Test
  internal fun `solo classes stay unselected in multiplayer`() {
    matchingClasses("solo", preludeVenusMultiplayer).shouldBeEmpty()
  }

  @Test
  internal fun `award domain and scoring machinery stay uninhabited in solo`() {
    matchingClasses("award", baseSolo).shouldBeEmpty()
    baseSolo.classNames.shouldNotContain(cn("FirstPlace"))
    baseSolo.classNames.shouldNotContain(cn("SecondPlace"))
  }

  private fun assertValidView(config: String, assertions: (GameView) -> Unit) {
    val result = runCatching {
      gameView(config, "Player1", "Player2").also { view -> view.classTable }
    }
    withClue("configuration [$config] should produce a game view") {
      result.exceptionOrNull() shouldBe null
    }
    result.getOrNull()?.let(assertions)
  }

  private fun assertInvalidView(config: String) {
    withClue("configuration [$config] should reject a hard Module dependency") {
      shouldThrow<InvalidGameConfigException> {
        gameView(config, "Player1", "Player2").classTable
      }
    }
  }

  private fun assertSelected(view: GameView, classNames: Set<ClassName>) {
    classNames.forEach { className ->
      withClue("$className should be known, selected, and inhabited in [${view.config}]") {
        Canon.allClassNames.contains(className) shouldBe true
        view.classNames.contains(className) shouldBe true
        view.classTable.isInhabited(className) shouldBe true
      }
    }
  }

  private fun assertOmitted(view: GameView, classNames: Set<ClassName>) {
    classNames.forEach { className ->
      withClue("$className should be known but unselected and uninhabited in [${view.config}]") {
        Canon.allClassNames.contains(className) shouldBe true
        view.classNames.contains(className) shouldBe false
        view.classTable.isInhabited(className) shouldBe false
      }
    }
  }

  private fun matchingClasses(pattern: String, gameView: GameView): List<ClassName> =
      gameView.classTable
          .allClasses()
          .map { it.className }
          .filter { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(it.toString()) }

  private class GameView(val config: GameConfig) {
    val classTable: ClassTable by lazy { Canon.gamePremise(config).classTable }
    val classNames: Set<ClassName> by lazy { classTable.allClassNames }
  }

  private class ModuleContentExample(
      val module: ClassName,
      val contentPack: ClassName? = null,
      val intrinsicRules: Set<ClassName>,
      val moduleSupport: Set<ClassName> = emptySet(),
      val associatedNonPackContent: Set<ClassName> = emptySet(),
      val contentSupport: Set<ClassName> = emptySet(),
      val standaloneContent: Set<ClassName> = emptySet(),
      val moduleDependentContent: Set<ClassName> = emptySet(),
      val excludedContent: ClassName,
      val privateSupport: Set<ClassName> = emptySet(),
  )

  private val examples =
      listOf(
          ModuleContentExample(
              module = cn("PreludeExpansion"),
              contentPack = cn("Prelude1CardPack"),
              intrinsicRules = setOf(cn("PreludePhase")),
              moduleSupport = setOf(cn("PreludeCard")),
              contentSupport = setOf(cn("PreludeCard")),
              standaloneContent =
                  setOf(
                      cn("ValleyTrust"),
                      cn("Vitor"),
                      cn("AcquiredSpaceAgency"),
                      cn("HousePrinting"),
                  ),
              excludedContent = cn("Vitor"),
              privateSupport = setOf(cn("NonNegativeIconsOf")),
          ),
          ModuleContentExample(
              module = cn("VenusNextExpansion"),
              intrinsicRules = setOf(cn("VenusStep"), cn("AirScrappingProject")),
              standaloneContent = setOf(cn("Manutech")),
              excludedContent = cn("Manutech"),
          ),
          ModuleContentExample(
              module = cn("ColoniesExpansion"),
              intrinsicRules = setOf(cn("TradeAction"), cn("BuildColonyProject")),
              associatedNonPackContent = setOf(cn("Callisto")),
              standaloneContent = setOf(cn("Arklight"), cn("RefugeeCamps")),
              moduleDependentContent = setOf(cn("CryoSleep")),
              excludedContent = cn("RefugeeCamps"),
              privateSupport = setOf(cn("Camp")),
          ),
          ModuleContentExample(
              module = cn("TurmoilExpansion"),
              intrinsicRules = setOf(cn("GlobalEvent"), cn("Party")),
              associatedNonPackContent = setOf(cn("AquiferReleasedByPublicCouncil")),
              standaloneContent =
                  setOf(
                      cn("LakefrontResorts"),
                      cn("Pristar"),
                      cn("TerralabsResearch"),
                      cn("UtopiaInvest"),
                  ),
              moduleDependentContent = setOf(cn("AerialLenses")),
              excludedContent = cn("AerialLenses"),
          ),
      )

  // Compiled game views belong to this test instance, not the test worker's lifetime.
  private val baseMultiplayer = gameView("", "Player1", "Player2")
  private val baseSolo = gameView("", "Me")
  private val promosUtopiaWithoutCorporateEra =
      gameView(
          "PromoCardPack, UtopiaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )
  private val promosCimmeriaWithoutCorporateEra =
      gameView(
          "PromoCardPack, CimmeriaMap, -CorporateEraExpansion",
          "Player1",
          "Player2",
      )
  private val preludeVenusMultiplayer =
      gameView("PreludeExpansion, VenusNextExpansion", "Player1", "Player2")

  private fun gameView(config: String, vararg playerNames: String): GameView =
      GameView(GameConfig(config, *playerNames))
}
