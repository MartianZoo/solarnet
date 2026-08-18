package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition

/**
 * English card text, derived from canonical card definitions or read from the backing data file.
 */
public object English {
  /** Returns the text printed above the artwork on [cardFront]. */
  public fun topText(cardFront: ClassName): String {
    val card = cardsByClassName[cardFront] ?: return text(cardFront).top
    return topText(card) { text(cardFront).top }
  }

  /** Returns the text printed below the artwork on [cardFront]. */
  public fun bottomText(cardFront: ClassName): String {
    val card = cardsByClassName[cardFront] ?: return text(cardFront).bottom
    return bottomText(card) { text(cardFront).bottom }
  }

  internal fun topText(card: CardDefinition, fallback: () -> String): String =
      if (hasTopTextElement(card)) fallback() else ""

  internal fun bottomText(card: CardDefinition, fallback: () -> String): String =
      if (hasBottomTextElement(card)) fallback() else ""

  private fun text(cardFront: ClassName): EnglishCardTextData.Text =
      EnglishCardTextData.byCardFront[cardFront] ?: error("No English text for $cardFront")

  // An immediate instruction can be printed above the artwork, and an effect can be end scoring
  // below it. Until those shapes are distinguished, either keeps both regions data-backed.
  private fun hasTopTextElement(card: CardDefinition): Boolean =
      card.immediate != null || card.actions.isNotEmpty() || card.effects.isNotEmpty()

  private fun hasBottomTextElement(card: CardDefinition): Boolean =
      card.requirement != null || card.immediate != null || card.effects.isNotEmpty()

  private val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    Canon.cardDefinitions.associateBy { it.className }
  }
}
