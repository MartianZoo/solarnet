package dev.martianzoo.tfm.data

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CardDefinitionTest {
  private val birds =
      CardData(
          name = "Birds",
          deck = "PROJECT",
          tags = listOf("AnimalTag"),
          immediate = "PROD[-2 Plant<Anyone>]",
          actions = listOf("-> Animal<This>"),
          effects = listOf("End: VictoryPoint / Animal<This>"),
          resourceType = "Animal",
          requirement = "13 OxygenStep",
          cost = 10,
          projectKind = "ACTIVE",
      )

  @Test
  internal fun realCardDefinitionFromApi() {
    val birds = CardDefinition(birds)
    birds.className shouldBe cn("Birds")
    birds.deck shouldBe PROJECT
    birds.tags.toStrings().shouldContainExactlyInAnyOrder("AnimalTag")
    birds.immediate!!.toString() shouldBe "PROD[-2 Plant<Anyone>]"
    birds.actions.toStrings().shouldContainExactlyInAnyOrder("-> Animal<This>")
    birds.effects.toStrings().shouldContainExactlyInAnyOrder("End: VictoryPoint / Animal<This>")
    birds.replaces shouldBe null
    birds.resourceType shouldBe cn("Animal")
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
    prelude.asClassDeclaration.supertypes.shouldContainExactly(parse<Expression>("CardFront"))
    event.tags.toStrings().shouldContainExactly("EventTag")
    event.asClassDeclaration.supertypes.shouldContainExactly(parse<Expression>("EventCard"))

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
            "resourceType": "Animal",
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
                        "CARDS[4 ProjectCard<Selecting>, 2 ProjectCard FROM ProjectCard<Selecting>], " +
                        "CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS SpaceTag): ProjectCard) OR Ok)], " +
                        "CARDS[2 ProjectCard FROM ProjectCard<EventPile>?]",
            )
        )

    card.immediate.toString() shouldBe
        "CARDS[2 ProjectCard(HAS Citations<Class<Floater>>)], " +
            "CARDS[4 ProjectCard<Selecting>, 2 ProjectCard FROM ProjectCard<Selecting>], " +
            "CARDS[ProjectCard<Revealed> THEN ((ProjectCard<Revealed>(HAS SpaceTag): ProjectCard) OR Ok)], " +
            "CARDS[2 ProjectCard FROM ProjectCard<EventPile>?]"
    card.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "This: 2 ProjectCard, 2 ProjectCard, ProjectCard?, 2 ProjectCard FROM PlayedEvent?"
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
    val filtered =
        CardDefinition(
            CardData(
                name = "FilteredCardPurchase",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions =
                    listOf(
                        "-> CARDS[2 ProjectCard<Selecting> THEN 2 ProjectCard FROM ProjectCard<Selecting>(HAS VenusTag). THEN -2 ProjectCard<Selecting>? THEN BuySelectedCards]"
                    ),
            )
        )

    single.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: BuyCard?")
    )
    filtered.asClassDeclaration.effects.shouldContainExactly(
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
    assertFails { CardData("") }
    assertFails { card.copy(replaces = "") }
    assertFails { card.copy(resourceType = "") }
    assertFails { card.copy(requirement = "") }
  }

  @Test
  internal fun badCost() {
    assertFails { card.copy(cost = -1) }
    assertFails { card.copy(deck = "PRELUDE", cost = 1) }
    assertFails { card.copy(deck = "CORPORATION", cost = 1) }
  }

  @Test
  internal fun badProjectKind() {
    assertFails { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFails { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
    assertFails { card.copy(deck = "PROJECT") }
  }

  @Test
  internal fun badRequirement() {
    assertFails { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFails { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
  }

  @Test
  internal fun badActiveCard() {
    assertFails { card.copy(projectKind = "EVENT", effects = listOf("Foo: Bar")) }
    assertFails { card.copy(projectKind = "AUTOMATED", effects = listOf("Bar: Qux")) }
    assertFails { card.copy(projectKind = "EVENT", actions = listOf("Foo -> Bar")) }
    assertFails { card.copy(projectKind = "AUTOMATED", actions = listOf("Bar -> Qux")) }
    assertFails { card.copy(projectKind = "AUTOMATED", resourceType = "Whatever") }
    assertFails { card.copy(projectKind = "ACTIVE", immediate = "Whatever") }
  }

  @Test
  internal fun testRoundTripForAllCanonCardData() { // move to canon
    val oops =
        Canon.cardDefinitions
            .flatMap { it.asClassDeclaration.allNodes }
            .filter { it != parse(it.kind, "$it") }
    oops.shouldBeEmpty()
  }
}
