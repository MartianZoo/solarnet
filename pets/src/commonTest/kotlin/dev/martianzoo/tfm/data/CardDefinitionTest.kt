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
          id = "072",
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
  fun realCardDefinitionFromApi() {
    val birds = CardDefinition(birds)
    birds.className shouldBe cn("Card072")
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
  fun cardWithoutRequirementOmitsThePropertyValue() {
    val card =
        CardDefinition(
            CardData(
                id = "001",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
            )
        )

    card.asClassDeclaration.properties.containsKey(PropertyName("requirement")) shouldBe false
  }

  @Test
  fun realCardFromJson() {
    val json =
        """
      {
        "cards": [
          {
            "id": "072",
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
  fun setupRequirementIsParsedAsPets() {
    val json =
        """
          {
            "cards": [{
              "id": "X40",
              "setupRequirement": "PreludeExpansion, VenusNextExpansion",
              "deck": "PRELUDE",
              "immediate": "Plant"
            }]
          }
        """

    val card = CardDefinition(JsonReader.readCards(json).single())

    card.setupRequirement.toString() shouldBe "PreludeExpansion, VenusNextExpansion"
  }

  @Test
  fun derivedClassAtPointOfUseLowersToAnOrdinaryCardLocalClass() {
    val card = CardDefinition(CardData(id = "T1", immediate = "Mandate { -> 3 ProjectCard }"))

    card.immediate.toString() shouldBe "CardT1_Mandate"
    val declaration = card.extraClasses.single()
    declaration.className shouldBe cn("CardT1_Mandate")
    declaration.supertypes.shouldContainExactly(parse<Expression>("Mandate"))
    declaration.effects.shouldContainExactly(parse<Effect>("UseAction1<This>: 3 ProjectCard"))
  }

  @Test
  fun derivedClassSuffixPreservesUseSiteRefinementsAndSpecializesItsSupertype() {
    val card =
        CardDefinition(
            CardData(
                id = "T2",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                immediate =
                    "CityTile<RemoteArea {}>, " +
                        "SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> { HAS MAX 1 This }",
            )
        )

    card.immediate.toString() shouldBe
        "CityTile<CardT2_RemoteArea>, " + "CardT2_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>"
    card.extraClasses
        .map { it.className }
        .shouldContainExactly(
            cn("CardT2_RemoteArea"),
            cn("CardT2_SpecialTile"),
        )
    val specialTile = card.extraClasses.last()
    specialTile.supertypes.shouldContainExactly(parse<Expression>("SpecialTile<LandArea>"))
    specialTile.invariants.shouldContainExactly(parse<Requirement>("MAX 1 This"))
  }

  @Test
  fun derivedClassBodyMustFollowTheCompleteExpressionAndCannotContainDefaults() {
    assertFailsWith<PetSyntaxException> {
      CardDefinition(CardData(id = "T3", immediate = "SpecialTile {}<LandArea>"))
    }
    assertFailsWith<PetSyntaxException> {
      CardDefinition(
          CardData(
              id = "T3",
              immediate = "SpecialTile<LandArea> { DEFAULT +SpecialTile<LandArea> }",
          )
      )
    }
  }

  @Test
  fun assignedDerivedClassNameMayBeReferencedElsewhereInTheParentInstruction() {
    val card =
        CardDefinition(
            CardData(
                id = "T3",
                immediate = "Mandate { -> ProjectCard } THEN Link<CardT3_Mandate>",
            )
        )

    card.immediate.toString() shouldBe "CardT3_Mandate THEN Link<CardT3_Mandate>"
  }

  @Test
  fun repeatedUnnamedDerivedClassMustBeExplicit() {
    assertFailsWith<PetSyntaxException> {
      CardDefinition(
          CardData(
              id = "T4",
              deck = "PROJECT",
              projectKind = "AUTOMATED",
              requirement = "Mandate {} OR Mandate { HAS MAX 1 This }",
          )
      )
    }
  }

  @Test
  fun derivedClassesCannotContainDerivedClasses() {
    listOf(
            "Mandate { -> NextCardEffect {} }",
            "Mandate<NextCardEffect {}> {}",
        )
        .forEach { source ->
          assertFailsWith<PetSyntaxException>(source) {
            CardDefinition(CardData(id = "T5", immediate = source))
          }
        }
  }

  // Just so we don't have to keep repeating the "x" part
  private val card: CardData = CardData("123")

  @Test
  fun emptyStrings() {
    assertFails { CardData("") }
    assertFails { card.copy(setupRequirement = "") }
    assertFails { card.copy(replaces = "") }
    assertFails { card.copy(resourceType = "") }
    assertFails { card.copy(requirement = "") }
  }

  @Test
  fun badCost() {
    assertFails { card.copy(cost = -1) }
    assertFails { card.copy(deck = "PRELUDE", cost = 1) }
    assertFails { card.copy(deck = "CORPORATION", cost = 1) }
  }

  @Test
  fun badProjectKind() {
    assertFails { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFails { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
    assertFails { card.copy(deck = "PROJECT") }
  }

  @Test
  fun badRequirement() {
    assertFails { card.copy(deck = "CORPORATION", projectKind = "ACTIVE") }
    assertFails { card.copy(deck = "PRELUDE", projectKind = "AUTOMATED") }
  }

  @Test
  fun badActiveCard() {
    assertFails { card.copy(projectKind = "EVENT", effects = listOf("Foo: Bar")) }
    assertFails { card.copy(projectKind = "AUTOMATED", effects = listOf("Bar: Qux")) }
    assertFails { card.copy(projectKind = "EVENT", actions = listOf("Foo -> Bar")) }
    assertFails { card.copy(projectKind = "AUTOMATED", actions = listOf("Bar -> Qux")) }
    assertFails { card.copy(projectKind = "AUTOMATED", resourceType = "Whatever") }
    assertFails { card.copy(projectKind = "ACTIVE", immediate = "Whatever") }
  }

  @Test
  fun testRoundTripForAllCanonCardData() { // move to canon
    val oops =
        Canon.cardDefinitions
            .flatMap { it.asClassDeclaration.allNodes }
            .filter { it != parse(it.kind, "$it") }
    oops.shouldBeEmpty()
  }
}
