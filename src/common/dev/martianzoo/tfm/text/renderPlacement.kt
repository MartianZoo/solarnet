package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

/** Renders a placed component and any structurally described restriction on its site. */
internal fun renderPlacement(
    instruction: Instruction,
    description: ComponentDescriber.ChangeFrame.Positioned,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain
  if (gain == null) {
    val removal = instruction as? Remove ?: return null
    if (
        removal.quantifier.modality() != Modality.REQUIRED ||
            removal.removing.refinement != null ||
            !removal.removing.simple ||
            !describers.concrete(removal.removing.className)
    ) {
      return null
    }
    val count = removal.count.fixedQuantity() ?: return null
    return Clause.Simple(
        Predicate(
            Verb("remove"),
            Coordination.one(
                describers.quantifiedComponentNounPhrase(
                    removal.removing.className,
                    count,
                    description.singular,
                    description.plural,
                    description.determiner,
                )
            ),
        )
    )
  }
  if (gain.quantifier.modality() != Modality.REQUIRED) return null
  if (!describers.concrete(gain.gaining.className)) return null
  if (gain.gaining.refinement != null) return null

  val placement = resolvePlacementExpression(gain.gaining, describers) ?: return null
  if (placement.owner != null || placement.unknownDependencies.isNotEmpty()) return null
  val implicitSites = describers.gainDefaultExpressions(gain.gaining.className)
  val siteModifiers =
      renderRelaxedPlacementDefault(placement, implicitSites, description, describers)
          ?: renderPlacementSites(placement, describers, implicitSites)
          ?: return null
  val count = gain.count.fixedQuantity() ?: return null
  if (siteModifiers.isNotEmpty() && count != 1) return null
  val noun =
      describers.quantifiedComponentNounPhrase(
          gain.gaining.className,
          count,
          description.singular,
          description.plural,
          description.determiner,
      )
  return placementClause(noun, siteModifiers)
}

private fun renderRelaxedPlacementDefault(
    placement: PlacementExpression,
    implicitSites: List<Expression>,
    description: ComponentDescriber.ChangeFrame.Positioned,
    describers: Describers,
): List<Modifier>? {
  val site = placement.sites.singleOrNull()?.takeIf { it.refinement == null } ?: return null
  val defaultSite = implicitSites.singleOrNull { it.className == site.className } ?: return null
  val restriction = defaultSite.refinement?.requirementsOrNull()?.singleOrNull() ?: return null
  val maximum = restriction as? Requirement.Max ?: return null
  val excluded = countedExpression(maximum) ?: return null
  if (
      maximum.target != 0 ||
          !excluded.simple ||
          describers.positionedFrame(excluded.className) != description
  ) {
    return null
  }
  val piece = NounPhrase(description.singular, determiner = description.determiner).linearize()
  return listOf(Modifier.Parenthetical("may be placed where you already have $piece"))
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
    implicitSites: List<Expression> = emptyList(),
): List<Modifier>? {
  // No dependencies, including an explicitly authored <>, accept the placement defaults.
  if (placement.sites.isEmpty()) return emptyList()

  val expression = placement.sites.singleOrNull() ?: return null
  val site = describers.placementSite(expression.className) ?: return null
  val resolvedSite = describers.resolveExpression(expression) ?: return null
  if (resolvedSite.sourceDependencies.isNotEmpty()) return null

  val siteNoun = describers.describedNoun(expression.className, site.noun, 1)
  val modifiers =
      mutableListOf<Modifier>(
          Modifier.Relation("on", NounPhrase(siteNoun, determiner = site.determiner))
      )
  expression.refinement?.let {
    val authoredRequirements = it.requirementsOrNull() ?: return null
    val implicitRequirements =
        implicitSites
            .singleOrNull { it.className == expression.className }
            ?.refinement
            ?.requirementsOrNull()
            .orEmpty()
    val novelRequirements = authoredRequirements.filterNot { it in implicitRequirements }
    val renderedRequirements =
        (novelRequirements.ifEmpty { authoredRequirements }).map { requirement ->
          renderPlacementSiteRequirement(requirement, describers) ?: return null
        }
    modifiers += renderedRequirements
  }
  return modifiers
}

