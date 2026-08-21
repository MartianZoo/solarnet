package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.types.Class

/** English Pets text using the client's complete map of sparse component descriptions. */
public class English public constructor(descriptions: Map<Class, ComponentDescriber>) {
  private val describers = Describers(descriptions)

  /** Returns complete English sentences describing [effect]. */
  public fun describe(effect: Effect): String = describeOrNull(effect) ?: rawSentence(effect)

  /** Returns complete English sentences describing [actions] as one action region. */
  public fun describe(actions: List<Action>): String =
      describeOrNull(actions) ?: rawSentence(actions.joinToString(" OR "))

  /** Returns complete English sentences describing [actions] as actions on [card]. */
  public fun describe(actions: List<Action>, card: CardDefinition): String =
      describeOrNull(actions, card) ?: rawSentence(actions.joinToString(" OR "))

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  public fun describe(instructionTree: InstructionTree): String =
      describeOrNull(instructionTree) ?: rawSentence(instructionTree)

  /**
   * Returns complete English sentences describing [instructionTree] as an instruction on [card].
   */
  public fun describe(instructionTree: InstructionTree, card: CardDefinition): String =
      describeOrNull(instructionTree, card) ?: rawSentence(instructionTree)

  /** Returns complete English sentences describing [requirement]. */
  public fun describe(requirement: Requirement): String =
      describeOrNull(requirement) ?: rawSentence(requirement)

  /** Returns the best available text above [card]'s artwork. */
  public fun topText(card: CardDefinition): String = derivedTopText(card)

  /** Returns the best available text below [card]'s artwork. */
  public fun bottomText(card: CardDefinition): String = derivedBottomText(card)

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun derivedBottomText(card: CardDefinition): String {
    val requirement = card.requirement?.let(::describe)
    val immediateEffects =
        card.effects.filter(::isImmediateSelfEffect).map { describe(it.instruction, card) }
    val instructions = card.immediate?.let { describe(it, card) }
    val scoring = card.effects.filter { isEndEffect(it, describers) }.map(::describe)
    return (listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring)
        .joinToString(" ")
  }

  private fun derivedTopText(card: CardDefinition): String {
    val actions = card.actions.takeIf { it.isNotEmpty() }?.let { "Action: ${describe(it, card)}" }
    val effects =
        card.effects
            .filterNot { isEndEffect(it, describers) || isImmediateSelfEffect(it) }
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
              "Effect: ${renderEffects(list, describers, drawFilter(card), card.resourceType)}"
            }
    return listOfNotNull(actions, effects).joinToString(" / ")
  }

  private fun describeOrNull(effect: Effect): String? = renderEffect(effect, describers)

  private fun describeOrNull(actions: List<Action>): String? = renderActions(actions, describers)

  private fun describeOrNull(actions: List<Action>, card: CardDefinition): String? =
      renderActions(actions, describers, drawFilter(card))

  private fun describeOrNull(instructionTree: InstructionTree): String? =
      renderInstructionTree(instructionTree, describers)

  private fun describeOrNull(
      instructionTree: InstructionTree,
      card: CardDefinition,
  ): String? = renderInstructionTree(instructionTree, describers, drawFilter(card))

  private fun describeOrNull(requirement: Requirement): String? =
      renderRequirement(requirement, describers)

  private fun drawFilter(card: CardDefinition) = EnglishFilteredDrawData.byCardFront[card.className]

  private fun isImmediateSelfEffect(effect: Effect): Boolean {
    return effect.automatic && effect.trigger == WhenGain
  }

  private fun rawSentence(element: Any): String = completeSentence("[$element]")
}
