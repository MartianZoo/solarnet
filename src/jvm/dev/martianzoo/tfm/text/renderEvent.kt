package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as TriggerFrame

internal fun Describers.renderEvent(trigger: Trigger): Event? {
  if (trigger is ByTrigger) {
    if (trigger.by != anyoneExpression) return null
    val expression = (trigger.inner as? OnGainOf)?.expression ?: return null
    relationshipEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
    placementEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
    if (!expression.simple) return null
    return scaleFrame(expression.className)?.let {
      Event(
          Event.Kind.RAISE,
          Event.ActorConstraint.UNRESTRICTED,
          NounPhrase.text(it.subject),
          listOf(Modifier.Phrase("1 step")),
      )
    }
  }
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.complement) return null
  unrestrictedPlayedTagEvent(expression)?.let {
    return it
  }
  relationshipEvent(expression, Event.ActorConstraint.YOU)?.let {
    return it
  }
  productionEvent(expression)?.let {
    return it
  }
  if (expression.refinement == null) {
    scaleFrame(expression.className)?.let {
      return Event(
          Event.Kind.RAISE,
          Event.ActorConstraint.YOU,
          NounPhrase.text(it.subject),
          listOf(Modifier.Phrase("1 step")),
      )
    }
  }
  purchaseEvent(expression)?.let {
    return it
  }
  playedCardEvent(expression)?.let {
    return it
  }
  if (expression.refinement != null) return null
  when (val frame = triggerFrame(expression.className)) {
    is TriggerFrame.PlayCard -> {
      if (expression.simple) {
        return Event(
            Event.Kind.PLAY,
            Event.ActorConstraint.YOU,
            NounPhrase("card", determiner = Determiner.INDEFINITE),
        )
      }
      val represented = representedExpression(expression) ?: return null
      return playedCardEvent(represented)
    }
    is TriggerFrame.PlayTag -> {
      if (frame.noun == null) {
        val tag = representedClass(expression) ?: return null
        val name = tagName(tag.className) ?: return null
        return Event(
            Event.Kind.PLAY,
            Event.ActorConstraint.YOU,
            NounPhrase("$name tag", determiner = Determiner.INDEFINITE),
        )
      }
    }
    else -> Unit
  }
  playedTagPhrase(expression.className)?.let { objectPhrase ->
    val resolved = resolveExpression(expression) ?: return null
    if (resolved.sourceDependencies.isNotEmpty() || expression.refinement != null) return null
    return Event(Event.Kind.PLAY, Event.ActorConstraint.YOU, objectPhrase)
  }
  actionUseEvent(trigger)?.let { actionUse ->
    val action = actionUse.provider
    val objectPhrase =
        if (action == thisExpression) {
          NounPhrase.text("this action")
        } else {
          renderActionUse(action) ?: return null
        }
    return Event(
        Event.Kind.USE_ACTION,
        Event.ActorConstraint.YOU,
        objectPhrase,
    )
  }
  val resolvedCardResource = resolveCardResource(expression)
  if (resolvedCardResource != null && cardResourceHasHolder(resolvedCardResource, thisExpression)) {
    cardResourceNoun(expression.className, 1)?.let {
      return Event(
          Event.Kind.ADD,
          Event.ActorConstraint.YOU,
          NounPhrase(it, determiner = Determiner.INDEFINITE),
          listOf(Modifier.Phrase("to this card")),
      )
    }
  }
  if (resolvePlacementExpression(expression, this)?.owner == anyoneExpression) {
    placementEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
  }
  placementEvent(expression, Event.ActorConstraint.YOU)?.let {
    return it
  }
  val resolved = resolveExpression(expression)
  if (resolved?.sourceDependencies?.isEmpty() == true && expression.refinement == null) {
    tagName(expression.className)?.let { name ->
      return Event(
          Event.Kind.PLAY,
          Event.ActorConstraint.YOU,
          NounPhrase("$name tag", determiner = Determiner.INDEFINITE),
      )
    }
    cardResourceNoun(expression.className, 1)?.let {
      return Event(
          Event.Kind.ADD,
          Event.ActorConstraint.YOU,
          NounPhrase(it, determiner = Determiner.INDEFINITE),
          listOf(Modifier.Phrase("to any card")),
      )
    }
  }
  if (resolved?.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression) == true) {
    tagName(expression.className)?.let { name ->
      return Event(
          Event.Kind.PLAY,
          Event.ActorConstraint.UNRESTRICTED,
          NounPhrase("$name tag", determiner = Determiner.ANY),
      )
    }
  }
  return null
}

