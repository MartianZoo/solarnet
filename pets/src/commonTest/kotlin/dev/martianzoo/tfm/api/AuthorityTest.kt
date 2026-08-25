package dev.martianzoo.tfm.api

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.CARDS
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class AuthorityTest {
  @Test
  internal fun everyAuthorityIncludesThePetsRuntimeDeclarations() {
    TfmAuthority().classDeclaration(COMPONENT)
  }

  @Test
  internal fun specializedThisInvariantCanLimitOneConcreteClassAcrossOwners() {
    val table =
        authority(
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
  internal fun definitionsAndTheirExtraClassesBecomeDeclarations() {
    val authority =
        object : TfmAuthority() {
          override val cardDefinitions =
              setOf(
                  CardDefinition(
                      CardData(
                          name = "ExampleCard",
                          deck = "PRELUDE",
                          immediate = "Plant",
                          components = setOf("CLASS Foo<Boo> : Loo { HAS =1 Bar; Abc: Xyz }"),
                      )
                  )
              )
        }

    authority.classDeclaration(cn("ExampleCard")).abstract shouldBe false
    authority.classDeclaration(cn("Foo")).dependencies.shouldHaveSize(1)
  }

  @Test
  internal fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")
    val composed = TfmAuthority.compose(authority(declaration), authority(declaration))

    composed.classDeclaration(cn("Shared")) shouldBe declaration
  }

  @Test
  internal fun explicitDeclarationsRetainGenericCardLocationsInFollowMode() {
    val source =
        parseClasses(
                """
                ABSTRACT CLASS Buyer {
                  ResearchPhase: CARDS[4 ProjectCard<Selecting>, -4 ProjectCard<Selecting>? THEN BuySelectedCards]
                }
                """
                    .trimIndent()
            )
            .single()
    val expected =
        parseClasses(
                "ABSTRACT CLASS Buyer { ResearchPhase: Selecting THEN (4 ProjectCard<Selecting>, -4 ProjectCard<Selecting>? THEN BuySelectedCards) }"
            )
            .single()

    val loaded = authority(source).allClassDeclarations.getValue(cn("Buyer"))

    loaded.effects shouldBe expected.effects
    loaded.authoredEffects shouldBe source.effects
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

    shouldThrow<IllegalArgumentException> {
      TfmAuthority.compose(first, second).modules
    }
  }

  @Test
  internal fun compositionRejectsDifferentDeclarationsWithTheSameName() {
    val concrete = parseOneLinerClass("CLASS Shared")
    val abstract = parseOneLinerClass("ABSTRACT CLASS Shared")

    shouldThrow<PetException> {
      TfmAuthority.compose(authority(concrete), authority(abstract)).allClassDeclarations
    }
  }

  @Test
  internal fun compositionRejectsConflictingDisplayNames() {
    fun named(displayName: String) =
        object : TfmAuthority() {
          override val displayNamesByLanguage = mapOf("en" to mapOf(cn("Shared") to displayName))
        }

    shouldThrow<IllegalArgumentException> {
      TfmAuthority.compose(named("First"), named("Second")).displayNamesByLanguage
    }
  }

  @Test
  internal fun moduleCanSelectOneContentKindFromAnotherBundle() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              setOf(
                  parseOneLinerClass("ABSTRACT CLASS Module"),
                  parseOneLinerClass("ABSTRACT CLASS CardBack"),
                  parseOneLinerClass("ABSTRACT CLASS CardFront<Class<CardBack>>"),
                  parseOneLinerClass("CLASS ExampleModule : Module"),
              )
          override val moduleContentSelections =
              mapOf(
                  cn("ExampleModule") to
                      setOf(BundleContentSelection(cn("ContentProvider"), setOf(CARDS)))
              )
        }
    val card = CardDefinition(CardData(name = "ExampleCard"))
    val unrelated = parseOneLinerClass("CLASS Unrelated")
    val contentBundle =
        object : Bundle(cn("ContentProvider")) {
          override val explicitClassDeclarations = setOf(unrelated)
          override val cardDefinitions = setOf(card)
        }
    val source = TfmAuthority.compose(moduleBundle, contentBundle)

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("ExampleModule")))

    table.isActive(card.className) shouldBe true
    table.isActive(unrelated.className) shouldBe false
  }

  @Test
  internal fun moduleWithoutAContentSelectionDoesNotSelectItsBundleDefinitions() {
    val card = CardDefinition(CardData(name = "ExampleCard"))
    val source =
        object : Bundle(cn("ExampleBundle")) {
          override val explicitClassDeclarations =
              setOf(
                  parseOneLinerClass("ABSTRACT CLASS Module"),
                  parseOneLinerClass("ABSTRACT CLASS CardBack"),
                  parseOneLinerClass("ABSTRACT CLASS CardFront<Class<CardBack>>"),
                  parseOneLinerClass("CLASS ExampleModule : Module"),
              )
          override val cardDefinitions = setOf(card)
        }

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("ExampleModule")))

    table.isActive(card.className) shouldBe false
  }

  @Test
  internal fun replacementsRemainKnownWhileTheSelectedModuleActivatesOnlyTheReplacement() {
    val moduleBundle =
        bundle(
            "Base",
            "ABSTRACT CLASS Module\nABSTRACT CLASS CardBack\nABSTRACT CLASS CardFront<Class<CardBack>>\nCLASS Base : Module",
        )
    val original = CardDefinition(CardData(name = "DeimosDown"))
    val replacement = CardDefinition(CardData(name = "DeimosDownPromo", replaces = "DeimosDown"))
    val baseCards = cardBundle("BaseCards", original)
    val replacementCards = cardBundle("ReplacementCards", replacement)
    val configuredModuleBundle =
        object : Bundle(cn("ConfiguredModule")) {
          override val explicitClassDeclarations = moduleBundle.explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("Base") to
                      setOf(
                          BundleContentSelection(cn("BaseCards"), setOf(CARDS)),
                          BundleContentSelection(cn("ReplacementCards"), setOf(CARDS)),
                      )
              )
        }
    val source = TfmAuthority.compose(configuredModuleBundle, baseCards, replacementCards)

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("Base")))

    source.cardDefinitions.map { it.className }.toSet() shouldBe
        setOf(cn("DeimosDown"), cn("DeimosDownPromo"))
    table.isActive(cn("DeimosDown")) shouldBe false
    table.isActive(cn("DeimosDownPromo")) shouldBe true
  }

  @Test
  internal fun replacementExclusionsFollowTheEntireChain() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              bundle(
                      "Declarations",
                      """
                      ABSTRACT CLASS Module
                      ABSTRACT CLASS CardBack
                      ABSTRACT CLASS CardFront<Class<CardBack>>
                      CLASS Base : Module
                      CLASS Latest : Module
                      """
                          .trimIndent(),
                  )
                  .explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("Base") to setOf(BundleContentSelection(cn("OriginalCards"), setOf(CARDS))),
                  cn("Latest") to setOf(BundleContentSelection(cn("LatestCards"), setOf(CARDS))),
              )
        }
    val original = CardDefinition(CardData(name = "OriginalCard"))
    val intermediate =
        CardDefinition(CardData(name = "IntermediateCard", replaces = "OriginalCard"))
    val latest = CardDefinition(CardData(name = "LatestCard", replaces = "IntermediateCard"))
    val source =
        TfmAuthority.compose(
            moduleBundle,
            cardBundle("OriginalCards", original),
            cardBundle("IntermediateCards", intermediate),
            cardBundle("LatestCards", latest),
        )

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("Base, Latest")))

    table.isActive(original.className) shouldBe false
    table.isActive(intermediate.className) shouldBe false
    table.isActive(latest.className) shouldBe true
  }

  @Test
  internal fun multipleSelectedReplacementsForOneDefinitionAreRejected() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              bundle(
                      "Declarations",
                      """
                      ABSTRACT CLASS Module
                      ABSTRACT CLASS CardBack
                      ABSTRACT CLASS CardFront<Class<CardBack>>
                      CLASS First : Module
                      CLASS Second : Module
                      """
                          .trimIndent(),
                  )
                  .explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("First") to setOf(BundleContentSelection(cn("FirstCards"), setOf(CARDS))),
                  cn("Second") to setOf(BundleContentSelection(cn("SecondCards"), setOf(CARDS))),
              )
        }
    val original = CardDefinition(CardData(name = "OriginalCard"))
    val first = CardDefinition(CardData(name = "FirstReplacement", replaces = "OriginalCard"))
    val second = CardDefinition(CardData(name = "SecondReplacement", replaces = "OriginalCard"))
    val source =
        TfmAuthority.compose(
            moduleBundle,
            cardBundle("OriginalCards", original),
            cardBundle("FirstCards", first),
            cardBundle("SecondCards", second),
        )

    shouldThrow<IllegalArgumentException> {
      source.gamePremise(GameConfig("First, Second"))
    }
  }

  @Test
  internal fun individualCardConfigurationIsSupported() {
    val source =
        TfmAuthority.compose(
            bundle(
                "Base",
                "ABSTRACT CLASS Module\nABSTRACT CLASS CardBack\nABSTRACT CLASS CardFront<Class<CardBack>>\nCLASS Base : Module",
            ),
            cardBundle("Cards", CardDefinition(CardData(name = "ExampleCard"))),
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
            ABSTRACT CLASS CardFront<Class<CardBack>>
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
    val contentPack =
        object : Bundle(cn("ContentPack")) {
          override val explicitClassDeclarations =
              parseClasses("CLASS ContentPack : Module").toSet()
          override val moduleContentSelections =
              mapOf(cn("ContentPack") to setOf(BundleContentSelection(cn("Cards"), setOf(CARDS))))
        }
    val observingCard =
        CardDefinition(
            CardData(
                name = "ObservingCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                requirement = "ObservedState",
            )
        )
    val constructingCard =
        CardDefinition(
            CardData(
                name = "ConstructingCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                immediate = "LockedState",
            )
        )
    val observingMaximumCard =
        CardDefinition(
            CardData(
                name = "ObservingMaximumCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                requirement = "MAX 5 ObservedState",
            )
        )
    val observingRemovalCard =
        CardDefinition(
            CardData(
                name = "ObservingRemovalCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                immediate = "-LockedState.",
            )
        )
    val independentCard =
        CardDefinition(
            CardData(
                name = "IndependentCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
            )
        )
    val source =
        TfmAuthority.compose(
            base,
            feature,
            contentPack,
            cardBundle(
                "Cards",
                observingCard,
                constructingCard,
                observingMaximumCard,
                observingRemovalCard,
                independentCard,
            ),
        )

    val filtered = ClassTable.forPremise(source.gamePremise(GameConfig("Base, ContentPack")))
    filtered.isActive(observingCard.className) shouldBe false
    filtered.isActive(constructingCard.className) shouldBe false
    filtered.isActive(observingMaximumCard.className) shouldBe false
    filtered.isActive(observingRemovalCard.className) shouldBe false
    filtered.isActive(independentCard.className) shouldBe true

    val automatic =
        ClassTable.forPremise(source.gamePremise(GameConfig("Base, ContentPack, Feature")))
    automatic.isActive(observingCard.className) shouldBe true
    automatic.isActive(constructingCard.className) shouldBe true
    automatic.isActive(observingMaximumCard.className) shouldBe true
    automatic.isActive(observingRemovalCard.className) shouldBe true
    automatic.isActive(independentCard.className) shouldBe true

    listOf(observingCard, constructingCard, observingMaximumCard, observingRemovalCard).forEach {
        card ->
      val unavailable =
          shouldThrow<IllegalArgumentException> {
            source.gamePremise(GameConfig("Base, ${card.className}"))
          }
      unavailable.message.orEmpty() shouldContain "configured definition"
    }

    val explicitIndependent =
        ClassTable.forPremise(source.gamePremise(GameConfig("Base, ${independentCard.className}")))
    explicitIndependent.isActive(independentCard.className) shouldBe true
  }

  private fun authority(vararg declarations: ClassDeclaration): TfmAuthority =
      object : TfmAuthority() {
        override val explicitClassDeclarations = declarations.toSet()
      }

  private fun bundle(name: String, declarations: String): Bundle =
      object : Bundle(cn(name)) {
        override val explicitClassDeclarations = parseClasses(declarations).toSet()
      }

  private fun cardBundle(name: String, vararg cards: CardDefinition): Bundle =
      object : Bundle(cn(name)) {
        override val cardDefinitions = cards.toSet()
      }
}
