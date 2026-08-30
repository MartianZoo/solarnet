package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardRequirement
import dev.martianzoo.tfm.canon.cardResourceType

/** English Pets text using the client's complete map of sparse component descriptions. */
internal class English public constructor(descriptions: Map<Class, ComponentDescriber>) {
  private val describers = Describers(descriptions)

  /** Returns complete English sentences describing [effect]. */
  internal fun describe(effect: Effect): String = renderEffect(effect, describers).value

  /** Returns complete English sentences describing [actions] as one action region. */
  internal fun describe(actions: List<Action>): String = renderActions(actions, describers).value

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  internal fun describe(instructionTree: InstructionTree): String =
      renderInstructionTree(instructionTree, describers).value

  /** Returns complete English sentences describing [requirement]. */
  internal fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).value

  /** Returns the best available text above [card]'s artwork. */
  internal fun topText(card: Class): String = renderTopText(card, describers).value

  /** Returns the best available text below [card]'s artwork. */
  internal fun bottomText(card: Class): String = renderBottomText(card, describers).value

  internal fun renderCard(card: Class): EnglishCardRendering {
    val top = renderTopText(card, describers)
    val bottom = renderBottomText(card, describers)
    return EnglishCardRendering(top.value, bottom.value, top.unresolved + bottom.unresolved)
  }

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun renderBottomText(
      card: Class,
      cardDescribers: Describers,
  ): Rendering<String> {
    val requirement = cardRequirement(card)?.let { renderRequirement(it, cardDescribers) }
    val immediateEffects =
        cardEffects(card).filter(::isImmediateSelfEffect).map {
          renderInstructionTree(it.instruction, cardDescribers)
        }
    val instructions = cardImmediate(card)?.let { renderInstructionTree(it, cardDescribers) }
    val scoring =
        cardEffects(card)
            .filter { isEndEffect(it, cardDescribers) }
            .map { renderEffect(it, cardDescribers) }
    return joinRenderings(
        listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring
    )
  }

  private fun renderTopText(
      card: Class,
      cardDescribers: Describers,
  ): Rendering<String> {
    val persistentEffects =
        cardEffects(card).filterNot {
          isEndEffect(it, cardDescribers) || isImmediateSelfEffect(it)
        }
    val integratedPayment =
        persistentEffects
            .mapIndexedNotNull { index, effect ->
              acceptedFirstActionPaymentResource(effect, cardDescribers)?.let { index to it }
            }
            .singleOrNull()
            ?.takeIf { cardActions(card).firstOrNull()?.cost != null }
    val actionsWithPayment =
        cardActions(card)
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
              renderEffects(
                      list,
                      cardDescribers,
                      cardResourceType = cardResourceType(card),
                  )
                  .map { text -> "Effect: $text" }
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
