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

  data class NonNegativeIcon(val className: ClassName) : CardCriterion
}

internal fun Describers.cardFilterCriterion(filter: Expression): CardCriterion? {
  if (filter.refinement != null) return null
  return when (filter.className) {
    cn("TagFilter") -> {
      val tag = filter.arguments.singleOrNull()?.let(::representedClassArgument)?.className
      tag?.takeIf(::isTag)?.let(CardCriterion::Tag)
    }
    cn("NoTagsFilter") -> CardCriterion.NoTags.takeIf { filter.simple }
    cn("RequirementFilter") ->
        CardCriterion.PropertyPresence("requirement").takeIf { filter.simple }
    cn("ReferenceFilter") -> {
      val referenced =
          filter.arguments.singleOrNull()?.let(::representedClassArgument)?.className ?: return null
      referenceCriterion(referenced)
    }
    else -> null
  }
}

internal fun Describers.cardCriterion(requirement: Requirement): CardCriterion? {
  if (requirement is Requirement.Or) {
    val alternatives = requirement.requirements.map { cardCriterion(it) ?: return null }
    return alternatives.distinct().singleOrNull()
  }
  val counting = requirement as? Requirement.Counting ?: return null
  if (counting is Requirement.Min && counting.target == 1) {
    val metric = counting.metric
    if (metric is Metric.Or) {
      return metric.metrics
          .map { cardCriterion(Requirement.Min(1, it)) ?: return null }
          .distinct()
          .singleOrNull()
    }
    if (
        metric is Metric.Count &&
            metric.expression.arguments.isEmpty() &&
            fact(metric.expression.className, ComponentDescriber::tagAssignmentHolder) == true
    ) {
      val held =
          (metric.expression.refinement as? Expression.Refinement.Has)?.requirement ?: return null
      return cardCriterion(held) as? CardCriterion.Tag
    }
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
          val referenced = represented ?: return null
          return referenceCriterion(referenced)
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
      if (fact(citations.className, ComponentDescriber::nonNegativeIconCount) == true) {
        val icon = representedClassName(citations) ?: return null
        return CardCriterion.NonNegativeIcon(icon)
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

private fun Describers.referenceCriterion(referenced: ClassName): CardCriterion? =
    fact(referenced, ComponentDescriber::requirementKind)?.let { kind ->
      CardCriterion.PropertyPresence("$kind requirement")
    } ?: referenced.takeIf { cardResourceNoun(it, 1) != null }?.let(CardCriterion::ResourceIcon)

internal fun Describers.cardSelector(
    expression: Expression,
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
): NounPhrase? {
  if (triggerFrame(expression.className) !is ComponentDescriber.TriggerFrame.PlayCard) return null
  if (
      expression.refinement == null &&
          expression.arguments.isEmpty() &&
          references.variableUsedAt(expression) != null
  )
      return NounPhrase("card", determiner = Determiner.THAT)
  val resolved = resolveExpression(expression.copy(refinement = null)) ?: return null
  if (resolved.sourceDependencies.isNotEmpty()) return null
  val refinements =
      when (val refinement = expression.refinement) {
        is Expression.Refinement.And -> refinement.refinements
        null -> emptyList()
        else -> listOf(refinement)
      }
  val criterion =
      refinements.filterIsInstance<Expression.Refinement.Has>().singleOrNull()?.let {
        cardCriterion(it.requirement) ?: return null
      }
  if (refinements.count { it is Expression.Refinement.Has } > 1) return null
  var noun =
      NounPhrase(
          criterion?.let { matchingCardNoun(it, true, this) } ?: "card",
          determiner = Determiner.INDEFINITE,
      )
  for (excluded in refinements.filterIsInstance<Expression.Refinement.Not>()) {
    if (references.variableUsedAt(excluded.excluded) == null) return null
    noun = noun.withModifier(Modifier.Phrase("that is not that card"))
  }
  return noun
}

internal fun matchingCardNoun(
    criterion: CardCriterion,
    singular: Boolean,
    describers: Describers,
): String {
  val card = if (singular) NounPhrase("card") else NounPhrase.plural("cards")
  return if (criterion is CardCriterion.Tag) {
    val tag = checkNotNull(describers.tagName(criterion.className))
    card.withAttributiveModifier(NounPhrase.text(tag)).linearize()
  } else {
    card
        .withModifier(checkNotNull(cardCriterionModifier(criterion, singular, describers)))
        .linearize()
  }
}

internal fun cardCriterionModifier(
    criterion: CardCriterion,
    singular: Boolean,
    describers: Describers,
): Modifier? {
  val noun =
      when (criterion) {
        is CardCriterion.Tag -> {
          val tag = describers.tagNoun(criterion.className) ?: return null
          NounPhrase.plural(tag.plural)
        }
        CardCriterion.NoTags -> NounPhrase.plural("tags").withDeterminer(Determiner.NO)
        is CardCriterion.PropertyPresence ->
            if (singular) NounPhrase(criterion.noun, determiner = Determiner.INDEFINITE)
            else NounPhrase.plural("${criterion.noun}s")
        is CardCriterion.ResourceIcon -> {
          val resource = describers.cardResourceNoun(criterion.className, 1) ?: return null
          if (singular) NounPhrase("$resource icon") else NounPhrase.plural("$resource icons")
        }
        is CardCriterion.NonNegativeIcon -> {
          val icon =
              describers.fact(criterion.className, ComponentDescriber::score)?.singular
                  ?: describers.componentNoun(criterion.className, 1)
          if (singular) NounPhrase("non-negative $icon icon", determiner = Determiner.INDEFINITE)
          else NounPhrase.plural("non-negative $icon icons")
        }
      }
  return Modifier.Relation("with", noun)
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
