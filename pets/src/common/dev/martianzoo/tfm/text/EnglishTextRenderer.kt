package dev.martianzoo.tfm.text

import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable

/** Derives printed English text for cards, milestones, and awards from Pets. */
public class EnglishTextRenderer(classTable: ClassTable) {
  private val english: English = English(classTable, TerraformingMarsDescribers.descriptions)

  /** Returns the two printed text regions for [card]. */
  public fun renderCard(card: Class): EnglishCardText {
    val rendering = english.renderCard(card)
    return EnglishCardText(rendering.top, rendering.bottom)
  }

  /** Returns the single printed sentence for a concrete milestone or award. */
  public fun renderGoal(goal: Class): String = english.renderGoal(goal).text
}
