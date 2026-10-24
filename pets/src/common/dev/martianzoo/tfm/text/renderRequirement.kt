package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

/** The component expression counted by a cardinality requirement, if it is a component count. */
internal fun countedExpression(requirement: Requirement.Counting): Expression? =
    (requirement.metric as? Metric.Count)?.expression

internal fun renderRequirement(
    requirement: Requirement,
    describers: Describers,
    terse: Boolean = false,
): EnglishText {
  val lowered = describers.lowerProductionSyntax(requirement)
  val clause =
      renderUniformResourceRequirement(lowered, describers)
          ?: renderLoweredRequirement(lowered, describers)
          ?: Clause.RawPets(Unresolved(requirement, RefusalReason.UNKNOWN_REQUIREMENT_FRAME))
  val simple = clause as? Clause.Simple
  val condition =
      (simple?.predicate?.complement as? Predicate.Complement.That)?.clause as? Clause.Simple
  val shortened =
      if (
          terse &&
              condition?.subject == NounPhrase.you() &&
              condition.predicate.verb == Verb.HAVE &&
              condition.predicate.modifiers.isEmpty() &&
              condition.predicate.complement == null
      ) {
        Clause.Simple(Predicate(Verb("requires"), condition.predicate.objects))
      } else clause
  return Sentence(shortened).asText()
}

/** Equal thresholds covering the complete resource category can share their quantity. */
private fun renderUniformResourceRequirement(
    requirement: Requirement,
    describers: Describers,
): Clause? {
  val members =
      when (requirement) {
        is Requirement.And -> requirement.requirements
        is Requirement.Or -> requirement.requirements
        else -> return null
      }
  val minima = members.map { it as? Requirement.Min ?: return null }
  val amount = minima.map { it.minimum }.distinct().singleOrNull() ?: return null
  val expressions = minima.map { minimum ->
    when (val metric = minimum.metric) {
      is Metric.Count -> metric.expression
      is Metric.Subtract -> {
        val offset = (metric.subtrahend as? Metric.Count)?.expression ?: return null
        if (!describers.isProductionOffset(offset)) return null
        val production = (metric.minuend as? Metric.Count)?.expression ?: return null
        val resource = productionExpression(production, describers)?.resource ?: return null
        if (describers.representedClass(offset)?.className != resource) return null
        production
      }
      else -> return null
    }
  }
  if (expressions.any { it.refinement != null }) return null
  val production = expressions.all { describers.isProduction(it.className) }
  val resources = expressions.map {
    if (production) {
      val selected = productionExpression(it, describers) ?: return null
      if (selected.owner != null) return null
      selected.resource
    } else {
      if (!it.simple || !describers.isStandardResource(it.className)) return null
      it.className
    }
  }
  val category = cn("StandardResource")
  if (
      resources.size != resources.toSet().size ||
          resources.toSet() != describers.expressions.concreteSubclassesOf(category).toSet()
  )
      return null
  val selector = if (requirement is Requirement.Or) "any" else "each"
  val objectPhrase =
      if (production) "$amount production of $selector standard resource type"
      else "$amount of $selector standard resource type"
  return Clause.Simple(Predicate(Verb("requires"), Coordination.one(NounPhrase.text(objectPhrase))))
}

private fun renderLoweredRequirement(requirement: Requirement, describers: Describers): Clause? =
    describers.renderDescribedRequirementCondition(requirement)?.let(::requirementClause)
        ?: when (requirement) {
          is Requirement.Min -> renderMinimum(requirement, describers)
          is Requirement.Max -> renderMaximum(requirement, describers)
          is Requirement.And -> describers.renderRequirementGroup(requirement)
          is Requirement.Eval,
          is Requirement.Exact,
          is Requirement.Or -> null
          is Requirement.Transform -> null
        }

