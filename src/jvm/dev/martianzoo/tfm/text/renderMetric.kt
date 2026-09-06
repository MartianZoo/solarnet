package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

internal fun renderMetricPhrase(metric: Metric, describers: Describers): NounPhrase? {
  return when (metric) {
    is Metric.Count -> renderCountPhrase(metric, describers)
    is Metric.Scaled -> renderScaledCountPhrase(metric, describers)
    is Metric.Max -> {
      val maximum = (metric.maximum as? Metric.Constant)?.value ?: return null
      renderMetricPhrase(metric.inner, describers)
          ?.withModifier(Modifier.Parenthetical("max $maximum"))
    }
    is Metric.Constant,
    is Metric.Eval,
    is Metric.Or,
    is Metric.Subtract,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderCountPhrase(metric: Metric.Count, describers: Describers): NounPhrase? =
    describers.renderMetric(metric.expression)

private fun renderScaledCountPhrase(metric: Metric.Scaled, describers: Describers): NounPhrase? {
  val count = metric.inner as? Metric.Count ?: return null
  return describers.renderMetric(count.expression, metric.unit)
}

internal fun Describers.renderMetric(expression: Expression, unit: Int? = null): NounPhrase? {
  val agreementCount = unit ?: 1
  renderCountedRelation(expression, this)?.let { relation ->
    return relation.countedObject(unit)
  }
  distinctOwnedKinds(expression, this)?.let { noun ->
    return NounPhrase(noun.singular, noun.plural, count = unit)
        .withModifier(Modifier.Phrase("you have"))
  }
  renderZeroMaximumFilter(expression, unit)?.let {
    return it
  }
  renderComponentCount(expression, unit)?.let {
    return it
  }
  renderTagMetric(expression, unit, this)?.let {
    return it
  }
  renderUnrestrictedOwnedComponent(expression, unit)?.let {
    return it
  }
  if (expression.simple) {
    cardResourceNounPhrase(expression.className, agreementCount)?.let { noun ->
      return noun.copy(count = unit).withModifier(Modifier.Phrase("you have"))
    }
    placementCountPhrase(expression, unit)?.let { phrase ->
      return phrase
    }
    return null
  }
  placementCountPhrase(expression, unit)?.let { phrase ->
    return phrase
  }
  val resolved = resolveCardResource(expression) ?: return null
  if (
      !cardResourceHasHolder(resolved, thisExpression) ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  val noun = cardResourceNounPhrase(expression.className, agreementCount) ?: return null
  return noun.copy(count = unit).withModifier(Modifier.Phrase("on this card"))
}

private fun Describers.renderUnrestrictedOwnedComponent(
    expression: Expression,
    count: Int?,
): NounPhrase? {
  if (expression.refinement != null || expression.complement) return null
  if (changeFrame(expression.className) != null) return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (!resolved.hasOnlySourceDependency(ownerKey, anyoneExpression)) return null
  return componentNounPhrase(expression.className, count ?: 1)
      .copy(count = count, determiner = "any")
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
    count: Int?,
): NounPhrase? {
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
  return NounPhrase(description.noun.singular, description.noun.plural, count = count)
      .withModifier(Modifier.Phrase(suffix))
}

private fun Describers.renderZeroMaximumFilter(
    expression: Expression,
    count: Int?,
): NounPhrase? {
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
  return NounPhrase(outer.singular, outer.plural, count = count)
      .withModifier(
          Modifier.Relation(
              "with",
              NounPhrase(
                  inner.singular,
                  inner.plural,
                  determiner = "no",
                  grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
              ),
          )
      )
}

private fun renderTagMetric(
    expression: Expression,
    unit: Int?,
    describers: Describers,
): NounPhrase? {
  if (expression.refinement != null || expression.complement) return null
  val name = describers.tagName(expression.className) ?: return null
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
  val noun = if (unit == null) "$name tag" else "$name tags"
  return NounPhrase(noun, "$name tags", count = unit).withModifier(Modifier.Phrase(ownership))
}

private fun Describers.placementCountPhrase(
    expression: Expression,
    count: Int?,
): NounPhrase? {
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
        ownedByYou && site == null -> (placement.unqualifiedOwnership ?: return null) to null
        explicitlyUnrestricted && site == null -> (placement.anyoneOwnership ?: return null) to null
        explicitlyUnrestricted && site != null -> {
          val location = site.expression
          if (!location.simple) return null
          (placement.anyoneOwnership ?: return null) to
              (fact(location.className, ComponentDescriber::metricLocation) ?: return null)
        }
        else -> null
      } ?: return null
  val ownerPhrase =
      when (owner) {
        ComponentDescriber.OwnershipPhrase.IMPLICIT -> null
        ComponentDescriber.OwnershipPhrase.YOURS -> "you own"
        ComponentDescriber.OwnershipPhrase.ANYONES -> "anyone owns"
      }
  val referenceNoun =
      placement.referenceNoun
          ?: ComponentDescriber.Noun.Counted(placement.singular, placement.plural)
  val determiner =
      if (explicitlyUnrestricted && owner == ComponentDescriber.OwnershipPhrase.IMPLICIT) "any"
      else null
  return listOfNotNull(ownerPhrase, location).fold(
      NounPhrase(referenceNoun.singular, referenceNoun.plural, count, determiner)
  ) { noun, phrase ->
    noun.withModifier(Modifier.Phrase(phrase))
  }
}

private val TILE = cn("Tile")