private fun Describers.unrestrictedPlayedTagEvent(expression: Expression): Event? {
  if (expression.refinement != null || expression.complement) return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val holderKey = Key(ClassName.cn("Tag"), 0)
  val holder = resolved.sourceDependency(holderKey) ?: return null
  if (
      resolved.sourceDependency(ownerKey) != anyoneExpression ||
          resolved.sourceDependencies.keys != setOf(ownerKey, holderKey)
  ) {
    return null
  }
  val resolvedHolder = resolveExpression(holder) ?: return null
  if (
      !resolvedHolder.hasOnlySourceDependency(ownerKey, anyoneExpression) ||
          holder.refinement != null ||
          holder.complement ||
          fact(holder.className, ComponentDescriber::cardResourceHolder) == null
  ) {
    return null
  }
  val name = tagName(expression.className) ?: return null
  return Event(
      Event.Kind.PLAY,
      Event.ActorConstraint.UNRESTRICTED,
      NounPhrase("$name tag", determiner = Determiner.ANY),
  )
}

private fun Describers.relationshipEvent(
    expression: Expression,
    actorConstraint: Event.ActorConstraint,
): Event? {
  if (expression.refinement != null || expression.complement) return null
  val relation = fact(expression.className, ComponentDescriber::spatialRelation) ?: return null
  val noun = relation.eventNoun ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val sourceKey = Key(ClassName.cn("Adjacency"), 0)
  val targetKey = Key(ClassName.cn("Adjacency"), 1)
  if (resolved.sourceDependencies.keys != setOf(sourceKey, targetKey)) return null
  val source =
      resolved.sourceDependency(sourceKey)?.let { relationshipParticipant(it) } ?: return null
  val target =
      resolved.sourceDependency(targetKey)?.let { relationshipParticipant(it) } ?: return null
  return Event(
      Event.Kind.CREATE,
      actorConstraint,
      NounPhrase(noun, determiner = Determiner.INDEFINITE)
          .withModifier(Modifier.Between(source, target)),
  )
}

private fun Describers.relationshipParticipant(expression: Expression): NounPhrase? {
  if (expression.refinement != null || expression.complement) return null
  val placement = positionedFrame(expression.className) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  return when {
    resolved.sourceDependencies.isEmpty() ->
        NounPhrase(placement.singular, determiner = Determiner.INDEFINITE)
    resolved.hasOnlySourceDependency(ownerKey, ownerExpression) -> oneOfYour(placement.plural)
    resolved.hasOnlySourceDependency(ownerKey, notOwnerExpression) ->
        NounPhrase(placement.singular, determiner = Determiner.OPPONENT_POSSESSIVE)
    else -> null
  }
}

internal fun Describers.renderActionUse(expression: Expression): NounPhrase? {
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.complement) return null
  val use = fact(expression.className, ComponentDescriber::actionUse) ?: return null
  val objectPhrase = NounPhrase.text(use.objectPhrase)
  val refinement = expression.refinement ?: return objectPhrase
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  val propertyMetric = minimum.metric as? Property ?: return null
  if (propertyMetric.receiver != null) return null
  val property = use.minimumProperties[propertyMetric.propertyName.value] ?: return null
  if (minimum.target == 1) {
    return objectPhrase.withModifier(
        Modifier.Relation(
            "with",
            NounPhrase("positive ${property.noun}", determiner = Determiner.INDEFINITE),
        )
    )
  }
  val unit = property.unit?.let { " $it" }.orEmpty()
  return objectPhrase.withModifier(
      Modifier.Relation(
          "with",
          NounPhrase(
              "${property.noun} of ${minimum.target}$unit or more",
              determiner = Determiner.INDEFINITE,
          ),
      )
  )
}

private fun Describers.purchaseEvent(expression: Expression): Event? {
  if (!expression.simple) return null
  val purchase = triggerFrame(expression.className) as? TriggerFrame.Purchase ?: return null
  val noun = purchase.noun.singular
  return Event(
      Event.Kind.BUY,
      Event.ActorConstraint.YOU,
      NounPhrase(noun, determiner = Determiner.INDEFINITE),
  )
}

private fun Describers.productionEvent(expression: Expression): Event? {
  val production = productionCategoryExpression(expression, this) ?: return null
  if (production.owner != null) return null
  val objectPhrase =
      if (concrete(production.resource)) {
        NounPhrase(
            "${componentNoun(production.resource, 1)} production",
            determiner = Determiner.YOUR,
        )
      } else {
        oneOfYour("productions")
      }
  return Event(
      Event.Kind.INCREASE_PRODUCTION,
      Event.ActorConstraint.YOU,
      objectPhrase,
      listOf(Modifier.Phrase("1 step")),
  )
}

