package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName

/** English card text, currently backed by the card-text data file. */
public object English {
  /** Returns the text printed above the artwork on [cardFront]. */
  public fun topText(cardFront: ClassName): String = text(cardFront).top

  /** Returns the text printed below the artwork on [cardFront]. */
  public fun bottomText(cardFront: ClassName): String = text(cardFront).bottom

  private fun text(cardFront: ClassName): EnglishCardTextData.Text =
      EnglishCardTextData.byCardFront[cardFront] ?: error("No English text for $cardFront")
}
