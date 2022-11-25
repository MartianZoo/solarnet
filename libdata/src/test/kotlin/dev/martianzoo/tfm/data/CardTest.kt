package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Card.Deck.CORPORATION
import dev.martianzoo.tfm.data.Card.Deck.PRELUDE
import dev.martianzoo.tfm.data.Card.Deck.PROJECT
import dev.martianzoo.tfm.data.Card.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.Card.ProjectKind.AUTOMATED
import dev.martianzoo.tfm.data.Card.ProjectKind.EVENT
import dev.martianzoo.tfm.petaform.api.PetaformNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CardTest {
  /**
   * This is honestly an incredibly stupid test that data classes shouldn't need to have.
   */
  @Test
  fun minimal() {
    val dumbCard = Card("xxx", deck = PRELUDE, effectsPetaform = setOf("This: Plant"))

    assertThat(dumbCard.id).isEqualTo("xxx")
    assertThat(dumbCard.bundle).isNull()
    assertThat(dumbCard.deck).isEqualTo(PRELUDE)
    assertThat(dumbCard.replacesId).isNull()
    assertThat(dumbCard.tagsPetaform).isEmpty()
    assertThat(dumbCard.effectsPetaform).containsExactly("This: Plant")
    assertThat(dumbCard.resourceTypePetaform).isNull()
    assertThat(dumbCard.requirementPetaform).isNull()
    assertThat(dumbCard.cost).isEqualTo(0)
    assertThat(dumbCard.projectKind).isNull()
  }

  val BIRDS = Card(
      id = "072",
      bundle = "B",
      deck = PROJECT,
      tagsPetaform = listOf("AnimalTag"),
      immediatePetaform = setOf("PROD[-2 Plant<Anyone>]"),
      actionsPetaform = setOf("-> Animal<This>"),
      effectsPetaform = setOf("End: VictoryPoint / Animal<This>"),
      resourceTypePetaform = "Animal",
      requirementPetaform = "13 OxygenStep",
      cost = 10,
      projectKind = ACTIVE,
  )

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardFromApi() {
    assertThat(BIRDS.id).isEqualTo("072")
    assertThat(BIRDS.bundle).isEqualTo("B")
    assertThat(BIRDS.deck).isEqualTo(PROJECT)
    assertThat(BIRDS.tagsPetaform).containsExactly("AnimalTag")
    assertThat(BIRDS.immediatePetaform).containsExactly("PROD[-2 Plant<Anyone>]")
    assertThat(BIRDS.actionsPetaform).containsExactly("-> Animal<This>")
    assertThat(BIRDS.effectsPetaform).containsExactly("End: VictoryPoint / Animal<This>")
    assertThat(BIRDS.replacesId).isNull()
    assertThat(BIRDS.resourceTypePetaform).isEqualTo("Animal")
    assertThat(BIRDS.requirementPetaform).isEqualTo("13 OxygenStep")
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
            "immediate": [ "PROD[-2 Plant<Anyone>]" ],
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
    assertThrows<RuntimeException> { C.copy(resourceTypePetaform = "") }
    assertThrows<RuntimeException> { C.copy(requirementPetaform = "") }
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
      C.copy(projectKind = EVENT, effectsPetaform = setOf("Foo: Bar"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, effectsPetaform = setOf("Bar: Qux"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = EVENT, actionsPetaform = setOf("Foo -> Bar"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, actionsPetaform = setOf("Bar -> Qux"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, resourceTypePetaform = "Whatever")
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = ACTIVE, immediatePetaform = setOf("Whatever"))
    }
  }

  @Test fun birdsFromDataFile() {
    val cards = Canon.cardData
    assertThat(cards["072"]).isEqualTo(BIRDS)
  }

  @Test fun slurp() {
    Canon.cardData.values.forEach { card ->
      card.requirement?.let { assertThat("$it").isEqualTo(card.requirementPetaform) }
      card.resourceType?.let { assertThat("$it").isEqualTo(card.resourceTypePetaform) }

      checkRoundTrip(card.tagsPetaform, card.tags)
      if (card.immediatePetaform.isNotEmpty()) {
        checkRoundTrip(listOf(card.immediatePetaform.joinToString()), listOf(card.immediate!!))
      } else {
        assertThat(card.immediate).isNull()
      }
      checkRoundTrip(card.actionsPetaform, card.actions)
      checkRoundTrip(card.effectsPetaform, card.effects)
    }
  }

  private fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
