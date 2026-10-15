package dev.martianzoo.tfm.text

import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable

/** Derives printed English card text from Pets and a loaded Terraforming Mars vocabulary. */
public class EnglishCardTextRenderer(classTable: ClassTable) {
  private val english: English = English(classTable, TerraformingMarsDescribers.descriptions)

  public fun render(card: Class): EnglishCardText {
    val rendering = english.renderCard(card)
    return EnglishCardText(rendering.top, rendering.bottom)
  }
}