internal fun Describers.playedCardEvent(expression: Expression): Event? {
  val description = triggerFrame(expression.className) as? TriggerFrame.PlayCard ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val actorConstraint =
      when {
        resolved.sourceDependencies.isEmpty() -> Event.ActorConstraint.YOU
        resolved.hasOnlySourceDependency(ownerKey, anyoneExpression) ->
            Event.ActorConstraint.UNRESTRICTED
        else -> return null
      }
  val card = componentNoun(expression.className, 1)
  val determiner =
      if (actorConstraint == Event.ActorConstraint.UNRESTRICTED) {
        Determiner.ANY
      } else {
        Determiner.INDEFINITE
      }
  val cardPhrase = NounPhrase(card, determiner = determiner)
  val objectPhrase =
      expression.refinement?.let { refinement ->
        if (refinement.forgiving) return null
        val counting = refinement.requirement as? Requirement.Counting ?: return null
        when (val metric = counting.metric) {
          is Metric.Count -> {
            val tagExpression = countedExpression(counting) ?: return null
            if (
                tagExpression.refinement != null ||
                    tagExpression.complement ||
                    resolveExpression(tagExpression)?.let { tag ->
                      tag.sourceDependencies.isNotEmpty() &&
                          !tag.hasOnlySourceDependency(ownerKey, anyoneExpression)
                    } != false
            ) {
              return null
            }
            val tag = tagName(tagExpression.className)
            if (counting is Requirement.Min && counting.target == 1 && tag != null) {
              NounPhrase(
                  "$tag $card",
                  determiner =
                      if (actorConstraint == Event.ActorConstraint.UNRESTRICTED) {
                        Determiner.ANY
                      } else {
                        Determiner.INDEFINITE
                      },
              )
            } else {
              if (
                  !isTag(tagExpression.className) ||
                      (tag == null && playedTagPhrase(tagExpression.className) != null)
              ) {
                return null
              }
              val singular = tag?.let { "$it tag" } ?: "tag"
              val plural = tag?.let { "$it tags" } ?: "tags"
              val quantity =
                  when (counting) {
                    is Requirement.Min -> "${counting.target} or more $plural"
                    is Requirement.Max ->
                        if (counting.target == 0) "no $plural"
                        else
                            "at most ${counting.target} ${if (counting.target == 1) singular else plural}"
                    is Requirement.Exact ->
                        "exactly ${counting.target} ${if (counting.target == 1) singular else plural}"
                  }
              cardPhrase.withModifier(Modifier.Relation("with", NounPhrase.text(quantity)))
            }
          }
          is Property -> {
            val minimum = counting as? Requirement.Min ?: return null
            if (metric.receiver != null) return null
            val property = description.minimumProperties[metric.propertyName.value] ?: return null
            when (property) {
              is ComponentDescriber.MinimumProperty.Threshold -> {
                val unit = property.unit?.let { " $it" }.orEmpty()
                cardPhrase.withModifier(
                    Modifier.Relation(
                        "with",
                        NounPhrase(
                            "${property.noun} of ${minimum.target}$unit or more",
                            determiner = Determiner.INDEFINITE,
                        ),
                    )
                )
              }
              is ComponentDescriber.MinimumProperty.Presence -> {
                if (minimum.target != 1) return null
                cardPhrase.withModifier(
                    Modifier.Relation(
                        "with",
                        NounPhrase(property.noun, determiner = Determiner.INDEFINITE),
                    )
                )
              }
            }
          }
          else -> return null
        }
      } ?: cardPhrase
  return Event(Event.Kind.PLAY, actorConstraint, objectPhrase)
}

private fun Describers.placementEvent(
    expression: Expression,
    actorConstraint: Event.ActorConstraint,
): Event? {
  if (expression.refinement != null || expression.complement) return null
  val resolvedPlacement = resolvePlacementExpression(expression, this) ?: return null
  if (resolvedPlacement.unknownDependencies.isNotEmpty()) return null
  if (actorConstraint == Event.ActorConstraint.YOU && resolvedPlacement.owner != null) return null
  if (
      actorConstraint == Event.ActorConstraint.UNRESTRICTED &&
          resolvedPlacement.owner != null &&
          resolvedPlacement.owner != anyoneExpression
  ) {
    return null
  }
  val placement = positionedFrame(expression.className) ?: return null
  val location =
      resolvedPlacement.sites
          .singleOrNull()
          ?.takeIf { it.simple }
          ?.let { fact(it.className, ComponentDescriber::metricLocation) }
  val complements =
      location?.let { listOf(Modifier.Phrase(it)) }
          ?: renderPlacementSites(resolvedPlacement, this)
          ?: return null
  val objectPhrase =
      when (actorConstraint) {
        Event.ActorConstraint.YOU ->
            NounPhrase(placement.singular, placement.plural, determiner = placement.determiner)
        Event.ActorConstraint.UNRESTRICTED ->
            NounPhrase(placement.singular, placement.plural, determiner = Determiner.ANY)
      }
  return Event(Event.Kind.PLACE, actorConstraint, objectPhrase, complements)
}
