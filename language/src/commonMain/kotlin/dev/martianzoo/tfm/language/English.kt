package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition

/**
 * English card text, derived from canonical card definitions or read from the backing data file.
 */
public object English {
  /** Returns complete English sentences describing [effect]. */
  public fun describe(effect: Effect): String = describeOrNull(effect) ?: unsupported(effect)

  /** Returns complete English sentences describing [actions] as one action region. */
  public fun describe(actions: List<Action>): String =
      describeOrNull(actions) ?: unsupported(actions)

  /** Returns complete English sentences describing [actions] as actions on [card]. */
  public fun describe(actions: List<Action>, card: CardDefinition): String =
      describeOrNull(actions, card) ?: unsupported(actions)

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  public fun describe(instructionTree: InstructionTree): String =
      describeOrNull(instructionTree) ?: unsupported(instructionTree)

  /**
   * Returns complete English sentences describing [instructionTree] as an instruction on [card].
   */
  public fun describe(instructionTree: InstructionTree, card: CardDefinition): String =
      describeOrNull(instructionTree, card) ?: unsupported(instructionTree)

  /** Returns complete English sentences describing [requirement]. */
  public fun describe(requirement: Requirement): String =
      describeOrNull(requirement) ?: unsupported(requirement)

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
      derivedTopText(card) ?: if (hasTopTextElement(card)) fallback() else ""

  internal fun bottomText(card: CardDefinition, fallback: () -> String): String =
      derivedBottomText(card) ?: if (hasBottomTextElement(card)) fallback() else ""

  private fun text(cardFront: ClassName): EnglishCardTextData.Text =
      EnglishCardTextData.byCardFront[cardFront] ?: error("No English text for $cardFront")

  // Immediate instructions are still treated as possible top elements. Of the card's Effects,
  // only endgame scoring is printed below the artwork.
  private fun hasTopTextElement(card: CardDefinition): Boolean =
      card.immediate != null || card.actions.isNotEmpty() || card.effects.isNotEmpty()

  private fun hasBottomTextElement(card: CardDefinition): Boolean =
      card.requirement != null || card.immediate != null || card.effects.any(::isEndEffect)

  private fun derivedBottomText(card: CardDefinition): String? {
    if (Describers.hasBehaviorBearingExtraClass(card)) {
      return null
    }
    val requirement = card.requirement?.let { describeOrNull(it) ?: return null }
    val instructions = card.immediate?.let { describeOrNull(it, card) ?: return null }
    val scoring = card.effects.filter(::isEndEffect).map { describeOrNull(it) ?: return null }
    return (listOfNotNull(requirement, instructions) + scoring)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" ")
  }

  private fun derivedTopText(card: CardDefinition): String? {
    if (card.immediate != null || Describers.hasBehaviorBearingExtraClass(card)) {
      return null
    }
    val actions =
        card.actions
            .takeIf { it.isNotEmpty() }
            ?.let {
              "Action: ${describeOrNull(it, card) ?: return null}"
            }
    val effects =
        card.effects
            .filterNot(::isEndEffect)
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
              val rendered = list.map { describeOrNull(it) ?: return null }
              "Effect: ${rendered.joinToString(" ")}"
            }
    return listOfNotNull(actions, effects).joinToString(" / ")
  }

  private fun describeOrNull(effect: Effect): String? = renderEndEffect(effect)

  private fun describeOrNull(
      actions: List<Action>,
      card: CardDefinition? = null,
  ): String? = renderActions(actions, card)

  private fun describeOrNull(
      instructionTree: InstructionTree,
      card: CardDefinition? = null,
  ): String? = renderInstructionTree(instructionTree, card)

  private fun describeOrNull(requirement: Requirement): String? = renderRequirement(requirement)

  private fun unsupported(element: Any): Nothing =
      throw IllegalArgumentException("No English description for Pets element: $element")

  private val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    Canon.cardDefinitions.associateBy { it.className }
  }
}
