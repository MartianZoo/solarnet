package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key

/** The payment operation identified by a Billing-family event. */
internal data class BillingEvent(
    val provider: Expression,
    val resource: Expression?,
    val card: Expression?,
)

internal fun Describers.billingEvent(trigger: Trigger): BillingEvent? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (!expressions.isBilling(expression.className)) return null
  val resolved = resolveExpression(expression) ?: return null
  val provider = resolved.dependency(PROVIDER)?.rootClass?.className?.expression ?: return null
  val resource =
      resolved.sourceDependency(RESOURCE)?.let {
        resolved.dependency(RESOURCE)?.representedClass?.className?.expression
      }
  val card =
      expression
          .takeIf { it.className == CARD_INVOICE }
          ?.let {
            val cardClass = resolved.dependency(CARD) ?: return null
            cardClass.representedClass
                ?.baseType
                ?.expression
                ?.copy(refinement = cardClass.refinement) ?: return null
          }
  return BillingEvent(provider, resource, card)
}

private val BILLING = cn("Billing")
private val CARD_INVOICE = cn("CardInvoice")
private val CARD = Key(CARD_INVOICE, 0)
private val PROVIDER = Key(BILLING, 0)
private val RESOURCE = Key(BILLING, 2)
