package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class CardDefinitionTest {
  /** This is honestly an incredibly stupid test that data classes shouldn't need to have. */
  @Test
  fun minimal() {
    val dumbCard = CardData("123", deck = "PRELUDE", immediate = "Plant", bundle = "Z")

    assertThat(dumbCard.id).isEqualTo("123")
    assertThat(dumbCard.bundle).isEqualTo("Z")
    assertThat(dumbCard.deck).isEqualTo("PRELUDE")
    assertThat(dumbCard.replaces).isNull()
    assertThat(dumbCard.tags).isEmpty()
    assertThat(dumbCard.immediate).isEqualTo("Plant")
    assertThat(dumbCard.resourceType).isNull()
    assertThat(dumbCard.requirement).isNull()
    assertThat(dumbCard.cost).isEqualTo(0)
    assertThat(dumbCard.projectKind).isNull()
  }

  val birds =
      CardData(
          id = "072",
          bundle = "B",
          deck = "PROJECT",
          tags = listOf("AnimalTag"),
          immediate = "PROD[-2 Plant<Anyone>]",
          actions = listOf("-> Animal<This>"),
          effects = setOf("End: VictoryPoint / Animal<This>"),
          resourceType = "Animal",
          requirement = "13 OxygenStep",
          cost = 10,
          projectKind = "ACTIVE",
      )

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardDataFromApi() {
    assertThat(birds.id).isEqualTo("072")
    assertThat(birds.bundle).isEqualTo("B")
    assertThat(birds.deck).isEqualTo("PROJECT")
    assertThat(birds.tags).containsExactly("AnimalTag")
    assertThat(birds.immediate).isEqualTo("PROD[-2 Plant<Anyone>]")
    assertThat(birds.actions).containsExactly("-> Animal<This>")
    assertThat(birds.effects).containsExactly("End: VictoryPoint / Animal<This>")
    assertThat(birds.replaces).isNull()
    assertThat(birds.resourceType).isEqualTo("Animal")
    assertThat(birds.requirement).isEqualTo("13 OxygenStep")
    assertThat(birds.cost).isEqualTo(10)
    assertThat(birds.projectKind).isEqualTo("ACTIVE")
  }

  @Test
  fun realCardDefinitionFromApi() {
    val birds = CardDefinition(birds)
    assertThat(birds.shortName).isEqualTo(cn("C072"))
    assertThat(birds.bundle).isEqualTo("B")
    assertThat(birds.deck).isEqualTo(PROJECT)
    assertThat(birds.tags.toStrings()).containsExactly("AnimalTag")
    assertThat(birds.immediate!!.toString()).isEqualTo("PROD[-2 Plant<Anyone>]")
    assertThat(birds.actions.toStrings()).containsExactly("-> Animal<This>")
    assertThat(birds.effects.toStrings()).containsExactly("End: VictoryPoint / Animal<This>")
    assertThat(birds.replaces).isNull()
    assertThat(birds.resourceType).isEqualTo(cn("Animal"))
    assertThat(birds.requirement?.toString()).isEqualTo("13 OxygenStep")
    assertThat(birds.cost).isEqualTo(10)
    assertThat(birds.projectInfo?.kind).isEqualTo(ACTIVE)
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

    assertThat(JsonReader.readCards(json)).containsExactly(birds)
  }

  // Just so we don't have to keep repeating the "x" part
  private val card: CardData = CardData("123", bundle = "Z")

  /** Since we only use C expecting an exception, we should make sure it normally works. */
  @Test
  fun justToBeSure() {
    @Suppress("UNUSED_VARIABLE") val card = card.copy(id = "123")
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
    assertFails { card.copy(projectKind = "EVENT", effects = setOf("Foo: Bar")) }
    assertFails { card.copy(projectKind = "AUTOMATED", effects = setOf("Bar: Qux")) }
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
    assertThat(oops).isEmpty()
  }
}
