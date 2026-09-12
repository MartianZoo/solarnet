package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
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
  internal fun describe(effect: Effect): String = renderEffect(effect, describers).value

  /** Returns complete English sentences describing [actions] as one action region. */
  internal fun describe(actions: List<Action>): String = renderActions(actions, describers).value

  /** Returns complete, context-neutral English sentences describing [instructionTree]. */
  internal fun describe(instructionTree: InstructionTree): String =
      renderInstructionTree(instructionTree, describers).value

  /** Returns complete English sentences describing [requirement]. */
  internal fun describe(requirement: Requirement): String =
      renderRequirement(requirement, describers).value

  /** Returns the best available English text describing [goal]. */
  internal fun renderGoal(goal: Class): EnglishGoalRendering = renderGoal(goal, describers)

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
    val interpretedEffects = interpretedCardEffects(card)
    val resourceValueEffects = renderCardResourceValueEffects(interpretedEffects, cardDescribers)
    val requirement = cardRequirement(card)?.let { renderRequirement(it, cardDescribers) }
    val immediateEffects =
        renderImmediateSelfEffects(
            interpretedEffects
                .filterNot { it in resourceValueEffects.first }
                .filter(::isImmediateSelfEffect),
            cardDescribers,
        )
    val instructions = cardImmediate(card)?.let { renderInstructionTree(it, cardDescribers) }
    val scoring =
        interpretedEffects
            .filter { isEndEffect(it, cardDescribers) }
            .filterNot { isUnconditionalFixedScore(it, cardDescribers) }
            .map { renderEffect(it, cardDescribers) }
    return joinRenderings(
        listOfNotNull(requirement) + immediateEffects + listOfNotNull(instructions) + scoring
    )
  }

  private fun renderTopText(
      card: Class,
      cardDescribers: Describers,
  ): Rendering<String> {
    val interpretedEffects = interpretedCardEffects(card)
    val resourceValueEffects = renderCardResourceValueEffects(interpretedEffects, cardDescribers)
    val persistentEffects =
        interpretedEffects
            .filterNot { it in resourceValueEffects.first }
            .filterNot { isEndEffect(it, cardDescribers) || isImmediateSelfEffect(it) }
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
    val effects =
        joinRenderings(
                listOfNotNull(
                    renderedPersistentEffects,
                    resourceValueEffects.second,
                )
            )
            .takeIf { it.value.isNotEmpty() }
            ?.map { text -> "Effect: $text" }
    return joinRenderings(listOfNotNull(actions, effects), " / ")
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
  ): Rendering<String> {
    val prepared = cardDescribers.prepareForRendering(effect)
    if (prepared.trigger == WhenGain) {
      return renderInstructionTree(prepared.instruction, cardDescribers)
    }
    val conditional = prepared.trigger as? IfTrigger
    val condition =
        conditional
            ?.takeIf { it.inner == WhenGain }
            ?.condition
            ?.let(cardDescribers::renderGateCondition)
    if (condition == null) {
      return Rendering.unresolved(
          effect,
          RefusalReason.UNSUPPORTED_EFFECT_TRIGGER,
          completeSentence("[$effect]"),
      )
    }
    val instruction = renderInstructions(prepared.instruction, cardDescribers)
    return Sentence(
            Clause.Prefaced(
                Clause.Preface.Conditional(condition),
                instruction.asCoordinatedClause(),
            )
        )
        .render()
  }

  private fun renderImmediateSelfEffects(
      effects: List<Effect>,
      cardDescribers: Describers,
  ): List<Rendering<String>> {
    val renderings = mutableListOf<Rendering<String>>()
    var index = 0
    while (index < effects.size) {
      val combined =
          effects.getOrNull(index + 1)?.let { next ->
            renderComplementaryProductionFloor(effects[index], next, cardDescribers)
          }
      if (combined != null) {
        renderings += combined
        index += 2
      } else {
        renderings += renderImmediateSelfEffect(effects[index], cardDescribers)
        index++
      }
    }
    return renderings
  }

  private fun renderComplementaryProductionFloor(
      first: Effect,
      second: Effect,
      cardDescribers: Describers,
  ): Rendering<String>? {
    val preparedFirst = cardDescribers.prepareForRendering(first)
    val preparedSecond = cardDescribers.prepareForRendering(second)
    if (preparedFirst.automatic != preparedSecond.automatic) return null
    val firstCondition = immediatePresenceCondition(preparedFirst) ?: return null
    val secondCondition = immediatePresenceCondition(preparedSecond) ?: return null
    if (
        firstCondition.expression != secondCondition.expression ||
            firstCondition.present == secondCondition.present
    ) {
      return null
    }
    val firstInstructions = InstructionGroup.of(preparedFirst.instruction).instructions
    val secondInstructions = InstructionGroup.of(preparedSecond.instruction).instructions
    val commonCount =
        firstInstructions.zip(secondInstructions).takeWhile { (one, two) -> one == two }.size
    if (commonCount == 0) return null
    val firstTargets =
        productionFloorTargets(firstInstructions.drop(commonCount), cardDescribers) ?: return null
    val secondTargets =
        productionFloorTargets(secondInstructions.drop(commonCount), cardDescribers) ?: return null
    if (firstTargets.keys != secondTargets.keys) return null
    val lowerTargets =
        when {
          firstTargets.all { (production, target) -> secondTargets[production] == target + 1 } ->
              firstTargets
          secondTargets.all { (production, target) -> firstTargets[production] == target + 1 } ->
              secondTargets
          else -> return null
        }
    val floor = lowerTargets.values.minOrNull()?.takeIf { it > 0 } ?: return null
    val common =
        renderInstructionTree(
            InstructionGroup(firstInstructions.take(commonCount)),
            cardDescribers,
        )
    if (common.unresolved.isNotEmpty()) return null
    val floorChange =
        Sentence(
                Clause.Simple(
                    Predicate(
                        Verb("increase"),
                        Coordination.one(
                            NounPhrase.text("each of your productions below $floor to $floor")
                        ),
                    )
                )
            )
            .render()
    return joinRenderings(listOf(common, floorChange))
  }

  private fun immediatePresenceCondition(effect: Effect): PresenceCondition? {
    val trigger = effect.trigger as? IfTrigger ?: return null
    if (trigger.inner != WhenGain) return null
    return when (val condition = trigger.condition) {
      is Requirement.Min ->
          (condition.countedMetric as? Metric.Count)
              ?.expression
              ?.takeIf { condition.minimum == 1 && it.simple }
              ?.let { PresenceCondition(it, present = true) }
      is Requirement.Max ->
          (condition.countedMetric as? Metric.Count)
              ?.expression
              ?.takeIf { condition.maximum == 0 && it.simple }
              ?.let { PresenceCondition(it, present = false) }
      else -> null
    }
  }

  private fun productionFloorTargets(
      instructions: List<Instruction>,
      cardDescribers: Describers,
  ): Map<Expression, Int>? {
    if (instructions.size < 2) return null
    val targets = linkedMapOf<Expression, Int>()
    for (instruction in instructions) {
      val per = instruction as? Instruction.Per ?: return null
      val gain = per.inner as? Gain ?: return null
      if (gain.quantifier.modality() != Modality.REQUIRED || gain.count.fixedQuantity() != 1) {
        return null
      }
      val production = productionExpression(gain.gaining, cardDescribers) ?: return null
      if (production.owner != null) return null
      val remaining = per.metric as? Metric.Subtract ?: return null
      val target = (remaining.minuend as? Metric.Constant)?.value ?: return null
      val current = (remaining.subtrahend as? Metric.Count)?.expression ?: return null
      if (current != gain.gaining || targets.put(gain.gaining, target) != null) return null
    }
    return targets
  }

  private data class PresenceCondition(val expression: Expression, val present: Boolean)
}
