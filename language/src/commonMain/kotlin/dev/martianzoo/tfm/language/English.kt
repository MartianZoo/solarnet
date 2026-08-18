package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE

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
      derivedBottomText(card) ?: if (hasBottomTextElement(card)) fallback() else ""

  private fun text(cardFront: ClassName): EnglishCardTextData.Text =
      EnglishCardTextData.byCardFront[cardFront] ?: error("No English text for $cardFront")

  // Immediate instructions are still treated as possible top elements. Of the card's Effects,
  // only End-triggered scoring is printed below the artwork.
  private fun hasTopTextElement(card: CardDefinition): Boolean =
      card.immediate != null || card.actions.isNotEmpty() || card.effects.isNotEmpty()

  private fun hasBottomTextElement(card: CardDefinition): Boolean =
      card.requirement != null || card.immediate != null || card.effects.any(::isEndEffect)

  private fun derivedBottomText(card: CardDefinition): String? {
    if (card.requirement != null || card.effects.any(::isEndEffect)) return null
    val instruction = card.immediate?.instructions?.singleOrNull() as? Gain ?: return null
    if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
    if (!instruction.gaining.simple) return null
    val count = (instruction.count as? ActualScalar)?.value ?: return null
    if (!isStandardResource(instruction.gaining.className)) return null
    return "Gain $count ${componentNoun(instruction.gaining.className, count)}."
  }

  private fun isStandardResource(className: ClassName): Boolean {
    val resourceClass = Canon.classTable.findClass(className) ?: return false
    return !resourceClass.abstract &&
        resourceClass.isSubtypeOf(Canon.classTable.getClass(STANDARD_RESOURCE))
  }

  private fun componentNoun(className: ClassName, count: Int): String =
      when {
        className == MEGACREDIT -> "M€"
        className == plant && count != 1 -> "plants"
        else -> unCamelCase(className.toString())
      }

  private fun unCamelCase(name: String): String = buildString {
    name.forEachIndexed { index, character ->
      val previous = name.getOrNull(index - 1)
      val next = name.getOrNull(index + 1)
      if (character == '_') {
        append(' ')
      } else {
        val startsWord =
            previous != null &&
                character.isUpperCase() &&
                (previous.isLowerCase() ||
                    previous.isDigit() ||
                    (previous.isUpperCase() && next?.isLowerCase() == true))
        if (startsWord) append(' ')
        append(character.lowercaseChar())
      }
    }
  }

  private fun isEndEffect(effect: Effect): Boolean {
    val cardTrigger = effect.trigger
    return cardTrigger is OnGainOf && cardTrigger.expression == endExpression
  }

  private val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    Canon.cardDefinitions.associateBy { it.className }
  }

  private val endExpression = cn("End").expression
  private val plant = cn("Plant")
}
