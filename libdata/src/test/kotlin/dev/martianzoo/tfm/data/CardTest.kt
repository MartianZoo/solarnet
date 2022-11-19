/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.Card.Deck.CORPORATION
import dev.martianzoo.tfm.data.Card.Deck.PRELUDE
import dev.martianzoo.tfm.data.Card.Deck.PROJECT
import dev.martianzoo.tfm.data.Card.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.Card.ProjectKind.AUTOMATED
import dev.martianzoo.tfm.data.Card.ProjectKind.EVENT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CardTest {
  /**
   * This is honestly an incredibly stupid test that data classes shouldn't need to have.
   */
  @Test
  fun minimal() {
    val dumbCard = Card("xxx", deck = PRELUDE, effects = listOf("This: Plant"))

    assertThat(dumbCard.id).isEqualTo("xxx")
    assertThat(dumbCard.bundle).isNull()
    assertThat(dumbCard.deck).isEqualTo(PRELUDE)
    assertThat(dumbCard.tags).isEmpty()
    assertThat(dumbCard.effects).containsExactly("This: Plant")
    assertThat(dumbCard.replacesId).isNull()
    assertThat(dumbCard.resourceType).isNull()
    assertThat(dumbCard.requirement).isNull()
    assertThat(dumbCard.cost).isEqualTo(0)
    assertThat(dumbCard.projectKind).isNull()
  }

  val BIRDS = Card(
      id = "072",
      bundle = "B",
      deck = PROJECT,
      tags = listOf("AnimalTag"),
      effects = listOf(
          "This: PROD[-2 Plant<Anyone>]",
          "-> Animal<This>",
          "End: VictoryPoint / Animal<This>",
      ),
      resourceType = "Animal",
      requirement = "13 OxygenStep",
      cost = 10,
      projectKind = ACTIVE,
  )

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardFromApi() {
    assertThat(BIRDS.id).isEqualTo("072")
    assertThat(BIRDS.bundle).isEqualTo("B")
    assertThat(BIRDS.deck).isEqualTo(PROJECT)
    assertThat(BIRDS.tags).containsExactly("AnimalTag")
    assertThat(BIRDS.effects).containsExactly(
        "This: PROD[-2 Plant<Anyone>]",
        "-> Animal<This>",
        "End: VictoryPoint / Animal<This>",
    ).inOrder()
    assertThat(BIRDS.replacesId).isNull()
    assertThat(BIRDS.resourceType).isEqualTo("Animal")
    assertThat(BIRDS.requirement).isEqualTo("13 OxygenStep")
    assertThat(BIRDS.cost).isEqualTo(10)
    assertThat(BIRDS.projectKind).isEqualTo(ACTIVE)
  }

  @Test
  fun realCardFromJson() {
    val json = """
      {
        "cards": [
          {
            "id": "072",
            "bundle": "B",
            "deck": "PROJECT",
            "tags": [ "AnimalTag" ],
            "effects": [
              "This: PROD[-2 Plant<Anyone>]",
              "-> Animal<This>",
              "End: VictoryPoint / Animal<This>"
            ],
            "resourceType": "Animal",
            "requirement": "13 OxygenStep",
            "cost": 10,
            "projectKind": "ACTIVE"
          }
        ]
      }
    """

    assertThat(MoshiReader.readCards(json)).containsExactly("072", BIRDS)
  }

  // Just so we don't have to keep repeating the "x" part
  private val C: Card = Card("x")

  /** Since we only use C expecting an exception, we should make sure it normally works. */
  @Test
  fun justToBeSure() {
    @Suppress("UNUSED_VARIABLE")
    val card = C.copy(id = "y")
  }

  @Test
  fun emptyStrings() {
    assertThrows<RuntimeException> { Card("") }
    assertThrows<RuntimeException> { C.copy(bundle = "") }
    assertThrows<RuntimeException> { C.copy(replacesId = "") }
    assertThrows<RuntimeException> { C.copy(resourceType = "") }
    assertThrows<RuntimeException> { C.copy(requirement = "") }
  }

  @Test
  fun badCost() {
    assertThrows<RuntimeException> { C.copy(cost = -1) }
    assertThrows<RuntimeException> { C.copy(deck = PRELUDE, cost = 1) }
    assertThrows<RuntimeException> { C.copy(deck = CORPORATION, cost = 1) }
  }

  @Test
  fun badProjectKind() {
    assertThrows<RuntimeException> { C.copy(deck = CORPORATION, projectKind = ACTIVE) }
    assertThrows<RuntimeException> { C.copy(deck = PRELUDE, projectKind = AUTOMATED) }
    assertThrows<RuntimeException> { C.copy(deck = PROJECT) }
  }

  @Test
  fun badRequirement() {
    assertThrows<RuntimeException> { C.copy(deck = CORPORATION, projectKind = ACTIVE) }
    assertThrows<RuntimeException> { C.copy(deck = PRELUDE, projectKind = AUTOMATED) }
  }

  @Test
  fun badActiveCard() {
    assertThrows<RuntimeException> {
      C.copy(projectKind = EVENT, effects = listOf("Foo: Bar"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, effects = listOf("This: Foo", "Bar: Qux"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, resourceType = "Whatever")
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = ACTIVE, effects = listOf("This: Foo", "End: Bar"))
    }
  }
}
