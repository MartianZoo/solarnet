package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.types.Class

/** English Pets text using the complete component descriptions supplied by its client. */
public class English public constructor(descriptions: Map<Class, ComponentDescriber>) {
  private val describers = Describers(descriptions)

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

  /** Returns the text above [card]'s artwork, using [fallback] for unsupported content. */
  public fun topText(card: CardDefinition, fallback: () -> String): String =
      derivedTopText(card) ?: if (hasTopTextElement(card)) fallback() else ""

  /** Returns the text below [card]'s artwork, using [fallback] for unsupported content. */
  public fun bottomText(card: CardDefinition, fallback: () -> String): String =
      derivedBottomText(card) ?: if (hasBottomTextElement(card)) fallback() else ""

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun hasTopTextElement(card: CardDefinition): Boolean =
      card.actions.isNotEmpty() || card.effects.isNotEmpty()

  private fun hasBottomTextElement(card: CardDefinition): Boolean =
      card.requirement != null ||
          card.immediate != null ||
          card.effects.any { isEndEffect(it, describers) }

  private fun derivedBottomText(card: CardDefinition): String? {
    if (describers.hasBehaviorBearingExtraClass(card)) {
      return null
    }
    val requirement = card.requirement?.let { describeOrNull(it) ?: return null }
    val instructions = card.immediate?.let { describeOrNull(it, card) ?: return null }
    val scoring =
        card.effects
            .filter { isEndEffect(it, describers) }
            .map { describeOrNull(it) ?: return null }
    return (listOfNotNull(requirement, instructions) + scoring)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" ")
  }

  private fun derivedTopText(card: CardDefinition): String? {
    if (describers.hasBehaviorBearingExtraClass(card)) {
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
            .filterNot { isEndEffect(it, describers) }
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
              "Effect: ${renderEffects(list, describers) ?: return null}"
            }
    return listOfNotNull(actions, effects).joinToString(" / ")
  }

  private fun describeOrNull(effect: Effect): String? = renderEffect(effect, describers)

  private fun describeOrNull(
      actions: List<Action>,
      card: CardDefinition? = null,
  ): String? = renderActions(actions, card, describers)

  private fun describeOrNull(
      instructionTree: InstructionTree,
      card: CardDefinition? = null,
  ): String? = renderInstructionTree(instructionTree, card, describers)

  private fun describeOrNull(requirement: Requirement): String? =
      renderRequirement(requirement, describers)

  private fun unsupported(element: Any): Nothing =
      throw IllegalArgumentException("No English description for Pets element: $element")
}
