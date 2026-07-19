package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardDefinitionTest {
  /** This is honestly an incredibly stupid test that data classes shouldn't need to have. */
  @Test
  fun minimal() {
    val dumbCard =
        CardData(
            "123",
            deck = "PRELUDE",
            loadRequirement = "HAS PreludeExpansion",
            immediate = "Plant",
            bundle = "Z",
        )

    dumbCard.id shouldBe "123"
    dumbCard.bundle shouldBe "Z"
    dumbCard.deck shouldBe "PRELUDE"
    dumbCard.replaces shouldBe null
    dumbCard.tags.shouldBeEmpty()
    dumbCard.immediate shouldBe "Plant"
    dumbCard.resourceType shouldBe null
    dumbCard.requirement shouldBe null
    dumbCard.cost shouldBe 0
    dumbCard.projectKind shouldBe null
  }

  val birds =
      CardData(
          id = "072",
          bundle = "B",
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

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardDataFromApi() {
    birds.id shouldBe "072"
    birds.bundle shouldBe "B"
    birds.deck shouldBe "PROJECT"
    birds.tags.shouldContainExactlyInAnyOrder("AnimalTag")
    birds.immediate shouldBe "PROD[-2 Plant<Anyone>]"
    birds.actions.shouldContainExactlyInAnyOrder("-> Animal<This>")
    birds.effects.shouldContainExactlyInAnyOrder("End: VictoryPoint / Animal<This>")
    birds.replaces shouldBe null
    birds.resourceType shouldBe "Animal"
    birds.requirement shouldBe "13 OxygenStep"
    birds.cost shouldBe 10
    birds.projectKind shouldBe "ACTIVE"
  }

  @Test
  fun realCardDefinitionFromApi() {
    val birds = CardDefinition(birds)
    birds.shortName shouldBe cn("C072")
    birds.bundle shouldBe "B"
    birds.deck shouldBe PROJECT
    birds.tags.toStrings().shouldContainExactlyInAnyOrder("AnimalTag")
    birds.immediate!!.toString() shouldBe "PROD[-2 Plant<Anyone>]"
    birds.actions.toStrings().shouldContainExactlyInAnyOrder("-> Animal<This>")
    birds.effects.toStrings().shouldContainExactlyInAnyOrder("End: VictoryPoint / Animal<This>")
    birds.replaces shouldBe null
    birds.resourceType shouldBe cn("Animal")
    birds.requirement?.toString() shouldBe "13 OxygenStep"
    birds.cost shouldBe 10
    birds.projectInfo?.kind shouldBe ACTIVE
  }

  @Test
  fun realCardFromJson() {
    val json =
        """
      {
        "cards": [
          {
            "id": "072",
            "bundle": "B",
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
  fun bundleProvenanceCanComeFromTheContainingDirectory() {
    val json =
        """
          {
            "cards": [{
              "id": "X40",
              "loadRequirement": "HAS PreludeExpansion",
              "deck": "PRELUDE",
              "immediate": "Plant"
            }]
          }
        """

    val card = CardDefinition(JsonReader.readCards(json, "PromosExpansion").single())

    card.bundle shouldBe "PromosExpansion"
  }

  @Test
  fun preludeCardsRequirePreludeExpansion() {
    val card =
        CardDefinition(
            CardData(
                id = "X40",
                deck = "PRELUDE",
                loadRequirement = "HAS PreludeExpansion",
                immediate = "Plant",
            ),
            "PromosExpansion",
        )

    card.loadRequirement.toString() shouldBe "PreludeExpansion"
  }

  // Just so we don't have to keep repeating the "x" part
  private val card: CardData = CardData("123", bundle = "Z")

  /** Since we only use C expecting an exception, we should make sure it normally works. */
  @Test
  fun justToBeSure() {
    card.copy(id = "123") shouldBe card
  }

  @Test
  fun emptyStrings() {
    assertFails { CardData("", bundle = "Z") }
    assertFails { card.copy(bundle = "") }
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
