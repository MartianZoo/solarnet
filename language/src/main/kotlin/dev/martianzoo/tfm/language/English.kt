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
  public fun describe(effect: Effect): String = renderEffect(effect, describers).value

  /** Returns complete English sentences describing [actions] as one action region. */
  public fun describe(actions: List<Action>): String = renderActions(actions, describers).value

  /** Returns complete English sentences describing [actions] as actions on [card]. */
  public fun describe(actions: List<Action>, card: CardDefinition): String =
      renderActions(actions, describers, drawFilter(card)).value

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  public fun describe(instructionTree: InstructionTree): String =
      renderInstructionTree(instructionTree, describers).value

  /**
   * Returns complete English sentences describing [instructionTree] as an instruction on [card].
   */
  public fun describe(instructionTree: InstructionTree, card: CardDefinition): String =
      renderInstructionTree(instructionTree, describers, drawFilter(card)).value

  /** Returns complete English sentences describing [requirement]. */
  public fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).value

  /** Returns the best available text above [card]'s artwork. */
  public fun topText(card: CardDefinition): String = renderTopText(card).value

  /** Returns the best available text below [card]'s artwork. */
  public fun bottomText(card: CardDefinition): String = renderBottomText(card).value

  internal fun renderCard(card: CardDefinition): EnglishCardRendering {
    val top = renderTopText(card)
    val bottom = renderBottomText(card)
    return EnglishCardRendering(top.value, bottom.value, top.unresolved + bottom.unresolved)
  }

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun renderBottomText(card: CardDefinition): Rendering<String> {
    val requirement = card.requirement?.let { renderRequirement(it, describers) }
    val immediateEffects =
        card.effects.filter(::isImmediateSelfEffect).map {
          renderInstructionTree(it.instruction, describers, drawFilter(card))
        }
    val instructions =
        card.immediate?.let { renderInstructionTree(it, describers, drawFilter(card)) }
    val scoring =
        card.effects.filter { isEndEffect(it, describers) }.map { renderEffect(it, describers) }
    return joinRenderings(
        listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring
    )
  }

  private fun renderTopText(card: CardDefinition): Rendering<String> {
    val actions =
        card.actions
            .takeIf { it.isNotEmpty() }
            ?.let {
              renderActions(it, describers, drawFilter(card)).map { text -> "Action: $text" }
            }
    val effects =
        card.effects
            .filterNot { isEndEffect(it, describers) || isImmediateSelfEffect(it) }
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
              renderEffects(list, describers, drawFilter(card), card.resourceType).map { text ->
                "Effect: $text"
              }
            }
    return joinRenderings(listOfNotNull(actions, effects), " / ")
  }

  private fun joinRenderings(
      renderings: List<Rendering<String>>,
      separator: String = " ",
  ): Rendering<String> =
      Rendering(
          renderings.joinToString(separator) { it.value },
          renderings.flatMap { it.unresolved },
      )

  private fun drawFilter(card: CardDefinition) = EnglishFilteredDrawData.byCardFront[card.className]

  private fun isImmediateSelfEffect(effect: Effect): Boolean {
    return effect.automatic && effect.trigger == WhenGain
  }
}

internal data class EnglishCardRendering(
    val top: String,
    val bottom: String,
    val unresolved: List<Unresolved>,
)
