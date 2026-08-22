package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

/** Renders a placed component and any structurally described restriction on its site. */
internal fun renderPlacement(
    instruction: Instruction,
    description: ComponentDescriber.Placement,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (!describers.concrete(gain.gaining.className)) return null
  if (gain.gaining.refinement != null || gain.gaining.complement) return null

  val placement = describers.resolvePlacementExpression(gain.gaining) ?: return null
  if (placement.owner != null || placement.unknownDependencies.isNotEmpty()) return null
  val siteModifiers = renderPlacementSites(placement, describers) ?: return null
  val count = gain.count.fixedQuantity() ?: return null
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
    placement: Describers.PlacementExpression,
    describers: Describers,
): List<Modifier>? {
  // No dependencies, including an explicitly authored <>, accept the placement defaults.
  if (placement.sites.isEmpty()) return emptyList()

  val expression = placement.sites.singleOrNull() ?: return null
  val site = describers.placementSite(expression.className) ?: return null
  val resolvedSite = describers.resolveExpression(expression) ?: return null
  if (resolvedSite.sourceDependencies.isNotEmpty() || expression.complement) return null

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
      if (target.ownedByYou) {
        val count = if (requirement.target == 1) "one" else requirement.target.toString()
        "${relation.phrase} $count or more of your ${target.noun.plural}"
      } else {
        val count = if (requirement.target == 1) "one" else requirement.target.toString()
        "${relation.phrase} $count or more ${target.noun.plural}"
      }
    }
    is Requirement.Max -> {
      if (requirement.target != 0) return null
      if (target.ownedByYou) {
        "not ${relation.phrase} any of your ${target.noun.plural}"
      } else {
        "not ${relation.phrase} any ${target.noun.singular}"
      }
    }
    else -> null
  }
}

private data class SpatialTarget(
    val noun: ComponentDescriber.Noun.Counted,
    val ownedByYou: Boolean,
)

private fun renderSpatialTarget(
    relationExpression: Expression,
    relation: ComponentDescriber.SpatialRelation,
    describers: Describers,
): SpatialTarget? {
  if (relationExpression.arguments.isEmpty()) {
    return relation.defaultTarget?.let { SpatialTarget(it, ownedByYou = false) }
  }
  val target = relationExpression.arguments.singleOrNull() ?: return null
  if (
      (target.arguments.isNotEmpty() && target.arguments != listOf(describers.anyoneExpression)) ||
          target.refinement != null ||
          target.complement
  ) {
    return null
  }
  val placement = describers.fact(target.className, ComponentDescriber::placement) ?: return null
  return SpatialTarget(
      ComponentDescriber.Noun.Counted(placement.singular, placement.plural),
      ownedByYou =
          target.arguments.isEmpty() &&
              placement.unqualifiedMetricOwner == ComponentDescriber.MetricOwner.YOU,
  )
}

private fun placementClause(noun: NounPhrase, modifiers: List<Modifier>): Clause.Simple =
    Clause.Simple(Predicate("place", Coordination.one(noun), modifiers))
