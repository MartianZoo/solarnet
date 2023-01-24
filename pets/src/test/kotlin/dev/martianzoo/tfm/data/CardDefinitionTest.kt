package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition.Deck.CORPORATION
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.AUTOMATED
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.EVENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.testlib.assertFails
import org.junit.jupiter.api.Test

private class CardDefinitionTest {
  /** This is honestly an incredibly stupid test that data classes shouldn't need to have. */
  @Test
  fun minimal() {
    val dumbCard =
        CardDefinition("123", deck = PRELUDE, effectsText = setOf("This: Plant"), bundle = "Z")

    assertThat(dumbCard.id).isEqualTo(cn("C123"))
    assertThat(dumbCard.bundle).isEqualTo("Z")
    assertThat(dumbCard.deck).isEqualTo(PRELUDE)
    assertThat(dumbCard.replaces).isNull()
    assertThat(dumbCard.tagsText).isEmpty()
    assertThat(dumbCard.effectsText).containsExactly("This: Plant")
    assertThat(dumbCard.resourceTypeText).isNull()
    assertThat(dumbCard.requirementText).isNull()
    assertThat(dumbCard.cost).isEqualTo(0)
    assertThat(dumbCard.projectKind).isNull()
  }

  val birds =
      CardDefinition(
          idRaw = "072",
          bundle = "B",
          deck = PROJECT,
          tagsText = listOf("AnimalTag"),
          immediateText = "PROD[-2 Plant<Anyone>]",
          actionsText = listOf("-> Animal<This>"),
          effectsText = setOf("End: VictoryPoint / Animal<This>"),
          resourceTypeText = "Animal",
          requirementText = "13 OxygenStep",
          cost = 10,
          projectKind = ACTIVE,
      )

  /** This test is also quite pointless, but shows an example usage for readers. */
  @Test
  fun realCardFromApi() {
    assertThat(birds.id).isEqualTo(cn("C072"))
    assertThat(birds.bundle).isEqualTo("B")
    assertThat(birds.deck).isEqualTo(PROJECT)
    assertThat(birds.tagsText).containsExactly("AnimalTag")
    assertThat(birds.immediateText).isEqualTo("PROD[-2 Plant<Anyone>]")
    assertThat(birds.actionsText).containsExactly("-> Animal<This>")
    assertThat(birds.effectsText).containsExactly("End: VictoryPoint / Animal<This>")
    assertThat(birds.replaces).isNull()
    assertThat(birds.resourceTypeText).isEqualTo("Animal")
    assertThat(birds.requirementText).isEqualTo("13 OxygenStep")
    assertThat(birds.cost).isEqualTo(10)
    assertThat(birds.projectKind).isEqualTo(ACTIVE)
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
  private val card: CardDefinition = CardDefinition("123", bundle = "Z")

  /** Since we only use C expecting an exception, we should make sure it normally works. */
  @Test
  fun justToBeSure() {
    @Suppress("UNUSED_VARIABLE") val card = card.copy(idRaw = "123")
  }

  @Test
  fun emptyStrings() {
    assertFails { CardDefinition("", bundle = "Z") }
    assertFails { card.copy(bundle = "") }
    assertFails { card.copy(replaces = "") }
    assertFails { card.copy(resourceTypeText = "") }
    assertFails { card.copy(requirementText = "") }
  }

  @Test
  fun badCost() {
    assertFails { card.copy(cost = -1) }
    assertFails { card.copy(deck = PRELUDE, cost = 1) }
    assertFails { card.copy(deck = CORPORATION, cost = 1) }
  }

  @Test
  fun badProjectKind() {
    assertFails { card.copy(deck = CORPORATION, projectKind = ACTIVE) }
    assertFails { card.copy(deck = PRELUDE, projectKind = AUTOMATED) }
    assertFails { card.copy(deck = PROJECT) }
  }

  @Test
  fun badRequirement() {
    assertFails { card.copy(deck = CORPORATION, projectKind = ACTIVE) }
    assertFails { card.copy(deck = PRELUDE, projectKind = AUTOMATED) }
  }

  @Test
  fun badActiveCard() {
    assertFails {
      card.copy(projectKind = EVENT, effectsText = setOf("Foo: Bar"))
    }
    assertFails {
      card.copy(projectKind = AUTOMATED, effectsText = setOf("Bar: Qux"))
    }
    assertFails {
      card.copy(projectKind = EVENT, actionsText = listOf("Foo -> Bar"))
    }
    assertFails {
      card.copy(projectKind = AUTOMATED, actionsText = listOf("Bar -> Qux"))
    }
    assertFails {
      card.copy(projectKind = AUTOMATED, resourceTypeText = "Whatever")
    }
    assertFails { card.copy(projectKind = ACTIVE, immediateText = "Whatever") }
  }

  @Test
  fun birdsFromCanon() {
    val card = Canon.cardDefinitions.first { it.id == cn("C072") }
    assertThat(card).isEqualTo(birds)
  }

  @Test
  fun testRoundTripForAllCanonCardData() {
    Canon.cardDefinitions.forEach { card ->
      card.requirementRaw?.let { assertThat("$it").isEqualTo(card.requirementText) }
      card.resourceType?.let { assertThat("$it").isEqualTo(card.resourceTypeText) }

      checkRoundTrip(card.tagsText, card.tags)
      checkRoundTrip(listOfNotNull(card.immediateText), listOfNotNull(card.immediateRaw))
      checkRoundTrip(card.actionsText, card.actionsRaw)
      checkRoundTrip(card.effectsText, card.effectsRaw)
    }
  }

  private fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach { assertThat("${it.second}").isEqualTo(it.first) }
  }
}