internal fun Describers.renderDescribedRequirementCondition(
    requirement: Requirement,
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
): Clause? =
    when (requirement) {
      is Requirement.And ->
          requirement.requirements
              .sortedBy { it is Requirement.Or }
              .map { renderDescribedRequirementCondition(it, references) ?: return null }
              .let { Clause.Coordinated(Coordination(it, Conjunction.AND)) }
      is Requirement.Or ->
          requirement.requirements
              .map { renderDescribedRequirementCondition(it, references) ?: return null }
              .let { Clause.Either(Coordination(it, Conjunction.OR)) }
      is Requirement.Min -> renderDescribedMinimumCondition(requirement, references)
      is Requirement.Max -> renderDescribedMaximumCondition(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Transform -> null
    }

private fun Describers.renderDescribedMinimumCondition(
    requirement: Requirement.Min,
    references: TypeVariableReferences,
): Clause? {
  val expression = countedExpression(requirement) ?: return null
  return when (val frame = fact(expression.className, ComponentDescriber::requirementCondition)) {
    is ComponentDescriber.RequirementCondition.ArgumentState -> {
      if (requirement.target != 1 || expression.refinement != null) return null
      val resolved = resolveExpression(expression) ?: return null
      val subject = resolved.sourceDependency(frame.dependency) ?: return null
      if (isGameParticipant(subject.className)) {
        val ownerKey = Key(OWNED, 0)
        if (
            resolved.sourceDependency(ownerKey)?.let { !expressions.isOwner(it) } == true ||
                resolved.sourceDependencies.keys.any { it != ownerKey && it != frame.dependency } ||
                subject.refinement != null
        )
            return null
        val determiner =
            if (references.variableUsedAt(subject) == null) Determiner.INDEFINITE
            else Determiner.THAT
        return Clause.Simple(
            Predicate(Verb(frame.predicate)),
            NounPhrase("player", determiner = determiner),
        )
      }
      val represented = representedClassArgument(subject)
      val noun =
          if (represented != null) {
            if (!concrete(represented.className)) return null
            scaleFrame(represented.className)?.subject
                ?: "the number of ${positionedFrame(represented.className)?.noun?.plural ?: return null}"
          } else {
            if (!subject.simple || !concrete(subject.className)) return null
            componentNoun(subject.className, 1)
          }
      Clause.Simple(Predicate(Verb(frame.predicate)), NounPhrase.text(noun))
    }
    is ComponentDescriber.RequirementCondition.OwnedCount ->
        renderOwnedCountCondition(expression, requirement.target, frame)
    is ComponentDescriber.RequirementCondition.OwnerState -> {
      if (
          requirement.target != 1 ||
              expression.refinement != null ||
              expression.arguments.isNotEmpty()
      ) {
        return null
      }
      Clause.Simple(Predicate(Verb(frame.predicate)), NounPhrase.you())
    }
    null -> renderNamedRequirementCondition(requirement)
  }
}

private fun Describers.renderDescribedMaximumCondition(
    requirement: Requirement.Max,
): Clause? {
  if (requirement.maximum != 0) return null
  val expression = countedExpression(requirement) ?: return null
  val frame = fact(expression.className, ComponentDescriber::requirementCondition)
  if (frame is ComponentDescriber.RequirementCondition.OwnedCount) {
    return renderOwnedCountCondition(expression, 0, frame)
  }
  if (frame !is ComponentDescriber.RequirementCondition.OwnerState) return null
  if (expression.refinement != null || expression.arguments.singleOrNull() != anyoneExpression) {
    return null
  }
  return Clause.Simple(
      Predicate(Verb(frame.predicate)),
      NounPhrase("player", "players", determiner = Determiner.NO),
  )
}

private fun Describers.renderOwnedCountCondition(
    expression: Expression,
    count: Int,
    frame: ComponentDescriber.RequirementCondition.OwnedCount,
): Clause? {
  if (expression.refinement != null) return null
  val resolved = resolveExpression(expression) ?: return null
  if (
      resolved.sourceDependencies.keys.any {
        it != frame.ownerDependency && it != frame.qualifierDependency
      }
  )
      return null
  val owner =
      frame.ownerDependency?.let(resolved::sourceDependency)?.takeUnless { it == ownerExpression }
  val ownerAdjective =
      owner
          ?.takeUnless { it == anyoneExpression }
          ?.let {
            if (!it.simple) return null
            fact(it.className, ComponentDescriber::ownerAdjective) ?: return null
          }
  val countedNoun =
      if (owner != null && frame.ownerVerb != null)
          positionedFrame(expression.className)?.noun ?: return null
      else frame.noun
  val noun =
      if (ownerAdjective != null) {
        ComponentDescriber.Noun.Counted(
            "$ownerAdjective ${countedNoun.singular}",
            "$ownerAdjective ${countedNoun.plural}",
        )
      } else {
        countedNoun
      }
  var amount = quantifiedNoun(noun, count)
  frame.qualifierDependency?.let { dependency ->
    val qualifier = resolved.sourceDependency(dependency)
    val qualifierPhrase =
        when {
          qualifier == null || (qualifier.simple && !concrete(qualifier.className)) ->
              NounPhrase.text(frame.unboundQualifier ?: return null)
          qualifier.simple -> NounPhrase.text(componentNoun(qualifier.className, 1))
          else -> return null
        }
    amount =
        amount.withModifier(
            Modifier.Relation(frame.qualifierRelation ?: return null, qualifierPhrase)
        )
  }
  if (owner == null) {
    frame.singleOwnerState
        ?.takeIf { count == 1 }
        ?.let { state ->
          return Clause.Simple(Predicate(Verb(state)), NounPhrase.you())
        }
    return Clause.Simple(
        Predicate(Verb(frame.ownerVerb ?: "have"), Coordination.one(amount)),
        NounPhrase.you(),
    )
  }
  return Clause.Simple(
      Predicate(Verb.BE, Coordination.one(amount)),
      NounPhrase("there", grammaticalNumber = amount.number()),
  )
}

private fun renderMinimum(requirement: Requirement.Min, describers: Describers): Clause? =
    describers.renderMinimum(requirement)

private fun Describers.renderNamedRequirementCondition(requirement: Requirement.Min): Clause? {
  if (requirement.target != 1) return null
  val expression = countedExpression(requirement)?.takeIf { it.refinement == null } ?: return null
  val kind = fact(expression.className, ComponentDescriber::requirementKind) ?: return null
  val qualifier =
      resolveExpression(expression)?.sourceDependencies?.values?.singleOrNull()?.takeIf {
        it.simple && concrete(it.className)
      } ?: return null
  val namedRequirement =
      NounPhrase("$kind requirement", determiner = Determiner.THE)
          .withModifier(
              Modifier.Relation(
                  "for",
                  NounPhrase.text(componentNoun(qualifier.className, 1)),
              )
          )
  return Clause.Simple(
      Predicate(Verb("meet"), Coordination.one(namedRequirement)),
      NounPhrase.you(),
  )
}

private fun renderMaximum(requirement: Requirement.Max, describers: Describers): Clause? =
    describers.renderMaximum(requirement)

private fun Describers.renderMinimum(requirement: Requirement.Min): Clause? {
  val expression = countedExpression(requirement)
  val target = requirement.target
  if (expression != null) {
    renderCountedRelation(expression, this)
        ?.takeIf { target == 1 }
        ?.let { relation ->
          val objectPhrase =
              if (relation.source.ownedByYou) {
                relation.source
                    .referenceWithoutOwnership(Determiner.INDEFINITE)
                    .withModifier(Modifier.Relation(relation.phrase, relation.target.reference()))
              } else {
                relation.asRequirement()
              }
          return if (relation.source.ownedByYou) ownedRequirementClause(objectPhrase)
          else requirementClause(objectPhrase)
        }
    fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
      return renderRequirementBound(expression, target, bound, BoundDirection.MINIMUM)
    }
  }
  return renderProductionRequirement(requirement)
      ?: renderCardResourceRequirement(requirement)
      ?: renderTagRequirement(requirement)
      ?: renderDistinctKindsRequirement(requirement)
      ?: renderMetricRequirement(requirement)
}

