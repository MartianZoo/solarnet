package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.tfm.carddata.CardData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GenerateCardPetsTest {
  @Test
  internal fun groupedDecksPreserveCardDefinitionsAndDeriveProjectKinds() {
    val cards = CardData.definitions("TerraformingMars").associateBy { it.name }

    assertEquals("StandardCorporationCard", cards.getValue("CrediCor").deck)
    assertEquals(null, cards.getValue("CrediCor").projectKind)
    assertEquals("AutomatedCard", cards.getValue("DeepWellHeating").projectKind)
    assertEquals("ActiveCard", cards.getValue("ArcticAlgae").projectKind)
    assertEquals("EventCard", cards.getValue("ImportedHydrogen").projectKind)
    assertEquals("EventTag", cards.getValue("ImportedHydrogen").tags.last())
  }

  @Test
  internal fun cardEffectsHaveStableSetupOrdering() {
    val colonies = CardPetsGenerator.renderBundle("ColoniesExpansion")
    assertInOrder(
        colonies.cardDeclaration("StormcraftIncorporated"),
        "This:: JovianTag<This>",
        "This: 48 MC",
        "Billing<HasActions, ActionSlot, Class<Heat>>:: AcceptingFromCard<This>",
        "PayFromCard<This>:: -2 Owed<Class<Heat>>",
    )

    val promos = CardPetsGenerator.renderBundle("PromoCardPack")
    assertInOrder(
        promos.cardDeclaration("PharmacyUnion"),
        "This:: 54 MC",
        "This:: 2 MicrobeTag<This>",
        "This: SearchForCard<TagFilter<Class<ScienceTag>>>",
        "MicrobeTag<Anyone>:",
    )
  }

  @Test
  internal fun supportingDeclarationsFollowTheirCard() {
    val colonies = CardPetsGenerator.renderBundle("ColoniesExpansion")
    assertInOrder(
        colonies,
        "CLASS Aridor :",
        "CLASS AridorTagWatcher<Class<@Tag>> :",
        "CLASS Arklight :",
    )
  }

  @Test
  internal fun authoredSpecialCardsFollowGeneratedCardsInTheSameResource() {
    val cards = renderCardPets("TurmoilExpansion", "CLASS ExampleGlobalEvent : GlobalEvent\n")

    assertInOrder(cards, "CLASS AerialLenses :", "CLASS ExampleGlobalEvent : GlobalEvent")
    assertTrue(cards.endsWith('\n'))
  }

  private fun String.cardDeclaration(name: String): String =
      substringAfter("CLASS $name ").substringBefore("\n}")

  private fun assertInOrder(source: String, vararg fragments: String) {
    var previous = -1
    fragments.forEach { fragment ->
      val next = source.indexOf(fragment)
      assertTrue(next > previous, "Expected '$fragment' after index $previous in:\n$source")
      previous = next
    }
  }
}
