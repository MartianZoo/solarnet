package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

/** The source identified by a resource-payment event. */
internal sealed interface ResourcePaymentEvent {
  data class Standard(val resource: Expression) : ResourcePaymentEvent

  data class FromCard(val card: Expression, val resource: ClassName) : ResourcePaymentEvent
}

internal fun Describers.resourcePaymentEvent(trigger: Trigger): ResourcePaymentEvent? {
  val conditioned = trigger as? Trigger.IfTrigger ?: return null
  val attributed = conditioned.inner as? Trigger.ByTrigger ?: return null
  if (!expressions.isOwner(attributed.by)) return null
  val expression = (attributed.inner as? Trigger.OnRemoveOf)?.expression ?: return null
  if (expression.refinement != null) return null
  val condition = conditioned.condition as? Requirement.Min ?: return null
  if (condition.minimum != 1) return null
  val acceptance = (condition.countedMetric as? Metric.Count)?.expression ?: return null
  if (acceptance.refinement != null) return null
  if (acceptance.className == CARD.declaringClass) {
    val accepted = resolveExpression(acceptance, CARD) ?: return null
    val card = accepted.sourceDependency(CARD) ?: return null
    if (!accepted.hasOnlySourceDependency(CARD, card)) return null
    val spent = resolveCardResource(expression) ?: return null
    if (!cardResourceHasHolder(spent, card)) return null
    return ResourcePaymentEvent.FromCard(card, expression.className)
  }
  if (acceptance.className != cn("Accepting")) return null
  val resource = representedClass(acceptance) ?: return null
  val accepted = resolveExpression(acceptance) ?: return null
  if (accepted.sourceDependencies.keys.any { it != Key(cn("Accepting"), 0) }) return null
  if (expression.className != resource.className) return null
  val spent = resolveExpression(expression) ?: return null
  if (
      spent.sourceDependencies.any { (key, value) ->
        key != Key(OWNED, 0) || value != ownerExpression
      }
  ) {
    return null
  }
  return ResourcePaymentEvent.Standard(resource)
}

private val CARD = Key(cn("AcceptingFromCard"), 0)
