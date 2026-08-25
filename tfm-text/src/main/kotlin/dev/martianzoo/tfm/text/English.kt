package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.CardDefinition
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
      renderActions(actions, describers.withSourceDeclarations(card.extraClasses)).value

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  public fun describe(instructionTree: InstructionTree): String =
      renderInstructionTree(instructionTree, describers).value

  /**
   * Returns complete English sentences describing [instructionTree] as an instruction on [card].
   */
  public fun describe(instructionTree: InstructionTree, card: CardDefinition): String =
      renderInstructionTree(
              instructionTree,
              describers.withSourceDeclarations(card.extraClasses),
          )
          .value

  /** Returns complete English sentences describing [requirement]. */
  public fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).value

  /** Returns the best available text above [card]'s artwork. */
  public fun topText(card: CardDefinition): String =
      renderTopText(card, describers.withSourceDeclarations(card.extraClasses)).value

  /** Returns the best available text below [card]'s artwork. */
  public fun bottomText(card: CardDefinition): String =
      renderBottomText(card, describers.withSourceDeclarations(card.extraClasses)).value

  internal fun renderCard(card: CardDefinition): EnglishCardRendering {
    val cardDescribers = describers.withSourceDeclarations(card.extraClasses)
    val top = renderTopText(card, cardDescribers)
    val bottom = renderBottomText(card, cardDescribers)
    return EnglishCardRendering(top.value, bottom.value, top.unresolved + bottom.unresolved)
  }

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun renderBottomText(
      card: CardDefinition,
      cardDescribers: Describers,
  ): Rendering<String> {
    val requirement = card.requirement?.let { renderRequirement(it, cardDescribers) }
    val immediateEffects =
        card.effects.filter(::isImmediateSelfEffect).map {
          renderInstructionTree(it.instruction, cardDescribers)
        }
    val instructions = card.immediate?.let { renderInstructionTree(it, cardDescribers) }
    val scoring =
        card.effects
            .filter { isEndEffect(it, cardDescribers) }
            .map {
              renderEffect(it, cardDescribers)
            }
    return joinRenderings(
        listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring
    )
  }

  private fun renderTopText(
      card: CardDefinition,
      cardDescribers: Describers,
  ): Rendering<String> {
    val persistentEffects =
        card.effects.filterNot { isEndEffect(it, cardDescribers) || isImmediateSelfEffect(it) }
    val integratedPayment =
        persistentEffects
            .mapIndexedNotNull { index, effect ->
              acceptedFirstActionPaymentResource(effect, cardDescribers)?.let { index to it }
            }
            .singleOrNull()
            ?.takeIf { card.actions.firstOrNull()?.cost != null }
    val actionsWithPayment =
        card.actions
            .takeIf { it.isNotEmpty() }
            ?.let { renderActions(it, cardDescribers, integratedPayment?.second) }
    val paymentWasIntegrated = integratedPayment != null
    val actions = actionsWithPayment?.map { text -> "Action: $text" }
    val effects =
        persistentEffects
            .filterIndexed { index, _ ->
              !paymentWasIntegrated || index != integratedPayment?.first
            }
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
              val resourceType =
                  card.resourceTypeCandidates.filter(cardDescribers::isCardResource).singleOrNull()
              renderEffects(list, cardDescribers, cardResourceType = resourceType).map { text ->
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

  private fun isImmediateSelfEffect(effect: Effect): Boolean {
    return effect.automatic && effect.trigger == WhenGain
  }
}

internal data class EnglishCardRendering(
    val top: String,
    val bottom: String,
    val unresolved: List<Unresolved>,
)
