package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement

/** A printed card fact used to filter search, reveal, and test operations. */
internal sealed interface CardCriterion {
  data class Tag(val className: ClassName) : CardCriterion

  data object NoTags : CardCriterion

  data class PropertyPresence(val noun: String) : CardCriterion

  data class ResourceIcon(val className: ClassName) : CardCriterion

  data class PrintedIcon(val className: ClassName) : CardCriterion
}

internal fun Describers.cardCriterion(requirement: Requirement): CardCriterion? {
  val counting = requirement as? Requirement.Counting ?: return null
  if (counting is Requirement.Min && counting.target == 1) {
    val metric = counting.metric
    if (metric is Metric.Count && metric.expression.simple) {
      tagName(metric.expression.className)?.let {
        return CardCriterion.Tag(metric.expression.className)
      }
    }
    if (metric is Metric.Count) {
      val represented = representedClassName(metric.expression)
      when (metric.expression.className) {
        PRINTED_TAG -> {
          val tag = represented?.takeIf { tagName(it) != null } ?: return null
          return CardCriterion.Tag(tag)
        }
        REFERENCE_TO -> {
          val resource = represented?.takeIf { cardResourceNoun(it, 1) != null } ?: return null
          return CardCriterion.ResourceIcon(resource)
        }
      }
    }
    if (metric is Property && metric.receiver == null) {
      val frame = triggerFrame(cn("CardFront")) as? ComponentDescriber.TriggerFrame.PlayCard
      val property = frame?.minimumProperties?.get(metric.propertyName.value)
      val presence = property as? ComponentDescriber.MinimumProperty.Presence
      if (presence != null) return CardCriterion.PropertyPresence(presence.noun)
    }
    if (metric is Metric.Count) {
      val citations = metric.expression
      if (citations.className == CITATIONS) {
        val resource = representedClassName(citations) ?: return null
        if (cardResourceNoun(resource, 1) == null) return null
        return CardCriterion.ResourceIcon(resource)
      }
      if (fact(citations.className, ComponentDescriber::printedIconCount) == true) {
        val icon = representedClassName(citations) ?: return null
        return CardCriterion.PrintedIcon(icon)
      }
    }
  }
  if (
      counting is Requirement.Max &&
          counting.target == 0 &&
          (counting.metric as? Metric.Count)?.expression?.let {
            it.simple && it.className in setOf(TAG, PRINTED_TAG)
          } == true
  ) {
    return CardCriterion.NoTags
  }
  return null
}

internal fun Describers.cardSelector(expression: Expression): NounPhrase? {
  if (
      expression.refinement is Expression.Refinement.Not ||
          triggerFrame(expression.className) !is ComponentDescriber.TriggerFrame.PlayCard
  ) {
    return null
  }
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty()) return null
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val criterion = cardCriterion(refinement.requirement) ?: return null
  return NounPhrase(
      matchingCardNoun(criterion, singular = true, this),
      determiner = Determiner.INDEFINITE,
  )
}

internal fun matchingCardNoun(
    criterion: CardCriterion,
    singular: Boolean,
    describers: Describers,
): String =
    when (criterion) {
      is CardCriterion.Tag -> {
        val tag = checkNotNull(describers.tagName(criterion.className))
        "$tag ${if (singular) "card" else "cards"}"
      }
      CardCriterion.NoTags -> "${if (singular) "card" else "cards"} with no tags"
      is CardCriterion.PropertyPresence ->
          if (singular) "card with a ${criterion.noun}" else "cards with ${criterion.noun}s"
      is CardCriterion.ResourceIcon -> {
        val resource = checkNotNull(describers.cardResourceNoun(criterion.className, 1))
        "${if (singular) "card" else "cards"} with $resource ${if (singular) "icon" else "icons"}"
      }
      is CardCriterion.PrintedIcon -> {
        val noun =
            describers.fact(criterion.className, ComponentDescriber::score)?.singular
                ?: describers.componentNoun(criterion.className, 1)
        if (singular) {
          "card with ${NounPhrase(noun, determiner = Determiner.INDEFINITE).linearize()} icon"
        } else {
          "cards with $noun icons"
        }
      }
    }

private fun representedClassName(expression: Expression): ClassName? {
  if (expression.refinement != null) return null
  val wrapper =
      expression.arguments.singleOrNull { it.className == CLASS && it.refinement == null }
          ?: return null
  return wrapper.arguments.singleOrNull()?.takeIf { it.simple }?.className
}

private val CITATIONS = cn("Citations")
private val CLASS = cn("Class")
private val PRINTED_TAG = cn("PrintedTag")
private val REFERENCE_TO = cn("ReferenceTo")
private val TAG = cn("Tag")
