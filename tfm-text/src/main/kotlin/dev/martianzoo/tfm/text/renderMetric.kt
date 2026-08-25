package dev.martianzoo.tfm.text

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Dependency.Key

internal fun renderMetricPhrase(metric: Metric, describers: Describers): String? {
  return when (metric) {
    is Metric.Count -> renderCountPhrase(metric, describers)
    is Metric.Scaled -> renderScaledCountPhrase(metric, describers)
    is Metric.Max -> {
      val maximum = (metric.maximum as? Metric.Constant)?.value ?: return null
      renderMetricPhrase(metric.inner, describers)?.let { "$it (max $maximum)" }
    }
    is Metric.Constant,
    is Metric.Eval,
    is Metric.Or,
    is Metric.Subtract,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderCountPhrase(metric: Metric.Count, describers: Describers): String? =
    describers.renderMetric(metric.expression)

private fun renderScaledCountPhrase(metric: Metric.Scaled, describers: Describers): String? {
  val count = metric.inner as? Metric.Count ?: return null
  return describers.renderMetric(count.expression, metric.unit)
}

internal fun Describers.renderMetric(expression: Expression, unit: Int? = null): String? {
  val count = unit ?: 1
  val prefix = unit?.let { "every $it" } ?: "each"
  renderCountedRelation(expression, this)?.let { relation ->
    return "$prefix ${relation.countedObject(count)}"
  }
  distinctOwnedKinds(expression, this)?.let { noun ->
    return "$prefix ${if (count == 1) noun.singular else noun.plural} you have"
  }
  renderZeroMaximumFilter(expression, prefix, count)?.let {
    return it
  }
  renderComponentCount(expression, prefix, count)?.let {
    return it
  }
  renderTagMetric(expression, prefix, unit, this)?.let {
    return it
  }
  renderUnrestrictedOwnedComponent(expression, prefix, count)?.let {
    return it
  }
  if (expression.simple) {
    cardResourceNoun(expression.className, count)?.let { noun ->
      return "$prefix $noun you have"
    }
    placementCountPhrase(expression, count)?.let { phrase ->
      return "$prefix $phrase"
    }
    return null
  }
  placementCountPhrase(expression, count)?.let { phrase ->
    return "$prefix $phrase"
  }
  val resolved = resolveCardResource(expression) ?: return null
  if (
      !cardResourceHasHolder(resolved, thisExpression) ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  val noun = cardResourceNoun(expression.className, count) ?: return null
  return "$prefix $noun on this card"
}

private fun Describers.renderUnrestrictedOwnedComponent(
    expression: Expression,
    prefix: String,
    count: Int,
): String? {
  if (expression.refinement != null || expression.complement) return null
  if (changeFrame(expression.className) != null) return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (!resolved.hasOnlySourceDependency(ownerKey, anyoneExpression)) return null
  return "$prefix ANY ${componentNounPhrase(expression.className, count).noun()}"
}

internal fun distinctOwnedKinds(
    expression: Expression,
    describers: Describers,
): ComponentDescriber.Noun.Counted? {
  if (expression.className != CLASS || expression.complement) return null
  val classKey = Key(CLASS, 0)
  val resolvedClass = describers.resolveExpression(expression) ?: return null
  val kind = resolvedClass.sourceDependency(classKey) ?: return null
  if (!resolvedClass.hasOnlySourceDependency(classKey, kind) || !kind.simple) return null
  val refinement = expression.refinement?.takeIf { !it.forgiving } ?: return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val member = (minimum.metric as? Metric.Count)?.expression ?: return null
  val resolvedMember = describers.resolveExpression(member) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (
      member.className != kind.className ||
          !resolvedMember.hasOnlySourceDependency(ownerKey, describers.ownerExpression) ||
          member.refinement != null ||
          member.complement
  ) {
    return null
  }
  return describers.fact(kind.className, ComponentDescriber::distinctKinds)
}

private fun Describers.renderComponentCount(
    expression: Expression,
    prefix: String,
    count: Int,
): String? {
  if (expression.refinement != null || expression.complement) return null
  val description = fact(expression.className, ComponentDescriber::metricCount) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val suffix =
      when {
        resolved.sourceDependencies.isEmpty() -> description.unqualifiedSuffix
        resolved.hasOnlySourceDependency(ownerKey, anyoneExpression) -> description.anyoneSuffix
        else -> return null
      } ?: return null
  val noun = if (count == 1) description.noun.singular else description.noun.plural
  return "$prefix $noun $suffix"
}

private fun Describers.renderZeroMaximumFilter(
    expression: Expression,
    prefix: String,
    count: Int,
): String? {
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.complement) return null
  val outer = fact(expression.className, ComponentDescriber::countNoun) ?: return null
  val refinement = expression.refinement ?: return null
  if (refinement.forgiving) return null
  val maximum = refinement.requirement as? Requirement.Max ?: return null
  if (maximum.target != 0) return null
  val excluded = (maximum.metric as? Metric.Count)?.expression ?: return null
  if (!excluded.simple) return null
  val inner = fact(excluded.className, ComponentDescriber::countNoun) ?: return null
  val outerNoun = if (count == 1) outer.singular else outer.plural
  return "$prefix $outerNoun with no ${inner.plural}"
}

private fun renderTagMetric(
    expression: Expression,
    prefix: String,
    unit: Int?,
    describers: Describers,
): String? {
  if (expression.refinement != null || expression.complement) return null
  val (name) = describers.tagName(expression.className) ?: return null
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val ownership =
      when {
        resolved.sourceDependencies.isEmpty() -> "you have"
        resolved.hasOnlySourceDependency(ownerKey, describers.anyoneExpression) ->
            "among all players"
        resolved.hasOnlySourceDependency(ownerKey, describers.notOwnerExpression) ->
            "your opponents have"
        else -> return null
      }
  return "$prefix $name ${if (unit == null) "tag" else "tags"} $ownership"
}

private fun Describers.placementCountPhrase(expression: Expression, count: Int): String? {
  if (expression.refinement != null || expression.complement) return null
  val placement = positionedFrame(expression.className) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val siteKey = Key(TILE, 0)
  val ownerType = resolved.dependency(ownerKey) ?: return null
  val site = resolved.selectedDependency(siteKey)
  val explicitlyUnrestricted = resolved.sourceDependency(ownerKey) == anyoneExpression
  val ownedByYou =
      !explicitlyUnrestricted &&
          (ownerType.expression == ownerExpression ||
              isGameParticipant(ownerType.rootClass.className))
  val (owner, location) =
      when {
        ownedByYou && site == null -> (placement.unqualifiedMetricOwner ?: return null) to null
        explicitlyUnrestricted && site == null ->
            (placement.anyoneMetricOwner ?: return null) to null
        explicitlyUnrestricted && site != null -> {
          val location = site.expression
          if (!location.simple) return null
          (placement.anyoneMetricOwner ?: return null) to
              (fact(location.className, ComponentDescriber::metricLocation) ?: return null)
        }
        else -> null
      } ?: return null
  val ownerPhrase =
      when (owner) {
        ComponentDescriber.MetricOwner.YOU -> " you own"
        ComponentDescriber.MetricOwner.ANY_PLAYER -> ""
      }
  val noun = if (count == 1) placement.singular else placement.plural
  return "$noun$ownerPhrase${location?.let { " $it" }.orEmpty()}"
}

private val TILE = cn("Tile")
