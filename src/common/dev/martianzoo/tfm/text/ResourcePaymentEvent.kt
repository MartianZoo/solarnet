package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The source identified by a resource-payment event. */
internal sealed interface ResourcePaymentEvent {
  data class Standard(val resource: Expression) : ResourcePaymentEvent

  data class FromCard(val card: Expression) : ResourcePaymentEvent
}

internal fun Describers.resourcePaymentEvent(trigger: Trigger): ResourcePaymentEvent? {
  val expression = (trigger as? Trigger.OnGainOf)?.expression ?: return null
  if (expression.refinement != null) return null
  if (expression.className == CARD.declaringClass) {
    val payment = resolveExpression(expression, CARD) ?: return null
    val card = payment.sourceDependency(CARD) ?: return null
    if (!payment.hasOnlySourceDependency(CARD, card)) return null
    return ResourcePaymentEvent.FromCard(card)
  }
  if (expression.className != cn("Pay")) return null
  val payment = resolveExpression(expression) ?: return null
  if (
      payment.sourceDependencies.any { (key, value) ->
        key != RESOURCE && (key != Key(OWNED, 0) || value != ownerExpression)
      }
  )
      return null
  val resource = representedClass(expression) ?: return null
  return ResourcePaymentEvent.Standard(resource)
}

private val CARD = Key(cn("PayFromCard"), 0)
private val RESOURCE = Key(cn("Pay"), 0)
