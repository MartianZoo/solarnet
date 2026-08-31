package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

/** Renders a placed component and any structurally described restriction on its site. */
internal fun renderPlacement(
    instruction: Instruction,
    description: ComponentDescriber.ChangeFrame.Positioned,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (!describers.concrete(gain.gaining.className)) return null
  if (gain.gaining.refinement != null || gain.gaining.complement) return null

  val placement = resolvePlacementExpression(gain.gaining, describers) ?: return null
  if (placement.owner != null || placement.unknownDependencies.isNotEmpty()) return null
  val siteModifiers = renderPlacementSites(placement, describers) ?: return null
  val count = gain.count.fixedQuantity() ?: return null
  if (siteModifiers.isNotEmpty() && count != 1) return null
  val noun =
      describers.quantifiedComponentNounPhrase(
          gain.gaining.className,
          count,
          description.singular,
          description.plural,
          description.article,
      )
  return placementClause(noun, siteModifiers)
}

internal fun resolvePlacementExpression(
    expression: Expression,
    describers: Describers,
): PlacementExpression? {
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val siteDependencies =
      resolved.sourceDependencies.filterKeys { key ->
        val siteClassName =
            resolved.dependency(key)?.rootClass?.className ?: return@filterKeys false
        describers.placementSite(siteClassName) != null
      }
  val recognizedKeys = siteDependencies.keys + ownerKey
  return PlacementExpression(
      owner = resolved.sourceDependency(ownerKey)?.takeUnless { it == describers.ownerExpression },
      sites = siteDependencies.values.toList(),
      unknownDependencies = resolved.sourceDependencies.keys - recognizedKeys,
  )
}

internal fun renderPlacementSites(
    placement: PlacementExpression,
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
  val expression = countedExpression(minimum) ?: return null
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
  val counting = requirement as? Requirement.Counting ?: return null
  val relationExpression = countedExpression(counting) ?: return null
  if (relationExpression.refinement != null || relationExpression.complement) return null
  val relation =
      describers.fact(relationExpression.className, ComponentDescriber::spatialRelation)
          ?: return null
  val target = renderSpatialTarget(relationExpression, relation, describers) ?: return null

  return when (counting) {
    is Requirement.Min -> "${relation.phrase} ${target.minimumPhrase(counting.target, describers)}"
    is Requirement.Max -> {
      if (counting.target != 0) return null
      "${relation.phrase} ${target.absencePhrase()}"
    }
    is Requirement.Exact -> null
  }
}

private data class SpatialTarget(
    val noun: ComponentDescriber.Noun.Counted,
    val ownership: Ownership,
    val implicit: Boolean = false,
    val explicitlyAny: Boolean = false,
) {
  enum class Ownership {
    UNRESTRICTED,
    YOURS,
    ANYONES,
  }

  fun minimumPhrase(count: Int, describers: Describers): String {
    val noun = if (count == 1) noun.singular else noun.plural
    val suffix = ownership.suffix()
    if (count != 1) return "any ${spelledOutCount(count)} $noun$suffix"
    val determiner =
        when {
          implicit -> "another"
          explicitlyAny && ownership == Ownership.UNRESTRICTED -> "any"
          ownership == Ownership.UNRESTRICTED && suffix.isEmpty() ->
              describers.indefiniteArticle(noun)
          else -> "a"
        }
    return "$determiner $noun$suffix"
  }

  fun absencePhrase(): String {
    val other = if (implicit) "other " else ""
    return "no $other${noun.singular}${ownership.suffix()}"
  }

  private fun Ownership.suffix(): String =
      when (this) {
        Ownership.UNRESTRICTED -> ""
        Ownership.YOURS -> " you own"
        Ownership.ANYONES -> " anyone owns"
      }
}

private fun renderSpatialTarget(
    relationExpression: Expression,
    relation: ComponentDescriber.SpatialRelation,
    describers: Describers,
): SpatialTarget? {
  val resolvedRelation = describers.resolveExpression(relationExpression) ?: return null
  if (resolvedRelation.sourceDependencies.isEmpty()) {
    return relation.defaultTarget?.let {
      SpatialTarget(it, SpatialTarget.Ownership.UNRESTRICTED, implicit = true)
    }
  }
  val target = resolvedRelation.sourceDependencies.values.singleOrNull() ?: return null
  val resolvedTarget = describers.resolveExpression(target) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (
      (resolvedTarget.sourceDependencies.isNotEmpty() &&
          !resolvedTarget.hasOnlySourceDependency(ownerKey, describers.anyoneExpression)) ||
          target.refinement != null ||
          target.complement
  ) {
    return null
  }
  val placement = describers.positionedFrame(target.className) ?: return null
  val explicitlyAnyOwner =
      resolvedTarget.hasOnlySourceDependency(ownerKey, describers.anyoneExpression)
  val ownership =
      when {
        explicitlyAnyOwner -> placement.anyoneOwnership ?: return null
        resolvedTarget.sourceDependencies.isEmpty() ->
            placement.unqualifiedOwnership ?: ComponentDescriber.OwnershipPhrase.IMPLICIT
        else -> return null
      }
  return SpatialTarget(
      placement.referenceNoun
          ?: ComponentDescriber.Noun.Counted(placement.singular, placement.plural),
      ownership.asSpatialOwnership(),
      explicitlyAny = explicitlyAnyOwner,
  )
}

private fun ComponentDescriber.OwnershipPhrase.asSpatialOwnership(): SpatialTarget.Ownership =
    when (this) {
      ComponentDescriber.OwnershipPhrase.IMPLICIT -> SpatialTarget.Ownership.UNRESTRICTED
      ComponentDescriber.OwnershipPhrase.YOURS -> SpatialTarget.Ownership.YOURS
      ComponentDescriber.OwnershipPhrase.ANYONES -> SpatialTarget.Ownership.ANYONES
    }

private fun spelledOutCount(count: Int): String =
    when (count) {
      0 -> "zero"
      1 -> "one"
      2 -> "two"
      3 -> "three"
      4 -> "four"
      5 -> "five"
      6 -> "six"
      7 -> "seven"
      8 -> "eight"
      9 -> "nine"
      10 -> "ten"
      else -> count.toString()
    }

private fun placementClause(noun: NounPhrase, modifiers: List<Modifier>): Clause.Simple =
    Clause.Simple(Predicate("place", Coordination.one(noun), modifiers))