internal fun Expression.Refinement.requirementsOrNull(): List<Requirement>? =
    when (this) {
      is Expression.Refinement.Has -> listOf(requirement)
      is Expression.Refinement.And ->
          refinements.map { (it as? Expression.Refinement.Has)?.requirement ?: return null }
      is Expression.Refinement.Not -> null
    }

internal fun renderPlacementSiteRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? =
    renderSpatialRequirement(requirement, describers)
        ?: renderPlacementBonusRequirement(requirement, describers)
        ?: renderEmptySiteRequirement(requirement, describers)

private fun renderEmptySiteRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? {
  val maximum = requirement as? Requirement.Max ?: return null
  if (maximum.target != 0) return null
  val expression = countedExpression(maximum) ?: return null
  if (!expression.simple) return null
  val noun = describers.componentNoun(expression.className, 1)
  return Modifier.Relation("with", NounPhrase(noun, determiner = Determiner.NO))
}

private fun renderPlacementBonusRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? {
  val minimum = requirement as? Requirement.Min ?: return null
  val expression = countedExpression(minimum) ?: return null
  if (expression.refinement != null) return null
  val bonus =
      describers.fact(expression.className, ComponentDescriber::placementBonus) ?: return null
  val resource = describers.representedClass(expression) ?: return null
  val count = minimum.target
  val resourceNoun = describers.componentNoun(resource.className, count)
  val noun =
      NounPhrase(
          "$resourceNoun ${bonus.noun.singular}",
          "$resourceNoun ${bonus.noun.plural}",
          count = count.takeUnless { it == 1 },
          determiner = Determiner.INDEFINITE.takeIf { count == 1 },
      )
  return Modifier.Relation("with", noun)
}

private fun renderSpatialRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? {
  val counting = requirement as? Requirement.Counting ?: return null
  val relationExpression = countedExpression(counting) ?: return null
  if (relationExpression.refinement != null) return null
  val relation =
      describers.fact(relationExpression.className, ComponentDescriber::spatialRelation)
          ?: return null
  val target = renderSpatialTarget(relationExpression, relation, describers) ?: return null

  return when (counting) {
    is Requirement.Min -> Modifier.Relation(relation.phrase, target.minimumPhrase(counting.target))
    is Requirement.Max -> {
      if (counting.target != 0) return null
      Modifier.Relation(relation.phrase, target.absencePhrase())
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

  fun minimumPhrase(count: Int): NounPhrase {
    val noun = if (count == 1) noun.singular else noun.plural
    val suffix = ownership.suffix()
    if (count != 1) {
      return NounPhrase.text("any ${spelledOutCount(count)} $noun").withSuffix(suffix)
    }
    val determiner =
        when {
          implicit -> Determiner.ANOTHER
          explicitlyAny && ownership == Ownership.UNRESTRICTED -> Determiner.ANY
          else -> Determiner.INDEFINITE
        }
    return NounPhrase(noun, determiner = determiner).withSuffix(suffix)
  }

  fun absencePhrase(): NounPhrase {
    val other = if (implicit) "other " else ""
    return NounPhrase("$other${noun.singular}", determiner = Determiner.NO)
        .withSuffix(ownership.suffix())
  }

  private fun NounPhrase.withSuffix(suffix: String): NounPhrase =
      if (suffix.isEmpty()) this else withModifier(Modifier.Phrase(suffix))

  private fun Ownership.suffix(): String =
      when (this) {
        Ownership.UNRESTRICTED -> ""
        Ownership.YOURS -> "you own"
        Ownership.ANYONES -> "anyone owns"
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
          target.refinement != null
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
    Clause.Simple(Predicate(Verb("place"), Coordination.one(noun), modifiers))
