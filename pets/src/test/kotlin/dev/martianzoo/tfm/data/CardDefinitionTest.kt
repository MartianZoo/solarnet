package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition.Deck.CORPORATION
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.AUTOMATED
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.EVENT
import dev.martianzoo.tfm.pets.ast.PetsNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CardDefinitionTest {
  /**
   * This is honestly an incredibly stupid test that data classes shouldn't need to have.
   */
  @Test
  fun minimal() {
    val dumbCard = CardDefinition("xxx", deck = PRELUDE, effectsText = setOf("This: Plant"))

    assertThat(dumbCard.id).isEqualTo("xxx")
    assertThat(dumbCard.bundle).isNull()
    assertThat(dumbCard.deck).isEqualTo(PRELUDE)
    assertThat(dumbCard.replaces).isNull()
    assertThat(dumbCard.tagsText).isEmpty()
    assertThat(dumbCard.effectsText).containsExactly("This: Plant")
    assertThat(dumbCard.resourceTypeText).isNull()
    assertThat(dumbCard.requirementText).isNull()
    assertThat(dumbCard.cost).isEqualTo(0)
    assertThat(dumbCard.projectKind).isNull()
  }

  val BIRDS = CardDefinition(
      id = "072",
      bundle = "B",
      deck = PROJECT,
      tagsText = listOf("AnimalTag"),
      immediateText = setOf("PROD[-2 Plant<Anyone>]"),
      actionsText = setOf("-> Animal<This>"),
      effectsText = setOf("End: VictoryPoint / Animal<This>"),
      resourceTypeText = "Animal",
      requirementText = "13 OxygenStep",
      cost = 10,
      projectKind = ACTIVE,
  )

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardFromApi() {
    assertThat(BIRDS.id).isEqualTo("072")
    assertThat(BIRDS.bundle).isEqualTo("B")
    assertThat(BIRDS.deck).isEqualTo(PROJECT)
    assertThat(BIRDS.tagsText).containsExactly("AnimalTag")
    assertThat(BIRDS.immediateText).containsExactly("PROD[-2 Plant<Anyone>]")
    assertThat(BIRDS.actionsText).containsExactly("-> Animal<This>")
    assertThat(BIRDS.effectsText).containsExactly("End: VictoryPoint / Animal<This>")
    assertThat(BIRDS.replaces).isNull()
    assertThat(BIRDS.resourceTypeText).isEqualTo("Animal")
    assertThat(BIRDS.requirementText).isEqualTo("13 OxygenStep")
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

    assertThat(JsonReader.readCards(json)).containsExactly("072", BIRDS)
  }

  // Just so we don't have to keep repeating the "x" part
  private val C: CardDefinition = CardDefinition("x")

  /** Since we only use C expecting an exception, we should make sure it normally works. */
  @Test
  fun justToBeSure() {
    @Suppress("UNUSED_VARIABLE")
    val card = C.copy(id = "y")
  }

  @Test
  fun emptyStrings() {
    assertThrows<RuntimeException> { CardDefinition("") }
    assertThrows<RuntimeException> { C.copy(bundle = "") }
    assertThrows<RuntimeException> { C.copy(replaces = "") }
    assertThrows<RuntimeException> { C.copy(resourceTypeText = "") }
    assertThrows<RuntimeException> { C.copy(requirementText = "") }
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
      C.copy(projectKind = EVENT, effectsText = setOf("Foo: Bar"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, effectsText = setOf("Bar: Qux"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = EVENT, actionsText = setOf("Foo -> Bar"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, actionsText = setOf("Bar -> Qux"))
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = AUTOMATED, resourceTypeText = "Whatever")
    }
    assertThrows<RuntimeException> {
      C.copy(projectKind = ACTIVE, immediateText = setOf("Whatever"))
    }
  }

  @Test fun birdsFromDataFile() {
    val cards = Canon.cardDefinitions
    assertThat(cards["072"]).isEqualTo(BIRDS)
  }

  @Test fun slurp() {
    Canon.cardDefinitions.values.forEach { card ->
      card.requirement?.let { assertThat("$it").isEqualTo(card.requirementText) }
      card.resourceType?.let { assertThat("$it").isEqualTo(card.resourceTypeText) }

      checkRoundTrip(card.tagsText, card.tags)
      if (card.immediateText.isNotEmpty()) {
        checkRoundTrip(listOf(card.immediateText.joinToString()), listOf(card.immediate!!))
      } else {
        assertThat(card.immediate).isNull()
      }
      checkRoundTrip(card.actionsText, card.actions)
      checkRoundTrip(card.effectsText, card.effects)
    }
  }

  private fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetsNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