private fun Describers.renderDistinctKindsRequirement(
    requirement: Requirement.Min,
): Clause? {
  val counts =
      when (val metric = requirement.metric) {
        is Metric.Count -> listOf(metric.expression)
        is Metric.Or -> metric.metrics.map { it.expression }
        else -> return null
      }
  val expression = counts.singleOrNull { distinctOwnedKinds(it, this) != null } ?: return null
  val noun = distinctOwnedKinds(expression, this) ?: return null
  val kind = resolveExpression(expression)?.sourceDependency(Key(CLASS, 0))
  if (counts.size > 1) {
    if (counts.size != 2 || kind?.simple != true || !isTag(kind.className)) return null
    val wild = counts.single { it != expression }
    if (
        wild.arguments.isNotEmpty() ||
            fact(wild.className, ComponentDescriber::tagAssignmentHolder) != true
    )
        return null
    val absent =
        (wild.refinement as? Expression.Refinement.Has)?.requirement as? Requirement.Max
            ?: return null
    if (absent.target != 0 || countedExpression(absent) != cn("Tag").expression) return null
  }
  val amount = quantifiedNoun(noun, requirement.target)
  return if (kind?.simple == true && isTag(kind.className)) requirementClause(amount)
  else ownedRequirementClause(amount)
}

