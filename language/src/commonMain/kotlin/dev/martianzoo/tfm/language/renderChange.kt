package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

/** Interprets one Pets state change from passive component construction facts. */
internal fun renderChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        else -> return null
      }
  describers.fact(expression.className, ComponentDescriber::directGain)?.let {
    return renderDirectGain(instruction, it)
  }
  if (describers.fact(expression.className, ComponentDescriber::discardable) == true)
      return renderDiscard(instruction, describers)
  if (describers.fact(expression.className, ComponentDescriber::cardResource) != null)
      return renderCardResourceChange(instruction, describers)
  if (describers.fact(expression.className, ComponentDescriber::production) == true)
      return renderProductionChange(instruction, describers)
  describers.fact(expression.className, ComponentDescriber::track)?.let {
    return renderTrackChange(instruction, it)
  }
  describers.fact(expression.className, ComponentDescriber::placement)?.let {
    return renderPlacement(instruction, it)
  }
  if (describers.fact(expression.className, ComponentDescriber::standardResource) == true)
      return renderStandardResourceChange(instruction, describers)
  return null
}

private fun renderDiscard(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  if (!removal.removing.simple) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return clause("discard", describers.componentNounPhrase(removal.removing.className, count))
}

internal fun isProductionChange(instruction: Instruction, describers: Describers): Boolean {
  val expression =
      (instruction as? Instruction.Change)?.let { it.gaining ?: it.removing } ?: return false
  return describers.fact(expression.className, ComponentDescriber::production) == true
}

internal fun isCoalescibleStandardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Boolean {
  val expression = (instruction as? Gain)?.gaining ?: return false
  return describers.fact(expression.className, ComponentDescriber::standardResource) == true
}

internal fun standardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Pair<ClassName, Int>? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  return (className to count).takeIf {
    describers.concrete(className) &&
        describers.fact(className, ComponentDescriber::standardResource) == true
  }
}

private fun renderDirectGain(
    instruction: Instruction,
    description: ComponentDescriber.DirectGain,
): Clause? {
  val (_, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != description.count) return null
  return clause("gain", NounPhrase(description.noun, count = count))
}

private fun renderStandardResourceChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  standardResourceGain(instruction, describers)?.let { (className, count) ->
    return clause("gain", describers.componentNounPhrase(className, count))
  }
  val removal = instruction as? Remove ?: return null
  val expression = removal.removing
  if (expression.refinement != null || expression.complement) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  if (!describers.concrete(expression.className)) return null
  if (describers.fact(expression.className, ComponentDescriber::standardResource) != true)
      return null
  if (expression.simple && (removal.intensity == null || removal.intensity == MANDATORY)) {
    return clause("remove", describers.componentNounPhrase(expression.className, count))
  }
  if (
      expression.arguments == listOf(describers.anyoneExpression) && removal.intensity == OPTIONAL
  ) {
    val noun = describers.componentNoun(expression.className, count)
    return clause(
        "remove",
        NounPhrase.text("up to $count $noun"),
        Modifier.Phrase("from any player"),
    )
  }
  return null
}

private fun renderCardResourceChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  if (change.intensity != null && change.intensity != MANDATORY) return null
  val expression = change.gaining ?: change.removing ?: return null
  if (expression.refinement != null || expression.complement) return null
  val count = (change.count as? ActualScalar)?.value ?: return null
  val noun = describers.cardResourceNounPhrase(expression.className, count) ?: return null
  if (instruction is Remove) {
    if (!expression.simple) return null
    return clause("remove", noun, Modifier.Phrase("from any card"))
  }
  val target =
      when {
        expression.arguments == listOf(describers.thisExpression) -> "this card"
        expression.arguments.size == 1 ->
            describers.renderCardResourceHolder(expression.arguments.single()) ?: return null
        expression.arguments.isNotEmpty() -> return null
        else -> "any card"
      }
  return clause("add", noun, Modifier.Phrase("to $target"))
}

private fun renderProductionChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  if (change.intensity != null && change.intensity != MANDATORY) return null
  val gaining = change is Gain
  if (!gaining && change !is Remove) return null
  val expression = change.gaining ?: change.removing ?: return null
  val (ownerArguments, resourceClassName) =
      describers.productionExpression(expression) ?: return null
  val owner =
      when {
        ownerArguments.isEmpty() -> "your"
        !gaining && ownerArguments == listOf(describers.anyoneExpression) -> "any player's"
        else -> return null
      }
  val count = (change.count as? ActualScalar)?.value ?: return null
  val steps = if (count == 1) "step" else "steps"
  val production =
      "$owner ${describers.componentNoun(resourceClassName, 1)} production $count $steps"
  return clause(if (gaining) "increase" else "decrease", NounPhrase.text(production))
}

private fun renderTrackChange(
    instruction: Instruction,
    description: ComponentDescriber.Track,
): Clause? {
  val gain = concreteMandatoryGain(instruction)
  val removal = concreteMandatoryRemoval(instruction)
  val (_, count) = gain ?: removal ?: return null
  val steps = if (count == 1) "step" else "steps"
  return clause(
      if (gain != null) "raise" else "lower",
      NounPhrase.text("${description.subject} $count $steps"),
  )
}

private fun renderPlacement(
    instruction: Instruction,
    description: ComponentDescriber.Placement,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  // Explicit <> accepts placement defaults and does not narrow the placed component.
  if (
      gain.gaining.arguments.isNotEmpty() ||
          gain.gaining.refinement != null ||
          gain.gaining.complement
  ) {
    return null
  }
  val count = (gain.count as? ActualScalar)?.value ?: return null
  if (count != 1 && !description.allowsMultiple) return null
  val noun =
      if (count == 1) {
        NounPhrase(description.singular, description.plural, determiner = description.article)
      } else {
        NounPhrase(description.singular, description.plural, count = count)
      }
  val consequence = description.consequence?.let(Modifier::Parenthetical)
  return clause("place", noun, *listOfNotNull(consequence).toTypedArray())
}

private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!gain.gaining.simple) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  return gain.gaining.className to count
}

private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  if (!removal.removing.simple) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return removal.removing.className to count
}

private fun Describers.renderCardResourceHolder(expression: Expression): String? {
  if (expression.arguments.isNotEmpty() || expression.complement) return null
  val holder = fact(expression.className, ComponentDescriber::cardResourceHolder) ?: return null
  val refinement = expression.refinement ?: return null
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val (tag) = tagName(metric.expression.className) ?: return null
  return "${indefiniteArticle(holder)} $holder with ${indefiniteArticle(tag)} $tag tag"
}

private fun clause(verb: String, noun: NounPhrase, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(Predicate(verb, Coordination.one(noun), modifiers.toList()))
