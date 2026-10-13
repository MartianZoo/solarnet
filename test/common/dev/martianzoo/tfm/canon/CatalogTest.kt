package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CatalogTest {

  @Test
  internal fun compositionRejectsAmbiguousModuleSelection() {
    val declarations =
        "ABSTRACT CLASS Module\nCLASS SharedModule : Module"
            .lines()
            .map(::parseOneLinerClass)
            .toSet()
    val first =
        object : Bundle("first") {
          override val explicitClassDeclarations = declarations
        }
    val second =
        object : Bundle("second") {
          override val explicitClassDeclarations = declarations
        }

    shouldThrow<IllegalArgumentException> { TfmCatalog(first, second).modules }
  }

  @Test
  internal fun moduleOnlySelectsCardsInItsMatchingResourceGroup() {
    val card = parseOneLinerClass("CLASS ExampleCard : CardFront<Class<CardBack>>")
    val source =
        object : Bundle("example") {
          override val explicitClassDeclarations =
              setOf(
                  parseOneLinerClass("ABSTRACT CLASS Module"),
                  parseOneLinerClass("ABSTRACT CLASS CardBack"),
                  parseOneLinerClass("ABSTRACT CLASS CardFront<Class<CardBack>>"),
                  parseOneLinerClass("CLASS ExampleModule : Module"),
                  card,
              )
        }

    val table = source.gamePremise(GameConfig("ExampleModule")).classTable

    table.isInhabited(card.className) shouldBe false
  }

  @Test
  internal fun concreteSubclassesCanBeFoundByBundle() {
    val source =
        TfmCatalog(
            bundle("types", "ABSTRACT CLASS Goal"),
            bundle(
                "goals",
                """
                ABSTRACT CLASS AbstractGoal : Goal
                CLASS DirectGoal : Goal
                CLASS SpecializedGoal : DirectGoal
                """
                    .trimIndent(),
            ),
            bundle("othergoals", "CLASS OtherGoal : Goal"),
        )

    source.classNamesInBundle("goals", cn("Goal")) shouldBe
        setOf(cn("DirectGoal"), cn("SpecializedGoal"))
  }

  @Test
  internal fun defaultBundleGoalPoolsMayContainFewerThanThreeOfEachKind() {
    val source =
        TfmCatalog(
            bundle(
                "rules",
                """
                ABSTRACT CLASS Module
                CLASS MultiplayerMode : Module
                ABSTRACT CLASS Milestone
                ABSTRACT CLASS Award
                """
                    .trimIndent(),
            ),
            bundle(
                "sparse",
                """
                CLASS SparseMap : Module
                CLASS FirstMilestone : Milestone
                CLASS SecondMilestone : Milestone
                CLASS FirstAward : Award
                CLASS SecondAward : Award
                CLASS ThirdAward : Award
                """
                    .trimIndent(),
                primaryModuleName = cn("SparseMap"),
            ),
        )

    val premise = source.gamePremise(GameConfig("MultiplayerMode, SparseMap"))

    premise.classSelections.filter { it.included }.mapTo(linkedSetOf()) { it.className } shouldBe
        setOf(
            cn("MultiplayerMode"),
            cn("SparseMap"),
            cn("FirstMilestone"),
            cn("SecondMilestone"),
            cn("FirstAward"),
            cn("SecondAward"),
            cn("ThirdAward"),
        )
  }

  @Test
  internal fun customGoalSupportComesFromDeclarationsWithoutImplementations() {
    val source =
        TfmCatalog(
            bundle(
                "rules",
                """
                ABSTRACT CLASS Module
                CLASS MultiplayerMode : Module
                ABSTRACT CLASS Award { metric = Metric }
                """
                    .trimIndent(),
            ),
            bundle(
                "sparse",
                """
                CLASS SparseMap : Module
                CLASS GoalMetric : CustomMetric
                CLASS FirstAward : Award { metric = COUNT "GoalMetric" }
                """
                    .trimIndent(),
            ),
        )

    val premise = source.gamePremise(GameConfig("MultiplayerMode, FirstAward"))

    source.classAvailabilityModules[cn("GoalMetric")] shouldBe null
    premise.classSelections.filter { it.included }.mapTo(linkedSetOf()) { it.className } shouldBe
        setOf(cn("MultiplayerMode"), cn("FirstAward"))
    premise.classTable.getClass(cn("GoalMetric")).declaration.customMetric shouldBe true
  }

  @Test
  internal fun aModuleOwningColonyTilesRequiresASelectedTile() {
    val source =
        TfmCatalog(
            bundle(
                "tiles",
                """
                ABSTRACT CLASS Module
                ABSTRACT CLASS ColonyTile
                CLASS FixtureExpansion : Module
                CLASS FixtureTile : ColonyTile
                """
                    .trimIndent(),
                primaryModuleName = cn("FixtureExpansion"),
            )
        )

    source.gamePremise(GameConfig("FixtureExpansion, FixtureTile"))
    shouldThrow<InvalidGameConfigException> {
          source.gamePremise(GameConfig("FixtureExpansion, -FixtureTile"))
        }
        .message shouldBe "FixtureExpansion requires at least one selected colony tile"
  }

  @Test
  internal fun cardResourceFragmentsActivateTheirCardsAndUnreferencedDeclarations() {
    val source =
        StandardFormBundle(
            name = "cardpack",
            primaryModuleName = cn("CardPack"),
            resourceDirectory = "cardpack",
            resourceFilenames = setOf("support.pets", "cards.pets", "handwritten.cards.pets"),
            resourceReader = { path ->
              when (path) {
                "cardpack/support.pets" ->
                    """
                    ABSTRACT CLASS Module
                    ABSTRACT CLASS CardBack
                    ABSTRACT CLASS CardFront<Class<CardBack>>
                    CLASS CardPack : Module
                    """
                        .trimIndent()
                "cardpack/cards.pets" ->
                    """
                    CLASS ExampleCard : CardFront<Class<CardBack>>
                    CLASS PassiveHelper
                    """
                        .trimIndent()
                "cardpack/handwritten.cards.pets" ->
                    "CLASS HandwrittenCard : CardFront<Class<CardBack>>"
                else -> error("Unexpected resource $path")
              }
            },
        )

    val table = source.gamePremise(GameConfig("CardPack")).classTable

    (cn("ExampleCard") in table.allClassNames) shouldBe true
    (cn("HandwrittenCard") in table.allClassNames) shouldBe true
    (cn("PassiveHelper") in table.allClassNames) shouldBe true
  }

  @Test
  internal fun individualCardConfigurationIsSupported() {
    val source =
        TfmCatalog(
            bundle(
                "base",
                "ABSTRACT CLASS Module\nABSTRACT CLASS CardBack\nABSTRACT CLASS CardFront<Class<CardBack>>\nCLASS Base : Module",
            ),
            cardBundle("cards", "ExampleCard"),
        )

    val premise = source.gamePremise(GameConfig("Base, ExampleCard"))

    (cn("ExampleCard") in premise.classTable.allClassNames) shouldBe true
  }

  @Test
  internal fun bundleSelectionFiltersAndRejectsDependentContent() {
    val base =
        bundle(
            "base",
            """
            ABSTRACT CLASS Module
            ABSTRACT CLASS CardBack
            ABSTRACT CLASS CardFront<Class<CardBack>> {
              cost = Number
              requirement = Requirement?
            }
            ABSTRACT CLASS AutomatedCard : CardFront<Class<ProjectCard>>
            CLASS ProjectCard : CardBack
            CLASS Base : Module
            """
                .trimIndent(),
        )
    val feature =
        bundle(
            "feature",
            """
            CLASS Feature : Module { HAS =1 Class<ObservedState>, =1 Class<LockedState> }
            CLASS ObservedState
            CLASS LockedState
            """
                .trimIndent(),
            primaryModuleName = cn("Feature"),
        )
    val cardDeclarations =
        """
        CLASS ObservingCard : AutomatedCard { cost = 0; requirement = HAS "ObservedState" }
        CLASS ConstructingCard : AutomatedCard { cost = 0; This: LockedState }
        CLASS ObservingMaximumCard : AutomatedCard { cost = 0; requirement = HAS "MAX 5 ObservedState" }
        CLASS ObservingRemovalCard : AutomatedCard { cost = 0; This: -LockedState* }
        CLASS SupportingClassCard : AutomatedCard { cost = 0 }
        CLASS IndependentCard : AutomatedCard { cost = 0 }
        CLASS SupportingClass<LockedState>
        """
            .trimIndent()
    val contentPack =
        StandardFormBundle(
            name = "content",
            primaryModuleName = cn("ContentPack"),
            resourceDirectory = "content",
            resourceFilenames = setOf("content.pets", "cards.pets"),
            resourceReader = { path ->
              when (path) {
                "content/content.pets" -> "CLASS ContentPack : Module"
                "content/cards.pets" -> cardDeclarations
                else -> error("Unexpected resource $path")
              }
            },
        )
    val observingCard = cn("ObservingCard")
    val constructingCard = cn("ConstructingCard")
    val observingMaximumCard = cn("ObservingMaximumCard")
    val observingRemovalCard = cn("ObservingRemovalCard")
    val independentCard = cn("IndependentCard")
    val source =
        TfmCatalog(
            base,
            feature,
            contentPack,
        )

    val filtered = source.gamePremise(GameConfig("Base, ContentPack")).classTable
    filtered.isInhabited(observingCard) shouldBe false
    filtered.isInhabited(constructingCard) shouldBe false
    filtered.isInhabited(observingMaximumCard) shouldBe false
    filtered.isInhabited(observingRemovalCard) shouldBe false
    (observingCard in filtered.allClassNames) shouldBe false
    (constructingCard in filtered.allClassNames) shouldBe false
    (observingMaximumCard in filtered.allClassNames) shouldBe false
    (observingRemovalCard in filtered.allClassNames) shouldBe false
    filtered.isInhabited(cn("SupportingClassCard")) shouldBe true
    filtered.isInhabited(independentCard) shouldBe true

    val automatic = source.gamePremise(GameConfig("Base, ContentPack, Feature")).classTable
    automatic.isInhabited(observingCard) shouldBe true
    automatic.isInhabited(constructingCard) shouldBe true
    automatic.isInhabited(observingMaximumCard) shouldBe true
    automatic.isInhabited(observingRemovalCard) shouldBe true
    automatic.isInhabited(cn("SupportingClassCard")) shouldBe true
    automatic.isInhabited(independentCard) shouldBe true

    listOf(
            observingCard,
            constructingCard,
            observingMaximumCard,
            observingRemovalCard,
        )
        .forEach { card ->
          val unavailable =
              shouldThrow<InvalidGameConfigException> {
                source.gamePremise(GameConfig("Base, $card"))
              }
          unavailable.message.orEmpty() shouldContain "configured content"
        }

    val explicitIndependent = source.gamePremise(GameConfig("Base, $independentCard")).classTable
    explicitIndependent.isInhabited(independentCard) shouldBe true
  }

  private fun bundle(
      name: String,
      declarations: String,
      primaryModuleName: ClassName? = null,
  ): Bundle =
      object : Bundle(name, primaryModuleName) {
        override val explicitClassDeclarations = parseClasses(declarations).toSet()
      }

  private fun cardBundle(name: String, vararg cards: String): Bundle =
      object : Bundle(name) {
        override val explicitClassDeclarations =
            parseClasses(
                    cards.joinToString("\n") { card ->
                      if (card.startsWith("CLASS ")) card
                      else "CLASS $card : CardFront<Class<CardBack>>"
                    }
                )
                .toSet()
      }
}
