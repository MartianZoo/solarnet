package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The source identified by a resource-payment event. */
internal sealed interface ResourcePaymentEvent {
  data class Standard(val resource: Expression) : ResourcePaymentEvent

  data class FromCard(val card: Expression) : ResourcePaymentEvent
}

internal fun Describers.resourcePaymentEvent(trigger: Trigger): ResourcePaymentEvent? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (triggerFrame(expression.className) != ComponentDescriber.TriggerFrame.SpendResource) {
    return null
  }
  resolveExpression(expression, CARD)?.let { resolved ->
    val card = resolved.sourceDependency(CARD) ?: return@let
    if (resolved.hasOnlySourceDependency(CARD, card)) {
      return ResourcePaymentEvent.FromCard(card)
    }
  }
  val resource = representedClass(expression) ?: return null
  return ResourcePaymentEvent.Standard(resource)
}

private val CARD = Key(cn("PayFromCard"), 0)
