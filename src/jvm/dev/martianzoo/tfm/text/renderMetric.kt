package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

internal data class MetricRendering(
    val phrase: NounPhrase,
    val ranking: Ranking = Ranking.MOST,
) {
  internal enum class Ranking {
    MOST,
    HIGHEST,
  }
}

internal fun renderMetricPhrase(metric: Metric, describers: Describers): NounPhrase? =
    renderMetric(metric, describers, possessorEstablished = false)?.phrase

internal fun renderRequirementMetricPhrase(
    metric: Metric,
    count: Int,
    describers: Describers,
): NounPhrase? = renderMetric(metric, describers, possessorEstablished = true, count)?.phrase

internal fun renderRankedMetricPhrase(metric: Metric, describers: Describers): NounPhrase? {
  val rendering = renderMetric(metric, describers, possessorEstablished = true) ?: return null
  return when (rendering.ranking) {
    MetricRendering.Ranking.MOST -> rendering.phrase.asPlural().withDeterminer(Determiner.MOST)
    MetricRendering.Ranking.HIGHEST -> rendering.phrase.withDeterminer(Determiner.HIGHEST)
  }
}

private fun renderMetric(
    metric: Metric,
    describers: Describers,
    possessorEstablished: Boolean,
    count: Int? = null,
): MetricRendering? {
  return when (metric) {
    is Metric.Count -> describers.renderCountMetric(metric.expression, count, possessorEstablished)
    is Metric.Scaled -> renderScaledCountPhrase(metric, describers, possessorEstablished)
    is Metric.Max -> {
      val maximum = (metric.maximum as? Metric.Constant)?.value ?: return null
      renderMetric(metric.inner, describers, possessorEstablished)?.let {
        it.copy(phrase = it.phrase.withModifier(Modifier.Parenthetical("max $maximum")))
      }
    }
    is Metric.Or -> renderCombinedMetric(metric, describers, possessorEstablished, count)
    is Metric.Subtract ->
        if (describers.isProductionOffset(metric.subtrahend)) {
          renderMetric(metric.minuend, describers, possessorEstablished, count)
        } else {
          null
        }
    is Metric.Constant,
    is Metric.Eval,
    is Metric.Rank,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderCombinedMetric(
    metric: Metric.Or,
    describers: Describers,
    possessorEstablished: Boolean,
    count: Int?,
): MetricRendering? {
  if (!possessorEstablished) return null
  val members =
      metric.metrics.map {
        renderMetric(it, describers, possessorEstablished) ?: return null
      }
  val ranking =
      if (members.all { it.ranking == MetricRendering.Ranking.HIGHEST }) {
        MetricRendering.Ranking.HIGHEST
      } else {
        MetricRendering.Ranking.MOST
      }
  val phrase =
      NounPhrase.coordinated(Coordination(members.map { it.phrase.asPlural() }, Conjunction.AND))
          .let { if (count == null) it else it.quantified(count) }
          .withModifier(Modifier.Phrase("combined"))
  return MetricRendering(phrase, ranking)
}

private fun renderScaledCountPhrase(
    metric: Metric.Scaled,
    describers: Describers,
    possessorEstablished: Boolean,
): MetricRendering? {
  return renderMetric(metric.inner, describers, possessorEstablished, metric.unit)
}

private fun Describers.isProductionOffset(metric: Metric): Boolean {
  val expression = (metric as? Metric.Count)?.expression ?: return false
  return fact(expression.className, ComponentDescriber::productionOffset) == true
}

private fun Describers.renderCountMetric(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
): MetricRendering? {
  productionCategoryExpression(expression, this)?.let { production ->
    if (production.owner != null) return null
    val noun = "${componentNoun(production.resource, 1)} production"
    return MetricRendering(
        NounPhrase(noun, noun, count = count),
        MetricRendering.Ranking.HIGHEST,
    )
  }
  scaleFrame(expression.className)
      ?.takeIf { expression.simple }
      ?.let { scale ->
        val subject =
            if (!possessorEstablished) scale.subject else scale.subject.removePrefix("your ")
        return MetricRendering(
            NounPhrase.text(subject),
            MetricRendering.Ranking.HIGHEST,
        )
      }
  val agreementCount = count ?: 1
  renderCountedRelation(expression, this)?.let { relation ->
    return MetricRendering(relation.countedObject(count))
  }
  distinctOwnedKinds(expression, this)?.let { noun ->
    val phrase = NounPhrase(noun.singular, noun.plural, count = count)
    return MetricRendering(phrase.withOwnership("you have", possessorEstablished))
  }
  renderFilteredComponentCount(expression, count, possessorEstablished)?.let {
    return MetricRendering(it)
  }
  renderZeroMaximumFilter(expression, count)?.let {
    return MetricRendering(it)
  }
  renderComponentCount(expression, count, possessorEstablished)?.let {
    return MetricRendering(it)
  }
  renderFilteredPlacementCount(expression, count, possessorEstablished)?.let {
    return MetricRendering(it)
  }
  renderTagMetric(expression, count, possessorEstablished, this)?.let {
    return MetricRendering(it)
  }
  renderUnrestrictedOwnedComponent(expression, count)?.let {
    return MetricRendering(it)
  }
  if (expression.simple) {
    if (isStandardResource(expression.className)) {
      val noun = componentNounPhrase(expression.className, agreementCount).copy(count = count)
      return MetricRendering(noun.withOwnership("you have", possessorEstablished))
    }
    cardResourceNounPhrase(expression.className, agreementCount)?.let { noun ->
      return MetricRendering(
          noun.copy(count = count).withOwnership("you have", possessorEstablished)
      )
    }
    placementCountPhrase(expression, count, possessorEstablished)?.let { phrase ->
      return MetricRendering(phrase)
    }
    fact(expression.className, ComponentDescriber::countNoun)?.let { noun ->
      return MetricRendering(NounPhrase(noun.singular, noun.plural, count = count))
    }
    return null
  }
  placementCountPhrase(expression, count, possessorEstablished)?.let { phrase ->
    return MetricRendering(phrase)
  }
  renderDeckLocationMetric(expression, count)?.let { phrase ->
    return MetricRendering(phrase)
  }
  val resolved = resolveCardResource(expression) ?: return null
  if (!cardResourceHasHolder(resolved, thisExpression) || expression.refinement != null) {
    return null
  }
  val noun = cardResourceNounPhrase(expression.className, agreementCount) ?: return null
  return MetricRendering(noun.copy(count = count).withModifier(Modifier.Phrase("on this card")))
}

private fun Describers.renderFilteredPlacementCount(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
): NounPhrase? {
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val noun =
      placementCountPhrase(expression.copy(refinement = null), count, possessorEstablished)
          ?: return null
  val modifier = renderSpatialFilter(refinement.requirement) ?: return null
  return noun.withModifier(modifier)
}

private fun Describers.renderFilteredComponentCount(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
): NounPhrase? {
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val noun =
      renderComponentCount(expression.copy(refinement = null), count, possessorEstablished)
          ?: return null
  val modifier = renderMetricFilter(expression.className, refinement.requirement) ?: return null
  return noun.withModifier(modifier)
}

private fun Describers.renderMetricFilter(
    className: dev.martianzoo.pets.ast.ClassName,
    requirement: Requirement,
): Modifier? {
  cardCriterion(requirement)?.let { criterion ->
    return when (criterion) {
      is CardCriterion.Tag -> {
        val tag = tagName(criterion.className) ?: return null
        Modifier.Relation("with", NounPhrase.plural("$tag tags"))
      }
      CardCriterion.NoTags ->
          Modifier.Relation(
              "with",
              NounPhrase(
                  "tag",
                  "tags",
                  determiner = Determiner.NO,
                  grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
              ),
          )
      CardCriterion.HasRequirement -> Modifier.Relation("with", NounPhrase.plural("requirements"))
      is CardCriterion.ResourceIcon -> return null
    }
  }
  val frame = triggerFrame(className) as? ComponentDescriber.TriggerFrame.PlayCard ?: return null
  val counting = requirement as? Requirement.Counting ?: return null
  val property = counting.metric as? Property ?: return null
  if (property.receiver != null) return null
  val threshold =
      frame.minimumProperties[property.propertyName.value]
          as? ComponentDescriber.MinimumProperty.Threshold ?: return null
  val comparison =
      when (counting) {
        is Requirement.Min -> "or more"
        is Requirement.Max -> "or less"
        is Requirement.Exact -> "exactly"
      }
  val unit = threshold.unit?.let { " $it" }.orEmpty()
  val value =
      if (counting is Requirement.Exact) {
        "${threshold.noun} of exactly ${counting.target}$unit"
      } else {
        "${threshold.noun} of ${counting.target}$unit $comparison"
      }
  return Modifier.Relation("with", NounPhrase(value, determiner = Determiner.INDEFINITE))
}

private fun NounPhrase.withOwnership(
    ownership: String,
    possessorEstablished: Boolean,
): NounPhrase = if (possessorEstablished) this else withModifier(Modifier.Phrase(ownership))

private fun Describers.renderUnrestrictedOwnedComponent(
    expression: Expression,
    count: Int?,
): NounPhrase? {
  if (expression.refinement != null) return null
  if (changeFrame(expression.className) != null) return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (!resolved.hasOnlySourceDependency(ownerKey, anyoneExpression)) return null
  return componentNounPhrase(expression.className, count ?: 1)
      .copy(count = count, determiner = Determiner.ANY)
}

internal fun distinctOwnedKinds(
    expression: Expression,
    describers: Describers,
): ComponentDescriber.Noun.Counted? {
  if (expression.className != CLASS || expression.refinement is Expression.Refinement.Not) {
    return null
  }
  val classKey = Key(CLASS, 0)
  val resolvedClass = describers.resolveExpression(expression) ?: return null
  val kind = resolvedClass.sourceDependency(classKey) ?: return null
  if (!resolvedClass.hasOnlySourceDependency(classKey, kind) || !kind.simple) return null
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val member = (minimum.metric as? Metric.Count)?.expression ?: return null
  val resolvedMember = describers.resolveExpression(member) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (
      member.className != kind.className ||
          !resolvedMember.hasOnlySourceDependency(ownerKey, describers.ownerExpression) ||
          member.refinement != null
  ) {
    return null
  }
  return describers.fact(kind.className, ComponentDescriber::distinctKinds)
}

private fun Describers.renderComponentCount(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
): NounPhrase? {
  if (expression.refinement != null) return null
  if (
      !possessorEstablished &&
          triggerFrame(expression.className) is ComponentDescriber.TriggerFrame.PlayCard
  ) {
    return null
  }
  val description = metricCount(expression.className) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val suffix =
      when {
        resolved.sourceDependencies.isEmpty() -> description.unqualifiedSuffix
        resolved.hasOnlySourceDependency(ownerKey, anyoneExpression) -> description.anyoneSuffix
        else -> return null
      } ?: return null
  val noun = NounPhrase(description.noun.singular, description.noun.plural, count = count)
  return if (suffix.isEmpty()) noun else noun.withModifier(Modifier.Phrase(suffix))
}

private fun Describers.renderZeroMaximumFilter(
    expression: Expression,
    count: Int?,
): NounPhrase? {
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty()) return null
  val outer = fact(expression.className, ComponentDescriber::countNoun) ?: return null
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val maximum = refinement.requirement as? Requirement.Max ?: return null
  if (maximum.target != 0) return null
  val excluded = (maximum.metric as? Metric.Count)?.expression ?: return null
  if (!excluded.simple) return null
  val inner = fact(excluded.className, ComponentDescriber::countNoun) ?: return null
  return NounPhrase(outer.singular, outer.plural, count = count)
      .withModifier(
          Modifier.Relation(
              "with",
              NounPhrase(
                  inner.singular,
                  inner.plural,
                  determiner = Determiner.NO,
                  grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
              ),
          )
      )
}

private fun renderTagMetric(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
    describers: Describers,
): NounPhrase? {
  if (expression.refinement != null) return null
  val singular = describers.playedTagPhrase(expression.className)?.noun() ?: return null
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val ownership =
      when {
        resolved.sourceDependencies.isEmpty() -> "you have"
        resolved.hasOnlySourceDependency(ownerKey, describers.anyoneExpression) ->
            "among all players"
        resolved.sourceDependencies.size == 1 &&
            resolved.sourceDependency(ownerKey)?.let(describers::isNotOwner) == true ->
            "your opponents have"
        else -> return null
      }
  val plural = if (singular.endsWith("tag")) singular + "s" else singular
  return NounPhrase(singular, plural, count = count).withOwnership(ownership, possessorEstablished)
}

private fun Describers.placementCountPhrase(
    expression: Expression,
    count: Int?,
    possessorEstablished: Boolean,
): NounPhrase? {
  if (expression.refinement != null) return null
  val placement = positionedFrame(expression.className) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val ownerType = resolved.dependency(ownerKey) ?: return null
  val site =
      resolved.type.rootClass.dependencies.keys
          .mapNotNull { key ->
            resolved.selectedDependency(key)?.takeIf {
              placementSite(it.rootClass.className) != null
            }
          }
          .singleOrNull()
  val explicitlyUnrestricted = resolved.sourceDependency(ownerKey) == anyoneExpression
  val ownedByYou =
      !explicitlyUnrestricted &&
          (ownerType.expression == ownerExpression ||
              isGameParticipant(ownerType.rootClass.className))
  val (owner, location) =
      when {
        ownedByYou && site == null -> (placement.unqualifiedOwnership ?: return null) to null
        ownedByYou && site != null ->
            (placement.unqualifiedOwnership ?: return null) to
                renderPlacementLocation(site.expression)
        explicitlyUnrestricted && site == null -> (placement.anyoneOwnership ?: return null) to null
        explicitlyUnrestricted && site != null ->
            (placement.anyoneOwnership ?: return null) to renderPlacementLocation(site.expression)
        else -> null
      } ?: return null
  val ownerPhrase =
      if (!possessorEstablished) {
        when (owner) {
          ComponentDescriber.OwnershipPhrase.IMPLICIT -> null
          ComponentDescriber.OwnershipPhrase.YOURS -> "you own"
          ComponentDescriber.OwnershipPhrase.ANYONES -> "anyone owns"
        }
      } else {
        null
      }
  val referenceNoun =
      placement.referenceNoun
          ?: ComponentDescriber.Noun.Counted(placement.singular, placement.plural)
  val determiner =
      if (explicitlyUnrestricted && owner == ComponentDescriber.OwnershipPhrase.IMPLICIT) {
        Determiner.ANY
      } else {
        null
      }
  val noun = NounPhrase(referenceNoun.singular, referenceNoun.plural, count, determiner)
  return listOfNotNull(
          ownerPhrase?.let(Modifier::Phrase),
          location,
      )
      .fold(noun, NounPhrase::withModifier)
}

private fun Describers.renderPlacementLocation(expression: Expression): Modifier? {
  if (expression.refinement is Expression.Refinement.Not) return null
  if (expression.simple) {
    fact(expression.className, ComponentDescriber::metricLocation)?.let {
      return Modifier.Phrase(it)
    }
  }
  val site = placementSite(expression.className) ?: return null
  val noun =
      when (val described = site.noun) {
        is ComponentDescriber.Noun.Counted -> NounPhrase.plural(described.plural)
        is ComponentDescriber.Noun.Fixed -> NounPhrase.text(described.text)
        ComponentDescriber.Noun.ClassName ->
            NounPhrase.plural(componentNoun(expression.className, 2))
      }
  val refinement =
      expression.refinement as? Expression.Refinement.Has ?: return Modifier.Relation("on", noun)
  val modifier = renderSpatialFilter(refinement.requirement) ?: return null
  return Modifier.Relation("on", noun.withModifier(modifier))
}

private fun Describers.renderSpatialFilter(requirement: Requirement): Modifier? {
  val counting = requirement as? Requirement.Counting ?: return null
  val expression = (counting.metric as? Metric.Count)?.expression ?: return null
  val relation = fact(expression.className, ComponentDescriber::spatialRelation) ?: return null
  val targetExpression = expression.arguments.singleOrNull()
  if (targetExpression == null && counting.target != 0) return null
  val targetNoun =
      if (targetExpression != null) {
        spatialTarget(targetExpression, counting.target) ?: return null
      } else {
        relation.defaultTarget?.let { noun ->
          NounPhrase(
              noun.singular,
              noun.plural,
              grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
          )
        } ?: return null
      }
  return when (counting) {
    is Requirement.Min ->
        Modifier.Relation("${relation.phrase} at least", targetNoun.quantified(counting.target))
    is Requirement.Max ->
        if (counting.target == 0) {
          Modifier.Relation(
              relation.phrase,
              targetNoun.asPlural().withDeterminer(Determiner.NO),
          )
        } else {
          Modifier.Relation(relation.phrase, targetNoun.quantified(counting.target).atMost())
        }
    is Requirement.Exact ->
        Modifier.Relation(relation.phrase, targetNoun.quantified(counting.target))
  }
}

private fun Describers.spatialTarget(expression: Expression, count: Int): NounPhrase? {
  if (expression.refinement != null) return null
  positionedFrame(expression.className)?.let { placement ->
    val resolved = resolveExpression(expression) ?: return null
    val ownerKey = Key(OWNED, 0)
    if (resolved.sourceDependencies.keys.any { it != ownerKey }) return null
    val noun =
        placement.referenceNoun
            ?: ComponentDescriber.Noun.Counted(placement.singular, placement.plural)
    return NounPhrase(noun.singular, noun.plural, count = count)
  }
  val site = placementSite(expression.className) ?: return null
  return when (val noun = site.noun) {
    is ComponentDescriber.Noun.Counted -> NounPhrase(noun.singular, noun.plural, count = count)
    is ComponentDescriber.Noun.Fixed -> NounPhrase(noun.text, count = count)
    ComponentDescriber.Noun.ClassName -> componentNounPhrase(expression.className, count)
  }
}

private fun Describers.renderDeckLocationMetric(
    expression: Expression,
    count: Int?,
): NounPhrase? {
  if (expression.refinement != null) return null
  if (changeFrame(expression.className) != ComponentDescriber.ChangeFrame.Deck) return null
  val resolved = resolveExpression(expression) ?: return null
  val location =
      resolved.sourceDependencies.values.mapNotNull { dependency ->
        if (!dependency.simple) return@mapNotNull null
        fact(dependency.className, ComponentDescriber::metricLocation)
      }
  if (location.size != 1) return null
  return componentNounPhrase(expression.className, count ?: 1)
      .copy(count = count)
      .withModifier(Modifier.Phrase(location.single()))
}
