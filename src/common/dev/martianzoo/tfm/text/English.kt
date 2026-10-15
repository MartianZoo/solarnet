package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardDirectEffect
import dev.martianzoo.tfm.canon.cardEffects
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

  /** Describes [instructionTree], optionally addressing the named [player] as "you". */
  internal fun describe(instructionTree: InstructionTree, player: ClassName? = null): String =
      renderInstructionTree(
              player?.let { playerRelativeInstruction(instructionTree, it, describers) }
                  ?: instructionTree,
              describers,
          )
          .linearize()

  /** Returns complete English sentences describing [requirement]. */
  internal fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).linearize()

  /** Returns the best available English text describing [goal]. */
  internal fun renderGoal(goal: Class): EnglishRendering {
    val rendered = renderGoal(goal, describers)
    return EnglishRendering(rendered.linearize(), rendered.unresolved())
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
    val resourceValueEffects =
        renderCardResourceValueEffects(
            card.declaration.invariants,
            interpretedEffects,
            cardDescribers,
        )
    val requirement = cardRequirement(card)?.let { renderRequirement(it, cardDescribers) }
    val selfGainEffects =
        renderSelfGainEffects(
            interpretedEffects
                .filterNot { it in resourceValueEffects.first }
                .filter(::isSelfGainEffect),
            enteringCardDescribers,
        )
    val instructions =
        cardDirectEffect(card)?.let { renderInstructionTree(it, enteringCardDescribers) }
    val invariantSentences =
        Requirement.split(card.declaration.invariants)
            .filterIsInstance<Requirement.Exact>()
            .mapNotNull { literalCardInvariant(it, cardDescribers) }
    val scoring =
        interpretedEffects
            .filter { isEndEffect(it, cardDescribers) }
            .filterNot { isUnconditionalFixedScore(it, cardDescribers) }
            .map { renderEffect(it, cardDescribers) }
    return EnglishText.join(
        listOfNotNull(requirement) +
            selfGainEffects +
            listOfNotNull(instructions) +
            invariantSentences +
            scoring
    )
  }

  private fun renderTopText(
      card: Class,
      cardDescribers: Describers,
  ): EnglishText {
    val interpretedEffects = interpretedCardEffects(card)
    val cardActions = cardActions(card).map { it.untransformed }
    val resourceValueEffects =
        renderCardResourceValueEffects(
            card.declaration.invariants,
            interpretedEffects,
            cardDescribers,
        )
    val persistentEffects =
        interpretedEffects
            .filterNot { it in resourceValueEffects.first }
            .filterNot { isEndEffect(it, cardDescribers) || isSelfGainEffect(it) }
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
            ?.let { renderActions(it, cardDescribers, integratedPayment?.second) }
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
    val invariantTexts =
        Requirement.split(card.declaration.invariants)
            .filterIsInstance<Requirement.Exact>()
            .filterNot { invariant ->
              invariant in resourceValueEffects.first ||
                  countedExpression(invariant)?.let { cardDescribers.isTag(it.className) } == true
            }
            .filterNot { literalCardInvariant(it, cardDescribers) != null }
            .map(::renderCardInvariant)
    val effectTexts =
        listOfNotNull(renderedPersistentEffects, resourceValueEffects.second) + invariantTexts
    val effects =
        effectTexts
            .takeIf(List<EnglishText>::isNotEmpty)
            ?.let { EnglishText.join(it) }
            ?.let { EnglishText.Labeled("Effect: ", it) }
    return EnglishText.join(listOfNotNull(actions, effects), " / ")
  }

  private fun literalCardInvariant(
      invariant: Requirement.Exact,
      cardDescribers: Describers,
  ): EnglishText.LiteralSentence? {
    if (invariant.target != 1) return null
    val component = countedExpression(invariant) ?: return null
    return cardDescribers.cardInvariantSentence(component.className)?.let {
      EnglishText.LiteralSentence(it)
    }
  }

  private fun renderCardInvariant(invariant: Requirement.Exact): EnglishText {
    val component = countedExpression(invariant)
    if (component == null || invariant.target <= 0) {
      return Sentence(
              Clause.RawPets(Unresolved(invariant, RefusalReason.UNKNOWN_REQUIREMENT_FRAME))
          )
          .asText()
    }
    val noun =
        NounPhrase.countedRawPets(
            Unresolved(component, RefusalReason.UNKNOWN_REQUIREMENT_FRAME),
            invariant.target,
        )
    return Sentence(Clause.Simple(Predicate(Verb("gain"), Coordination.one(noun)))).asText()
  }

  private fun interpretedCardEffects(card: Class): List<Effect> =
      cardEffects(card)
          .map { card.interpretTypeVariablesIn(it).untransformed }
          .filterNot { effect ->
            val instructions = InstructionGroup.of(effect.instruction).instructions
            instructions.isNotEmpty() && instructions.all { isImplicitInstruction(it, describers) }
          }

  private fun isSelfGainEffect(effect: Effect): Boolean {
    return (effect.automatic && effect.trigger == WhenGain) ||
        (effect.trigger as? IfTrigger)?.inner == WhenGain
  }

  private fun renderSelfGainEffect(
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

  private fun renderSelfGainEffects(
      effects: List<Effect>,
      cardDescribers: Describers,
  ): List<EnglishText> = effects.map {
    renderSelfGainEffect(it, cardDescribers)
  }
}