private fun Describers.renderMaximum(requirement: Requirement.Max): Clause? {
  val expression = countedExpression(requirement) ?: return null
  val bound = fact(expression.className, ComponentDescriber::requirement)?.maximum ?: return null
  return renderRequirementBound(
      expression,
      requirement.target,
      bound,
      BoundDirection.MAXIMUM,
  )
}

private fun Describers.renderRequirementGroup(requirement: Requirement.And): Clause? =
    renderTagRequirementGroup(requirement)
        ?: renderOwnedPlacementRequirementGroup(requirement)
        ?: renderMetricRequirementGroup(requirement)

private fun Describers.renderMetricRequirement(requirement: Requirement.Min): Clause? {
  val phrase =
      renderRequirementMetricPhrase(requirement.metric, requirement.target, this) ?: return null
  return ownedRequirementClause(phrase)
}

private fun Describers.renderMetricRequirementGroup(requirement: Requirement.And): Clause? {
  val phrases =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        renderRequirementMetricPhrase(minimum.metric, minimum.target, this) ?: return null
      }
  return ownedRequirementClause(Coordination(phrases, Conjunction.AND))
}

private fun Describers.renderProductionRequirement(minimum: Requirement.Min): Clause? {
  if (minimum.target != 1) return null
  val expression = countedExpression(minimum) ?: return null
  val production = productionExpression(expression, this) ?: return null
  if (production.owner != null) return null
  return ownedRequirementClause(productionNounPhrase(production.resource))
}

private fun Describers.renderCardResourceRequirement(requirement: Requirement.Min): Clause? {
  val expression = countedExpression(requirement) ?: return null
  if (!expression.simple) return null
  val noun = cardResourceNounPhrase(expression.className, requirement.target) ?: return null
  val quantified =
      if (requirement.target == 1) {
        noun.copy(count = null, determiner = Determiner.INDEFINITE)
      } else {
        noun
      }
  return ownedRequirementClause(quantified)
}

private fun Describers.renderTagRequirement(requirement: Requirement.Min): Clause? {
  val noun = tagNoun(requirement) ?: return null
  return requirementClause(quantifiedNoun(noun, requirement.target))
}

private fun Describers.renderTagRequirementGroup(requirement: Requirement.And): Clause? {
  val tags =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        val noun = tagNoun(minimum) ?: return null
        quantifiedNoun(noun, minimum.target)
      }
  return requirementClause(Coordination(tags, Conjunction.AND))
}

private fun Describers.tagNoun(
    requirement: Requirement.Min,
): ComponentDescriber.Noun.Counted? {
  val expression = countedExpression(requirement) ?: return null
  if (!expression.simple) return null
  return tagNoun(expression.className)
}

private fun Describers.renderOwnedPlacementRequirementGroup(requirement: Requirement.And): Clause? {
  val nouns =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        val expression = countedExpression(minimum) ?: return null
        if (!expression.simple) return null
        val noun =
            fact(expression.className, ComponentDescriber::requirement)?.ownedCount
                as? ComponentDescriber.Noun.Counted ?: return null
        quantifiedNoun(noun, minimum.target)
      }
  return ownedRequirementClause(Coordination(nouns, Conjunction.AND))
}

private fun Describers.renderRequirementBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound,
    direction: BoundDirection,
): Clause? {
  return when (bound) {
    is ComponentDescriber.Requirement.Bound.Threshold ->
        renderThresholdBound(expression, target, bound, direction)
    is ComponentDescriber.Requirement.Bound.Count ->
        renderCountBound(expression, target, bound, direction)
  }
}

