package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

/** Renders a placed component and any structurally described restriction on its site. */
internal fun renderPlacement(
    instruction: Instruction,
    description: ComponentDescriber.Placement,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!describers.concrete(gain.gaining.className)) return null
  if (gain.gaining.refinement != null || gain.gaining.complement) return null

  val siteModifiers = renderPlacementSites(gain.gaining.arguments, describers) ?: return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  if (siteModifiers.isNotEmpty() && count != 1) return null
  if (count != 1 && !description.allowsMultiple) return null
  val noun =
      if (count == 1) {
        NounPhrase(description.singular, description.plural, determiner = description.article)
      } else {
        NounPhrase(description.singular, description.plural, count = count)
      }
  val consequence = description.consequence?.let(Modifier::Parenthetical)
  return placementClause(noun, siteModifiers + listOfNotNull(consequence))
}

internal fun renderPlacementSites(
    arguments: List<Expression>,
    describers: Describers,
): List<Modifier>? {
  // No dependencies, including an explicitly authored <>, accept the placement defaults.
  if (arguments.isEmpty()) return emptyList()

  val describedSites = arguments.mapNotNull { expression ->
    describers.placementSite(expression.className)?.let {
      expression to it
    }
  }
  // A restricted placement has exactly one described site and no unknown dependency arguments.
  if (describedSites.size != 1 || describedSites.size != arguments.size) return null
  val (expression, site) = describedSites.single()
  if (expression.arguments.isNotEmpty() || expression.complement) return null

  val siteNoun = describers.describedNoun(expression.className, site.noun, 1)
  val article = site.article ?: describers.indefiniteArticle(siteNoun)
  val modifiers = mutableListOf(Modifier.Phrase("on $article $siteNoun"))
  expression.refinement?.let { refinement ->
    if (refinement.forgiving) return null
    modifiers +=
        Modifier.Phrase(
            renderPlacementSiteRequirement(refinement.requirement, describers) ?: return null
        )
  }
  return modifiers
}

private fun renderPlacementSiteRequirement(
    requirement: Requirement,
    describers: Describers,
): String? =
    renderSpatialRequirement(requirement, describers)
        ?: renderPlacementBonusRequirement(requirement, describers)

private fun renderPlacementBonusRequirement(
    requirement: Requirement,
    describers: Describers,
): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val expression = (minimum.metric as? Metric.Count)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  val bonus =
      describers.fact(expression.className, ComponentDescriber::placementBonus) ?: return null
  val resource = describers.representedClass(expression) ?: return null
  val resourceNoun = describers.componentNoun(resource.className, 1)
  val count = minimum.target
  val amount = if (count == 1) describers.indefiniteArticle(resourceNoun) else count.toString()
  val bonusNoun = if (count == 1) bonus.noun.singular else bonus.noun.plural
  return "with $amount $resourceNoun $bonusNoun"
}

private fun renderSpatialRequirement(
    requirement: Requirement,
    describers: Describers,
): String? {
  val relationExpression =
      when (requirement) {
        is Requirement.Min -> (requirement.metric as? Metric.Count)?.expression
        is Requirement.Max -> (requirement.metric as? Metric.Count)?.expression
        is Requirement.And,
        is Requirement.Exact,
        is Requirement.Eval,
        is Requirement.Or,
        is Requirement.Transform -> null
      } ?: return null
  if (relationExpression.refinement != null || relationExpression.complement) return null
  val relation =
      describers.fact(relationExpression.className, ComponentDescriber::spatialRelation)
          ?: return null
  val target = renderSpatialTarget(relationExpression, relation, describers) ?: return null

  return when (requirement) {
    is Requirement.Min -> {
      val count = if (requirement.target == 1) "one" else requirement.target.toString()
      "${relation.phrase} $count or more ${target.plural}"
    }
    is Requirement.Max -> {
      if (requirement.target != 0) return null
      "not ${relation.phrase} any ${target.singular}"
    }
    else -> null
  }
}

private fun renderSpatialTarget(
    relationExpression: Expression,
    relation: ComponentDescriber.SpatialRelation,
    describers: Describers,
): ComponentDescriber.Noun.Counted? {
  if (relationExpression.arguments.isEmpty()) return relation.defaultTarget
  val target = relationExpression.arguments.singleOrNull() ?: return null
  if (
      target.arguments != listOf(describers.anyoneExpression) ||
          target.refinement != null ||
          target.complement
  ) {
    return null
  }
  val placement = describers.fact(target.className, ComponentDescriber::placement) ?: return null
  return ComponentDescriber.Noun.Counted(placement.singular, placement.plural)
}

private fun placementClause(noun: NounPhrase, modifiers: List<Modifier>): Clause.Simple =
    Clause.Simple(Predicate("place", Coordination.one(noun), modifiers))
