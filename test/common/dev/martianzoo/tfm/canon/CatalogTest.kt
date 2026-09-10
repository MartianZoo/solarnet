package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CatalogTest {
  @Test
  internal fun everyCatalogIncludesThePetsRuntimeDeclarations() {
    TfmCatalog().classDeclaration(COMPONENT)
  }

  @Test
  internal fun specializedThisInvariantCanLimitOneConcreteClassAcrossOwners() {
    val table =
        catalog(
                *parseClasses(
                        """
                        ABSTRACT CLASS Player : Owner {
                          HAS =1 This
                          CLASS Player1, Player2
                        }
                        ABSTRACT CLASS CardFront<Player> : Owned<Player> { HAS MAX 1 This<Player> }
                        CLASS ExampleCard : CardFront
                        """
                            .trimIndent()
                    )
                    .toTypedArray()
            )
            .classTable
    val expectedLimitType = table.resolve(parse("ExampleCard<Player>"))

    listOf("ExampleCard<Player1>", "ExampleCard<Player2>").forEach { expression ->
      table.componentLimits
          .limitsFor(table.resolve(parse(expression)))
          .single { it.range.last == 1 }
          .type shouldBe expectedLimitType
    }
  }

  @Test
  internal fun selectedClassViabilityUsesItsLoadedDeclaration() {
    val source =
        catalog(
            *parseClasses(
                    """
                    CLASS Missing
                    CLASS Selected { This: -Missing! }
                    """
                        .trimIndent()
                )
                .toTypedArray()
        )

    val unavailable =
        shouldThrow<IllegalArgumentException> {
          source.gamePremise(GameConfig("Selected")).classTable
        }

    unavailable.message.orEmpty() shouldContain
        "unviable game premise: Selected has reachable mandatory removal Missing"
  }

  @Test
  internal fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")
    val composed = TfmCatalog.compose(catalog(declaration), catalog(declaration))

    composed.classDeclaration(cn("Shared")) shouldBe declaration
  }

  @Test
  internal fun revealAndTestDelegatesThePrintedPredicateInFollowMode() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Examiner {
                  -> CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS MicrobeTag): Science) OR Ok)]
                }
                """
                    .trimIndent()
            )
            .single()
    val expected =
        parseClasses(
                """
                ABSTRACT CLASS Examiner {
                  -> ProjectCard<Revealed> THEN Science?
                }
                """
                    .trimIndent()
            )
            .single()

    val loaded = catalog(source).allClassDeclarations.getValue(cn("Examiner"))

    loaded.effects shouldBe expected.effects
    loaded.authoredActions shouldBe source.authoredActions
  }

  @Test
  internal fun filteredCardSearchesLowerToOrdinaryFollowModeDraws() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Searcher {
                  This: CARDS[2 SearchForCard(HAS PrintedTag<Class<PlantTag>>)]
                }
                """
                    .trimIndent()
            )
            .single()
    val expected = parseClasses("ABSTRACT CLASS Searcher { This: 2 ProjectCard }").single()

    val loaded = catalog(source).allClassDeclarations.getValue(cn("Searcher"))

    loaded.effects shouldBe expected.effects
    loaded.authoredEffects shouldBe source.effects
  }

  @Test
  internal fun printedCardClassSelectionLowersToUnfilteredFollowModeSelection() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Stager {
                  -> CARDS[Stage<Class<CardFront>(HAS PrintedTag<Class<BuildingTag>> OR PrintedTag<Class<SpaceTag>>)>]
                }
                """
                    .trimIndent()
            )
            .single()
    val expected =
        parseClasses(
                """
                ABSTRACT CLASS Stager {
                  -> Stage<Class<CardFront>>
                }
                """
                    .trimIndent()
            )
            .single()

    val loaded = catalog(source).allClassDeclarations.getValue(cn("Stager"))

    loaded.effects shouldBe expected.effects
    loaded.authoredActions shouldBe source.authoredActions
  }

  @Test
  internal fun filteredRetentionDelegatesThePrintedPredicateInFollowMode() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Surveyor {
                  -> CARDS[2 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>(HAS VenusTag). THEN -2 ProjectCard<Selecting>? THEN BuySelectedCards]
                }
                """
                    .trimIndent()
            )
            .single()
    val expected =
        parseClasses(
                """
                ABSTRACT CLASS Surveyor {
                  -> 2 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>? THEN -2 ProjectCard<Selecting>? THEN BuySelectedCards
                }
                """
                    .trimIndent()
            )
            .single()

    val loaded = catalog(source).allClassDeclarations.getValue(cn("Surveyor"))

    loaded.effects shouldBe expected.effects
    loaded.authoredActions shouldBe source.authoredActions
  }

  @Test
  internal fun cardSyntaxOutsideCardsZonesIsUntouched() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Searcher {
                  This: SearchForCard(HAS PrintedTag<Class<PlantTag>>)
                }
                """
                    .trimIndent()
            )
            .single()

    val loaded = catalog(source).allClassDeclarations.getValue(cn("Searcher"))

    loaded.effects shouldBe source.effects
  }

  @Test
  internal fun compositionRejectsAmbiguousModuleOwnership() {
    val declarations =
        "ABSTRACT CLASS Module\nCLASS SharedModule : Module"
            .lines()
            .map(::parseOneLinerClass)
            .toSet()
    val first =
        object : Bundle(cn("First")) {
          override val explicitClassDeclarations = declarations
        }
    val second =
        object : Bundle(cn("Second")) {
          override val explicitClassDeclarations = declarations
        }

    shouldThrow<IllegalArgumentException> { TfmCatalog.compose(first, second).modules }
  }

  @Test
  internal fun compositionRejectsDifferentDeclarationsWithTheSameName() {
    val concrete = parseOneLinerClass("CLASS Shared")
    val abstract = parseOneLinerClass("ABSTRACT CLASS Shared")

    shouldThrow<PetException> {
      TfmCatalog.compose(catalog(concrete), catalog(abstract)).allClassDeclarations
    }
  }

  @Test
  internal fun compositionRejectsConflictingDisplayNames() {
    fun named(displayName: String) =
        object : TfmCatalog() {
          override val displayNamesByLanguage = mapOf("en" to mapOf(cn("Shared") to displayName))
        }

    shouldThrow<IllegalArgumentException> {
      TfmCatalog.compose(named("First"), named("Second")).displayNamesByLanguage
    }
  }

  @Test
  internal fun moduleOnlySelectsCardsInItsMatchingResourceGroup() {
    val card = parseOneLinerClass("CLASS ExampleCard : CardFront<Class<CardBack>>")
    val source =
        object : Bundle(cn("ExampleBundle")) {
          override val explicitClassDeclarations =
              setOf(
                  parseOneLinerClass("ABSTRACT CLASS Module"),
                  parseOneLinerClass("ABSTRACT CLASS CardBack"),
                  parseOneLinerClass("ABSTRACT CLASS CardFront<Class<CardBack>>"),
                  parseOneLinerClass("CLASS ExampleModule : Module"),
                  card,
              )
        }

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("ExampleModule")))

    table.isActive(card.className) shouldBe false
  }

  @Test
  internal fun concreteSubclassesCanBeFoundByBundle() {
    val source =
        TfmCatalog.compose(
            bundle("Types", "ABSTRACT CLASS Goal"),
            bundle(
                "Goals",
                """
                ABSTRACT CLASS AbstractGoal : Goal
                CLASS DirectGoal : Goal
                CLASS SpecializedGoal : DirectGoal
                """
                    .trimIndent(),
            ),
            bundle("OtherGoals", "CLASS OtherGoal : Goal"),
        )

    source.classNamesInBundle(cn("Goals"), cn("Goal")) shouldBe
        setOf(cn("DirectGoal"), cn("SpecializedGoal"))
  }

  @Test
  internal fun defaultBundleGoalPoolsRequireThreeOfEachKind() {
    val source =
        TfmCatalog.compose(
            bundle(
                "Rules",
                """
                ABSTRACT CLASS Module
                CLASS MultiplayerMode : Module
                ABSTRACT CLASS Milestone
                ABSTRACT CLASS Award
                """
                    .trimIndent(),
            ),
            bundle(
                "SparseMap",
                """
                CLASS SparseMap : Module
                CLASS FirstMilestone : Milestone
                CLASS SecondMilestone : Milestone
                CLASS FirstAward : Award
                CLASS SecondAward : Award
                CLASS ThirdAward : Award
                """
                    .trimIndent(),
            ),
        )

    shouldThrow<IllegalArgumentException> {
      source.gamePremise(GameConfig("MultiplayerMode, SparseMap"))
    }
  }

  @Test
  internal fun aCardResourceActivatesItsUnreferencedNonCardDeclarations() {
    val source =
        StandardFormBundle(
            name = "CardPack",
            resourceDirectory = "CardPack",
            resourceFilenames = setOf("classes.pets", "cards.pets"),
            resourceReader = { path ->
              when (path) {
                "CardPack/classes.pets" ->
                    """
                    ABSTRACT CLASS Module
                    ABSTRACT CLASS CardBack
                    ABSTRACT CLASS CardFront<Class<CardBack>>
                    CLASS CardPack : Module
                    """
                        .trimIndent()
                "CardPack/cards.pets" ->
                    """
                    CLASS ExampleCard : CardFront<Class<CardBack>>
                    CLASS PassiveHelper
                    """
                        .trimIndent()
                else -> error("Unexpected resource $path")
              }
            },
        )

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("CardPack")))

    table.isActive(cn("ExampleCard")) shouldBe true
    table.isActive(cn("PassiveHelper")) shouldBe true
  }

  @Test
  internal fun individualCardConfigurationIsSupported() {
    val source =
        TfmCatalog.compose(
            bundle(
                "Base",
                "ABSTRACT CLASS Module\nABSTRACT CLASS CardBack\nABSTRACT CLASS CardFront<Class<CardBack>>\nCLASS Base : Module",
            ),
            cardBundle("Cards", "ExampleCard"),
        )

    val premise = source.gamePremise(GameConfig("Base, ExampleCard"))

    ClassTable.forPremise(premise).isActive(cn("ExampleCard")) shouldBe true
  }

  @Test
  internal fun bundleOwnershipFiltersAndRejectsDependentContent() {
    val base =
        bundle(
            "Base",
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
            "Feature",
            """
            CLASS Feature : Module { HAS =1 Class<ObservedState>, =1 Class<LockedState> }
            CLASS ObservedState
            CLASS LockedState
            """
                .trimIndent(),
        )
    val cardDeclarations =
        """
        CLASS ObservingCard : AutomatedCard { cost = 0; requirement = HAS "ObservedState" }
        CLASS ConstructingCard : AutomatedCard { cost = 0; This: LockedState }
        CLASS ObservingMaximumCard : AutomatedCard { cost = 0; requirement = HAS "MAX 5 ObservedState" }
        CLASS ObservingRemovalCard : AutomatedCard { cost = 0; This: -LockedState. }
        CLASS SupportingClassCard : AutomatedCard { cost = 0 }
        CLASS IndependentCard : AutomatedCard { cost = 0 }
        CLASS SupportingClass<LockedState>
        """
            .trimIndent()
    val contentPack =
        StandardFormBundle(
            name = "ContentPack",
            resourceDirectory = "ContentPack",
            resourceFilenames = setOf("classes.pets", "cards.pets"),
            resourceReader = { path ->
              when (path) {
                "ContentPack/classes.pets" -> "CLASS ContentPack : Module"
                "ContentPack/cards.pets" -> cardDeclarations
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
        TfmCatalog.compose(
            base,
            feature,
            contentPack,
        )

    val filtered = ClassTable.forPremise(source.gamePremise(GameConfig("Base, ContentPack")))
    filtered.isActive(observingCard) shouldBe false
    filtered.isActive(constructingCard) shouldBe false
    filtered.isActive(observingMaximumCard) shouldBe false
    filtered.isActive(observingRemovalCard) shouldBe false
    filtered.isActive(cn("SupportingClassCard")) shouldBe true
    filtered.isActive(independentCard) shouldBe true

    val automatic =
        ClassTable.forPremise(source.gamePremise(GameConfig("Base, ContentPack, Feature")))
    automatic.isActive(observingCard) shouldBe true
    automatic.isActive(constructingCard) shouldBe true
    automatic.isActive(observingMaximumCard) shouldBe true
    automatic.isActive(observingRemovalCard) shouldBe true
    automatic.isActive(cn("SupportingClassCard")) shouldBe true
    automatic.isActive(independentCard) shouldBe true

    listOf(
            observingCard,
            constructingCard,
            observingMaximumCard,
            observingRemovalCard,
        )
        .forEach { card ->
          val unavailable =
              shouldThrow<IllegalArgumentException> {
                source.gamePremise(GameConfig("Base, $card"))
              }
          unavailable.message.orEmpty() shouldContain "configured content"
        }

    val explicitIndependent =
        ClassTable.forPremise(source.gamePremise(GameConfig("Base, $independentCard")))
    explicitIndependent.isActive(independentCard) shouldBe true
  }

  private fun catalog(vararg declarations: ClassDeclaration): TfmCatalog =
      object : TfmCatalog() {
        override val explicitClassDeclarations = declarations.toSet()
      }

  private fun bundle(name: String, declarations: String): Bundle =
      object : Bundle(cn(name)) {
        override val explicitClassDeclarations = parseClasses(declarations).toSet()
      }

  private fun cardBundle(name: String, vararg cards: String): Bundle =
      object : Bundle(cn(name)) {
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