private fun Describers.renderThresholdBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound.Threshold,
    direction: BoundDirection,
): Clause? {
  if (!expression.simple) return null
  val value = renderRequirementValue(bound.value, target)
  val phrase =
      when (bound.value) {
        ComponentDescriber.Requirement.Value.PLAIN ->
            "$value ${bound.subject}" + if (direction == BoundDirection.MAXIMUM) " or lower" else ""
        ComponentDescriber.Requirement.Value.PERCENT ->
            "$value ${bound.subject}" + if (direction == BoundDirection.MAXIMUM) " or less" else ""
        ComponentDescriber.Requirement.Value.DOUBLE_PERCENT ->
            "${bound.subject} $value" + if (direction == BoundDirection.MAXIMUM) " or lower" else ""
        ComponentDescriber.Requirement.Value.TEMPERATURE ->
            "$value or ${if (direction == BoundDirection.MINIMUM) "warmer" else "colder"}"
      }
  val requirement = NounPhrase.text(phrase)
  return if (isPlayerOwned(expression.className)) ownedRequirementClause(requirement)
  else requirementClause(requirement)
}

private fun Describers.renderCountBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound.Count,
    direction: BoundDirection,
): Clause? {
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (expression.refinement != null) return null
  val explicitlyAnyOwner = resolved.hasOnlySourceDependency(ownerKey, anyoneExpression)
  val owned =
      when {
        resolved.sourceDependencies.isEmpty() -> isPlayerOwned(expression.className)
        explicitlyAnyOwner -> false
        else -> return null
      }
  val noun = if (target == 1) bound.noun.singular else bound.noun.plural
  return when (direction) {
    BoundDirection.MINIMUM ->
        when {
          owned -> ownedRequirementClause(quantifiedNoun(bound.noun, target))
          explicitlyAnyOwner -> {
            val amount =
                NounPhrase(bound.noun.singular, bound.noun.plural, count = target)
                    .withModifier(Modifier.Phrase("in play"))
            requirementClause(amount)
          }
          else -> requirementClause(NounPhrase(noun, count = target))
        }
    BoundDirection.MAXIMUM ->
        NounPhrase.text("$target or fewer ${bound.noun.plural}").let { amount ->
          if (owned) ownedRequirementClause(amount) else requirementClause(amount)
        }
  }
}

private enum class BoundDirection {
  MINIMUM,
  MAXIMUM,
}

private fun requirementClause(objectPhrase: NounPhrase): Clause.Simple =
    requirementClause(Coordination.one(objectPhrase))

private fun requirementClause(objects: Coordination<NounPhrase>): Clause.Simple =
    Clause.Simple(
        predicate = Predicate(Verb("requires"), objects),
    )

private fun ownedRequirementClause(objectPhrase: NounPhrase): Clause.Simple =
    ownedRequirementClause(Coordination.one(objectPhrase))

private fun ownedRequirementClause(objects: Coordination<NounPhrase>): Clause.Simple =
    requirementClause(NounPhrase.you(), Verb.HAVE, objects)

private fun requirementClause(
    subject: NounPhrase,
    verb: Verb,
    objectPhrase: NounPhrase,
): Clause.Simple = requirementClause(subject, verb, Coordination.one(objectPhrase))

private fun requirementClause(
    subject: NounPhrase,
    verb: Verb,
    objects: Coordination<NounPhrase>,
): Clause.Simple =
    requirementClause(
        Clause.Simple(
            Predicate(verb, objects),
            subject,
        )
    )

private fun requirementClause(complement: Clause): Clause.Simple =
    Clause.Simple(
        predicate =
            Predicate(
                Verb("requires"),
                complement = Predicate.Complement.That(complement),
            ),
    )

private fun Describers.quantifiedNoun(
    noun: ComponentDescriber.Noun.Counted,
    count: Int,
): NounPhrase =
    if (count == 1) {
      NounPhrase(noun.singular, noun.plural, determiner = Determiner.INDEFINITE)
    } else {
      NounPhrase(noun.singular, noun.plural, count = count)
    }

private fun renderRequirementValue(
    value: ComponentDescriber.Requirement.Value,
    target: Int,
): String =
    when (value) {
      ComponentDescriber.Requirement.Value.PLAIN -> target.toString()
      ComponentDescriber.Requirement.Value.PERCENT -> "$target%"
      ComponentDescriber.Requirement.Value.DOUBLE_PERCENT -> "${target * 2}%"
      ComponentDescriber.Requirement.Value.TEMPERATURE -> {
        val degrees = -30 + 2 * target
        "${if (degrees > 0) "+" else ""}${degrees}°C"
      }
    }
