package dev.martianzoo.tfm.cardgenerator

import kotlin.test.Test
import kotlin.test.assertTrue

internal class GenerateCardPetsTest {
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
        "This: CARDS[SearchForCard(HAS PrintedTag<Class<ScienceTag>>)]",
        "MicrobeTag<Anyone>:",
    )
  }

  @Test
  internal fun supportingDeclarationsFollowTheirCard() {
    val colonies = CardPetsGenerator.renderBundle("ColoniesExpansion")
    assertInOrder(
        colonies,
        "CLASS Aridor :",
        "CLASS AridorPlayingEvent :",
        "CLASS AridorTagWatcher<Class<Tag>> :",
        "CLASS Arklight :",
    )
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
