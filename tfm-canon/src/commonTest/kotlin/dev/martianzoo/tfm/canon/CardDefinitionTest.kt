package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.util.toStrings
import dev.martianzoo.tfm.canon.CardDefinition.CardData
import dev.martianzoo.tfm.canon.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.canon.CardDefinition.ProjectKind.ACTIVE
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CardDefinitionTest {
  private val resourceDeclarations =
      parseClasses(
          """
          CLASS Animal : CardResource
          CLASS Floater : CardResource
          CLASS Microbe : CardResource
          """
              .trimIndent()
      )

  private val birds =
      CardData(
          name = "Birds",
          deck = "PROJECT",
          tags = listOf("AnimalTag"),
          immediate = "PROD[-2 Plant<Anyone>]",
          actions = listOf("-> Animal<This>"),
          effects = listOf("End: VictoryPoint / Animal<This>"),
          requirement = "13 OxygenStep",
          cost = 10,
          projectKind = "ACTIVE",
      )

  @Test
  internal fun realCardDefinitionFromApi() {
    val birds = CardDefinition(birds)
    val authority = authority(setOf(birds))
    birds.className shouldBe cn("Birds")
    birds.deck shouldBe PROJECT
    birds.tags.toStrings().shouldContainExactlyInAnyOrder("AnimalTag")
    birds.immediate!!.toString() shouldBe "PROD[-2 Plant<Anyone>]"
    birds.actions.toStrings().shouldContainExactlyInAnyOrder("-> Animal<This>")
    birds.effects.toStrings().shouldContainExactlyInAnyOrder("End: VictoryPoint / Animal<This>")
    birds.replaces shouldBe null
    authority.cardResourceType(cn("Birds")) shouldBe cn("Animal")
    birds.requirement?.toString() shouldBe "13 OxygenStep"
    birds.cost shouldBe 10
    birds.asClassDeclaration.properties[PropertyName("cost")] shouldBe NumberValue(10)
    birds.asClassDeclaration.properties[PropertyName("requirement")] shouldBe
        RequirementValue(parse("13 OxygenStep"))
    birds.projectInfo?.kind shouldBe ACTIVE
  }

  @Test
  internal fun cardWithoutRequirementOmitsThePropertyValue() {
    val card =
        CardDefinition(
            CardData(
                name = "ExampleCard",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
            )
        )

    card.asClassDeclaration.properties.containsKey(PropertyName("requirement")) shouldBe false
  }

  @Test
  internal fun eventTagIsDerivedOnlyForEventCards() {
    val prelude = CardDefinition(CardData(name = "ExamplePrelude", deck = "PRELUDE"))
    val event =
        CardDefinition(CardData(name = "ExampleEvent", deck = "PROJECT", projectKind = "EVENT"))

    prelude.immediate shouldBe null
    prelude.tags.toStrings().shouldBeEmpty()
    prelude.asClassDeclaration.supertypes.shouldContainExactly(
        parse<Expression>("CardFront<Class<PreludeCard>>")
    )
    event.tags.toStrings().shouldContainExactly("EventTag")
    event.asClassDeclaration.supertypes.shouldContainExactly(
        parse<Expression>("EventCard<Class<ProjectCard>>")
    )

    assertFailsWith<IllegalArgumentException> {
      CardDefinition(CardData(name = "TaggedPrelude", deck = "PRELUDE", tags = listOf("EventTag")))
    }
    assertFailsWith<IllegalArgumentException> {
      CardDefinition(
          CardData(
              name = "TaggedEvent",
              deck = "PROJECT",
              projectKind = "EVENT",
              tags = listOf("EventTag"),
          )
      )
    }
  }

  @Test
  internal fun mostSpecificCardRoleCarriesTheDeckClass() {
    val resourceCorporation =
        CardDefinition(
            CardData(
                name = "ExampleCorporation",
                deck = "CORPORATION",
                actions = listOf("-> Animal<This>"),
                effects = listOf("End: VictoryPoint / Animal<This>"),
            )
        )

    resourceCorporation.asClassDeclaration.supertypes.shouldContainExactlyInAnyOrder(
        parse<Expression>("ResourceCard<Class<Animal>, Class<CorporationCard>>"),
        parse<Expression>("ActionCard"),
    )
  }

  @Test
  internal fun cardTagsMustActuallyBeTags() {
    val badCard = CardDefinition(CardData(name = "BadCard", tags = listOf("WildTag")))
    val badSource =
        object : TfmAuthority() {
          override val cardDefinitions = setOf(badCard)
        }

    assertFailsWith<IllegalArgumentException> {
      TfmAuthority.compose(Canon, badSource).classTable
    }
  }

  @Test
  internal fun realCardFromJson() {
    val json =
        """
      {
        "cards": [
          {
            "name": "Birds",
            "deck": "PROJECT",
            "tags": [ "AnimalTag" ],
            "immediate": "PROD[-2 Plant<Anyone>]",
            "actions": [ "-> Animal<This>" ],
            "effects": [ "End: VictoryPoint / Animal<This>" ],
            "requirement": "13 OxygenStep",
            "cost": 10,
            "projectKind": "ACTIVE"
          }
        ]
      }
    """

    JsonReader.readCards(json).shouldContainExactlyInAnyOrder(birds)
  }

  @Test
  internal fun derivedClassAtPointOfUseLowersToAnOrdinaryCardLocalClass() {
    val card =
        CardDefinition(CardData(name = "TestCard1", immediate = "Mandate { -> 3 ProjectCard }"))

    card.immediate.toString() shouldBe "TestCard1_Mandate"
    val declaration = card.extraClasses.single()
    declaration.className shouldBe cn("TestCard1_Mandate")
    declaration.supertypes.shouldContainExactly(parse<Expression>("Mandate"))
    declaration.effects.shouldContainExactly(parse<Effect>("UseAction<This, First>: 3 ProjectCard"))
  }

  @Test
  internal fun derivedClassSuffixPreservesUseSiteRefinementsAndSpecializesItsSupertype() {
    val card =
        CardDefinition(
            CardData(
                name = "TestCard2",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                immediate =
                    "CityTile<RemoteArea {}>, " +
                        "SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> { HAS MAX 1 This }",
            )
        )

    card.immediate.toString() shouldBe
        "CityTile<TestCard2_RemoteArea>, " +
            "TestCard2_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>"
    card.extraClasses
        .map { it.className }
        .shouldContainExactly(
            cn("TestCard2_RemoteArea"),
            cn("TestCard2_SpecialTile"),
        )
    val specialTile = card.extraClasses.last()
    specialTile.supertypes.shouldContainExactly(parse<Expression>("SpecialTile<LandArea>"))
    specialTile.invariants.shouldContainExactly(parse<Requirement>("MAX 1 This"))
  }

  @Test
  internal fun derivedClassBodyMustFollowTheCompleteExpressionAndCannotContainDefaults() {
    assertFailsWith<PetSyntaxException> {
      CardDefinition(CardData(name = "TestCard3", immediate = "SpecialTile {}<LandArea>"))
    }
    assertFailsWith<PetSyntaxException> {
      CardDefinition(
          CardData(
              name = "TestCard3",
              immediate = "SpecialTile<LandArea> { DEFAULT +SpecialTile<LandArea> }",
          )
      )
    }
  }

  @Test
  internal fun assignedDerivedClassNameMayBeReferencedElsewhereInTheParentInstruction() {
    val card =
        CardDefinition(
            CardData(
                name = "TestCard3",
                immediate = "Mandate { -> ProjectCard } THEN Link<TestCard3_Mandate>",
            )
        )

    card.immediate.toString() shouldBe "TestCard3_Mandate THEN Link<TestCard3_Mandate>"
  }

  @Test
  internal fun realCardOperationsRemainInSourceWhileExecutableEffectsUseFollowMode() {
    val card =
        CardDefinition(
            CardData(
                name = "RealCardSource",
                immediate =
                    "CARDS[2 ProjectCard(HAS Citations<Class<Floater>>)], " +
                        "CARDS[4 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>], " +
                        "CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS SpaceTag): ProjectCard) OR Ok)], " +
                        "CARDS[2 ProjectCard<Hand FROM EventPile>?]",
            )
        )

    card.immediate.toString() shouldBe
        "CARDS[2 ProjectCard(HAS Citations<Class<Floater>>)], " +
            "CARDS[4 ProjectCard<Selecting>, 2 ProjectCard<Hand FROM Selecting>], " +
            "CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS SpaceTag): ProjectCard) OR Ok)], " +
            "CARDS[2 ProjectCard<Hand FROM EventPile>?]"
    card.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "This: 2 ProjectCard, 2 ProjectCard, ProjectCard?, 2 ProjectCard FROM PlayedEvent?"
        )
    )
  }

  @Test
  internal fun followModeNeutralizesCardAreaObservationsAndSelectedCardPlay() {
    val card =
        CardDefinition(
            CardData(
                name = "CardAreaSource",
                immediate =
                    "CARDS[3 PreludeCard<Selecting> THEN PreludeCard<Hand FROM Selecting> THEN PlayCard<Class<PreludeCard>>], " +
                        "CARDS[2 / ProjectCard<Hand>], " +
                        "CARDS[X ProjectCard<Revealed FROM Hand> THEN X ProjectCard<Hand FROM Revealed> THEN X], " +
                        "CARDS[1 / CardBack<EventPile, Anyone>]",
            )
        )

    card.immediate.toString() shouldBe
        "CARDS[3 PreludeCard<Selecting> THEN PreludeCard<Hand FROM Selecting> THEN PlayCard<Class<PreludeCard>>], " +
            "CARDS[2 / ProjectCard<Hand>], " +
            "CARDS[X ProjectCard<Revealed FROM Hand> THEN X ProjectCard<Hand FROM Revealed> THEN X], " +
            "CARDS[1 / CardBack<EventPile, Anyone>]"
    card.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "This: (3 PreludeCard THEN -2 PreludeCard THEN PlayCard<Class<PreludeCard>>), " +
                "2 / ProjectCard, 1? / ProjectCard, 1 / PlayedEvent<Anyone>"
        )
    )
  }

  @Test
  internal fun followModeNeutralizesEventPileMovements() {
    val card =
        CardDefinition(
            CardData(
                name = "EventPileSource",
                immediate =
                    "CARDS[CardBack<EventPile, Class<This>> FROM This], " +
                        "CARDS[CardBack<Player, EventPile, Class<This>> FROM CardBack<EventPile, Class<This>>]",
            )
        )

    card.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "This: PlayedEvent<Class<This>> FROM This, " +
                "PlayedEvent<Player, Class<This>> FROM PlayedEvent<Class<This>>"
        )
    )
  }

  @Test
  internal fun followModeNeutralizesRealCardOperationsInsideDerivedClassBodies() {
    val card =
        CardDefinition(
            CardData(
                name = "RealCardMandate",
                immediate = "Mandate { -> CARDS[ProjectCard(HAS VenusTag)] }",
            )
        )

    card.extraClasses
        .single()
        .effects
        .shouldContainExactly(
            parse<Effect>("UseAction<This, First>: CARDS[ProjectCard(HAS VenusTag)]")
        )
    card.executableExtraClasses
        .single()
        .effects
        .shouldContainExactly(parse<Effect>("UseAction<This, First>: ProjectCard"))
  }

  @Test
  internal fun followModeNeutralizesRealCardPurchaseActions() {
    val single =
        CardDefinition(
            CardData(
                name = "SingleCardPurchase",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions =
                    listOf(
                        "-> CARDS[ProjectCard<Selecting>, -ProjectCard<Selecting>? THEN BuySelectedCards]"
                    ),
            )
        )
    val revealed =
        CardDefinition(
            CardData(
                name = "RevealedCardPurchase",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions =
                    listOf(
                        "-> CARDS[2 ProjectCard<Selecting> THEN 2 ProjectCard<Hand>(HAS VenusTag) FROM ProjectCard<Selecting>. THEN -2 ProjectCard<Selecting>? THEN BuySelectedCards]"
                    ),
            )
        )
    val fourCards =
        CardDefinition(
            CardData(
                name = "FourCardPurchase",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions =
                    listOf(
                        "-> CARDS[4 ProjectCard<Selecting>, -4 ProjectCard<Selecting>? THEN BuySelectedCards]"
                    ),
            )
        )

    single.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: BuyCard?")
    )
    fourCards.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: 4 BuyCard?")
    )
    revealed.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: ProjectCard OR BuyCard?, ProjectCard OR BuyCard?")
    )
  }

  @Test
  internal fun repeatedUnnamedDerivedClassMustBeExplicit() {
    assertFailsWith<PetSyntaxException> {
      CardDefinition(
          CardData(
              name = "TestCard4",
              deck = "PROJECT",
              projectKind = "AUTOMATED",
              requirement = "Mandate {} OR Mandate { HAS MAX 1 This }",
          )
      )
    }
  }

  @Test
  internal fun derivedClassesCannotContainDerivedClasses() {
    listOf(
            "Mandate { -> NextCardEffect {} }",
            "Mandate<NextCardEffect {}> {}",
        )
        .forEach { source ->
          assertFailsWith<PetSyntaxException>(source) {
            CardDefinition(CardData(name = "TestCard5", immediate = source))
          }
        }
  }

  // Just so we don't have to keep repeating the "x" part
  private val card: CardData = CardData("TestCard")

  @Test
  internal fun emptyStrings() {
    assertFailsWith<RuntimeException> { CardData("") }
    assertFailsWith<RuntimeException> { card.copy(replaces = "") }
    assertFailsWith<RuntimeException> { card.copy(requirement = "") }
  }

  @Test
  internal fun badCost() {
    assertFailsWith<RuntimeException> { card.copy(cost = -1) }
    assertFailsWith<RuntimeException> { card.copy(deck = "PRELUDE", cost = 1) }
    assertFailsWith<RuntimeException> { card.copy(deck = "CORPORATION", cost = 1) }
  }

  @Test
  internal fun badProjectKind() {
    assertFailsWith<RuntimeException> { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFailsWith<RuntimeException> { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
    assertFailsWith<RuntimeException> { card.copy(deck = "PROJECT") }
  }

  @Test
  internal fun badRequirement() {
    assertFailsWith<RuntimeException> { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFailsWith<RuntimeException> { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
  }

  @Test
  internal fun badActiveCard() {
    assertFailsWith<RuntimeException> {
      card.copy(projectKind = "EVENT", effects = listOf("Foo: Bar"))
    }
    assertFailsWith<RuntimeException> {
      card.copy(projectKind = "AUTOMATED", effects = listOf("Bar: Qux"))
    }
    assertFailsWith<RuntimeException> {
      card.copy(projectKind = "EVENT", actions = listOf("Foo -> Bar"))
    }
    assertFailsWith<RuntimeException> {
      card.copy(projectKind = "AUTOMATED", actions = listOf("Bar -> Qux"))
    }
    assertFailsWith<RuntimeException> {
      card.copy(
          projectKind = "AUTOMATED",
          effects = listOf("End: VictoryPoint / Animal<This>"),
      )
    }
    assertFailsWith<RuntimeException> {
      card.copy(projectKind = "ACTIVE", immediate = "Whatever")
    }
  }

  @Test
  internal fun cardResourceTypeIsDerivedFromAuthoredOperations() {
    val paymentCard =
        CardDefinition(
            CardData(
                name = "PaymentCard",
                actions = listOf("-> Floater"),
                effects =
                    listOf(
                        "PlayTag<Class<VenusTag>>:: AcceptFromCard<This>",
                        "PayFromCard<This>:: -3 Owed<>",
                    ),
            )
        )
    val wildTagCard = CardDefinition(CardData(name = "WildTagCard", immediate = "WildTag<This>"))
    val genericMicrobeCard =
        CardDefinition(
            CardData(
                name = "GenericMicrobeCard",
                effects = listOf("MicrobeTag<CardFront>: Microbe<CardFront> OR 2"),
            )
        )

    val authority = authority(setOf(paymentCard, wildTagCard, genericMicrobeCard))
    authority.cardResourceType(cn("PaymentCard")) shouldBe cn("Floater")
    authority.cardResourceType(cn("WildTagCard")) shouldBe null
    authority.cardResourceType(cn("GenericMicrobeCard")) shouldBe null
  }

  @Test
  internal fun multipleDerivedCardResourceTypesAreRejected() {
    val card =
        CardDefinition(
            CardData(
                name = "AmbiguousResourceCard",
                effects =
                    listOf(
                        "End: VictoryPoint / Animal<This>",
                        "End: VictoryPoint / Microbe<This>",
                    ),
            )
        )
    assertFailsWith<IllegalArgumentException> {
      authority(setOf(card)).cardResourceType(card.className)
    }
  }

  private fun authority(cards: Set<CardDefinition>): TfmAuthority =
      TfmAuthority.compose(
          object : TfmAuthority() {
            override val explicitClassDeclarations = resourceDeclarations.toSet()
          },
          object : TfmAuthority() {
            override val cardDefinitions = cards
          },
      )

  @Test
  internal fun testRoundTripForAllCanonCardData() { // move to canon
    val oops =
        Canon.cardDefinitions
            .flatMap { it.asClassDeclaration.allNodes }
            .filter { it != parse(it.kind, "$it") }
    oops.shouldBeEmpty()
  }
}
