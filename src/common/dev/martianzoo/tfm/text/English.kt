package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardRequirement
import dev.martianzoo.tfm.canon.cardResourceType

/** English Pets text using one structural vocabulary and its sparse component descriptions. */
internal class English(
    classTable: ClassTable,
    descriptions: Map<ClassName, ComponentDescriber>,
) {
  private val describers = Describers(classTable, descriptions)

  /** Returns complete English sentences describing [effect]. */
  internal fun describe(effect: Effect): String = renderEffect(effect, describers).linearize()

  /** Returns complete English sentences describing [actions] as one action region. */
  internal fun describe(actions: List<Action>): String =
      renderActions(actions, describers).linearize()

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  internal fun describe(instructionTree: InstructionTree): String =
      renderInstructionTree(instructionTree, describers).linearize()

  /** Returns complete English sentences describing [requirement]. */
  internal fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).linearize()

  /** Returns the best available English text describing [goal]. */
  internal fun renderGoal(goal: Class): EnglishGoalRendering {
    val rendered = renderGoal(goal, describers)
    return EnglishGoalRendering(rendered.linearize(), rendered.unresolved())
  }

  /** Returns the best available text above [card]'s artwork. */
  internal fun topText(card: Class): String = renderTopText(card, cardDescribers(card)).linearize()

  /** Returns the best available text below [card]'s artwork. */
  internal fun bottomText(card: Class): String =
      renderBottomText(card, cardDescribers(card)).linearize()

  internal fun renderCard(card: Class): EnglishCardRendering {
    val cardDescribers = cardDescribers(card)
    val top = renderTopText(card, cardDescribers)
    val bottom = renderBottomText(card, cardDescribers)
    return EnglishCardRendering(
        top.linearize(),
        bottom.linearize(),
        top.unresolved() + bottom.unresolved(),
    )
  }

  private fun cardDescribers(card: Class): Describers =
      describers.forCard(card, cardResourceType(card))

  // Of the card's Effects, only endgame scoring is printed below the artwork.
  private fun renderBottomText(
      card: Class,
      cardDescribers: Describers,
  ): EnglishText {
    val enteringCardDescribers = cardDescribers.whileEnteringCard()
    val interpretedEffects = interpretedCardEffects(card)
    val resourceValueEffects = renderCardResourceValueEffects(interpretedEffects, cardDescribers)
    val requirement = cardRequirement(card)?.let { renderRequirement(it, cardDescribers) }
    val immediateEffects =
        renderImmediateSelfEffects(
            interpretedEffects
                .filterNot { it in resourceValueEffects.first }
                .filter(::isImmediateSelfEffect),
            enteringCardDescribers,
        )
    val instructions =
        cardImmediate(card)?.let {
          cardPrintedProcedureTextByName[card.className]?.immediate?.let(EnglishText::Authored)
              ?: renderInstructionTree(it, enteringCardDescribers)
        }
    val scoring =
        interpretedEffects
            .filter { isEndEffect(it, cardDescribers) }
            .filterNot { isUnconditionalFixedScore(it, cardDescribers) }
            .map { renderEffect(it, cardDescribers) }
    return EnglishText.join(
        listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring
    )
  }

  private fun renderTopText(
      card: Class,
      cardDescribers: Describers,
  ): EnglishText {
    val interpretedEffects = interpretedCardEffects(card)
    val cardActions = cardActions(card)
    val resourceValueEffects = renderCardResourceValueEffects(interpretedEffects, cardDescribers)
    val persistentEffects =
        interpretedEffects
            .filterNot { it in resourceValueEffects.first }
            .filterNot { isEndEffect(it, cardDescribers) || isImmediateSelfEffect(it) }
    val integratedPayment =
        persistentEffects
            .mapIndexedNotNull { index, effect ->
              acceptedFirstActionPaymentResource(
                      effect,
                      singleAction = cardActions.size == 1,
                      cardDescribers,
                  )
                  ?.let { index to it }
            }
            .singleOrNull()
            ?.takeIf { cardActions.firstOrNull()?.cost != null }
    val actionsWithPayment =
        cardActions
            .takeIf { it.isNotEmpty() }
            ?.let {
              cardPrintedProcedureTextByName[card.className]?.actions?.let(EnglishText::Authored)
                  ?: renderActions(it, cardDescribers, integratedPayment?.second)
            }
    val paymentWasIntegrated = integratedPayment != null
    val actions = actionsWithPayment?.let { EnglishText.Labeled("Action: ", it) }
    val renderedPersistentEffects =
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
            }
    val effectTexts = listOfNotNull(renderedPersistentEffects, resourceValueEffects.second)
    val effects =
        effectTexts
            .takeIf(List<EnglishText>::isNotEmpty)
            ?.let { EnglishText.join(it) }
            ?.let { EnglishText.Labeled("Effect: ", it) }
    return EnglishText.join(listOfNotNull(actions, effects), " / ")
  }

  private fun interpretedCardEffects(card: Class): List<Effect> =
      cardEffects(card).map(card::interpretTypeVariablesIn)

  private fun isImmediateSelfEffect(effect: Effect): Boolean {
    return (effect.automatic && effect.trigger == WhenGain) ||
        (effect.trigger as? IfTrigger)?.inner == WhenGain
  }

  private fun renderImmediateSelfEffect(
      effect: Effect,
      cardDescribers: Describers,
  ): EnglishText {
    val prepared = cardDescribers.prepareForRendering(effect)
    if (prepared.trigger == WhenGain) {
      return renderInstructionTree(prepared.instruction, cardDescribers)
    }
    val conditional = prepared.trigger as? IfTrigger
    if (
        conditional?.inner == WhenGain &&
            cardDescribers.isAvailableProcedureClass(conditional.condition)
    ) {
      return renderInstructionTree(prepared.instruction, cardDescribers)
    }
    val condition =
        conditional
            ?.takeIf { it.inner == WhenGain }
            ?.condition
            ?.let(cardDescribers::renderGateCondition)
    if (condition == null) {
      return Sentence(Clause.RawPets(Unresolved(effect, RefusalReason.UNSUPPORTED_EFFECT_TRIGGER)))
          .asText()
    }
    val instruction = renderInstructions(prepared.instruction, cardDescribers)
    return Sentence(
            Clause.Prefaced(
                Clause.Preface.Conditional(condition),
                instruction.asCoordinatedClause(),
            )
        )
        .asText()
  }

  private fun renderImmediateSelfEffects(
      effects: List<Effect>,
      cardDescribers: Describers,
  ): List<EnglishText> = effects.map {
    renderImmediateSelfEffect(it, cardDescribers)
  }
}
