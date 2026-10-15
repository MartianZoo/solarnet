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
    references: TypeVariableReferences,
): Clause? {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        else -> return null
      }
  val change = instruction as Instruction.Change
  if (change.quantifier.modality() != Modality.REQUIRED || expression.refinement != null)
      return null
  val placement = resolvePlacementExpression(expression, describers) ?: return null
  if (placement.unknownDependencies.isNotEmpty()) return null
  if (instruction is Gain && placement.owner == describers.anyoneExpression) return null
  val implicitSites =
      if (instruction is Gain) describers.gainDefaultExpressions(expression.className)
      else emptyList()
  val siteModifiers =
      renderRelaxedPlacementDefault(placement, implicitSites, description, describers)
          ?: renderPlacementSites(placement, describers, implicitSites, references)
          ?: return null
  val count = change.count.fixedQuantity() ?: return null
  if (siteModifiers.isNotEmpty() && count != 1) return null
  val noun =
      renderPlacedComponentNoun(placement, count, description, describers, instruction is Remove)
          ?: return null
  return Clause.Simple(
      Predicate(
          Verb(if (instruction is Gain) "place" else "remove"),
          Coordination.one(noun),
          modifiers = siteModifiers,
      )
  )
}

/** Ownership is explicit when selecting an existing piece or describing a replacement. */
internal fun renderPlacedComponentNoun(
    placement: PlacementExpression,
    count: Int,
    description: ComponentDescriber.ChangeFrame.Positioned,
    describers: Describers,
    explicitOwnership: Boolean,
): NounPhrase? {
  val noun =
      NounPhrase(
          description.noun.singular,
          description.noun.plural,
          count = count.takeUnless { it == 1 },
          determiner = description.determiner.takeIf { count == 1 },
      )
  val owner = placement.owner
  return when {
    owner == null ->
        if (
            explicitOwnership &&
                description.unqualifiedOwnership == ComponentDescriber.OwnershipPhrase.YOURS
        ) {
          noun.withModifier(Modifier.Relation("of", NounPhrase.text("your own")))
        } else noun
    owner == describers.anyoneExpression && description.anyoneOwnership != null -> noun
    owner.simple -> {
      val adjective =
          describers.fact(owner.className, ComponentDescriber::ownerAdjective) ?: return null
      noun.withAttributiveModifier(NounPhrase.text(adjective))
    }
    else -> null
  }
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
  val piece = NounPhrase(description.noun.singular, determiner = description.determiner).linearize()
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
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
    siteDeterminer: Determiner? = null,
): List<Modifier>? {
  // No dependencies, including an explicitly authored <>, accept the placement defaults.
  if (placement.sites.isEmpty()) return emptyList()

  val expression = placement.sites.singleOrNull() ?: return null
  val site = describers.placementSite(expression.className) ?: return null
  val resolvedSite = describers.resolveExpression(expression) ?: return null
  if (resolvedSite.sourceDependencies.isNotEmpty()) return null

  val siteNoun =
      if (describers.concrete(expression.className)) {
        describers.namedComponent(expression.className)
      } else {
        NounPhrase(
            describers.describedNoun(expression.className, site.noun, 1),
            determiner =
                siteDeterminer
                    ?: if (references.variableUsedAt(expression) == null) site.determiner
                    else Determiner.THAT,
        )
      }
  val modifiers = mutableListOf<Modifier>(Modifier.Relation(site.relation, siteNoun))
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
          renderPlacementSiteRequirement(requirement, describers, references) ?: return null
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
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
): Modifier? =
    renderSpatialRequirement(requirement, describers, references)
        ?: renderPlacementBonusRequirement(requirement, describers)
        ?: renderSiteOccupantRequirement(requirement, describers)
        ?: renderEmptySiteRequirement(requirement, describers)

private fun renderSiteOccupantRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? {
  val minimum = requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val expression = countedExpression(minimum) ?: return null
  if (!expression.simple || expression.typeVariableName != null) return null
  val frame = describers.positionedFrame(expression.className) ?: return null
  if (
      frame.determiner == Determiner.THIS &&
          (!describers.concrete(expression.className) ||
              !describers.currentCardGainsSingle(expression.className))
  ) {
    return null
  }
  val placement = resolvePlacementExpression(expression, describers) ?: return null
  if (placement.sites.isNotEmpty() || placement.unknownDependencies.isNotEmpty()) return null
  val occupant =
      renderPlacedComponentNoun(
          placement,
          1,
          frame,
          describers,
          frame.determiner != Determiner.THIS,
      ) ?: return null
  return Modifier.Relation("with", occupant)
}

private fun renderEmptySiteRequirement(
    requirement: Requirement,
    describers: Describers,
): Modifier? {
  val maximum = requirement as? Requirement.Max ?: return null
  if (maximum.target != 0) return null
  val expression = countedExpression(maximum) ?: return null
  if (!expression.simple) return null
  val noun = describers.componentNoun(expression.className, 1)
  val placement = describers.positionedFrame(expression.className)
  if (placement?.unqualifiedOwnership == ComponentDescriber.OwnershipPhrase.YOURS) {
    return Modifier.Phrase("with none of your ${placement.noun.plural}")
  }
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
    references: TypeVariableReferences,
): Modifier? {
  val counting = requirement as? Requirement.Counting ?: return null
  val relationExpression = countedExpression(counting) ?: return null
  if (relationExpression.refinement != null) return null
  val relation =
      describers.fact(relationExpression.className, ComponentDescriber::spatialRelation)
          ?: return null
  val target =
      renderSpatialTarget(relationExpression, relation, describers, references) ?: return null

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
    A_PLAYERS,
    THAT_PLAYERS,
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
        Ownership.A_PLAYERS -> "a player owns"
        Ownership.THAT_PLAYERS -> "that player owns"
      }
}

private fun renderSpatialTarget(
    relationExpression: Expression,
    relation: ComponentDescriber.SpatialRelation,
    describers: Describers,
    references: TypeVariableReferences,
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
  val namedOwner =
      resolvedTarget.sourceDependency(ownerKey)?.takeIf {
        it.typeVariableName != null &&
            !describers.expressions.isOwner(it) &&
            it.copy(typeVariableName = null) in
                listOf(describers.anyoneExpression, describers.playerExpression) &&
            resolvedTarget.sourceDependencies.size == 1
      }
  if (
      (resolvedTarget.sourceDependencies.isNotEmpty() &&
          !resolvedTarget.hasOnlySourceDependency(ownerKey, describers.anyoneExpression) &&
          namedOwner == null) || target.refinement != null
  ) {
    return null
  }
  val placement = describers.positionedFrame(target.className) ?: return null
  if (namedOwner != null) {
    return SpatialTarget(
        placement.referenceNoun ?: placement.noun,
        if (references.variableUsedAt(namedOwner) == null) SpatialTarget.Ownership.A_PLAYERS
        else SpatialTarget.Ownership.THAT_PLAYERS,
    )
  }
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
      placement.referenceNoun ?: placement.noun,
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
