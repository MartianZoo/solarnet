package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The payment operation identified by a Billing-family event. */
internal data class BillingEvent(
    val provider: Expression?,
    val resource: Expression?,
    val phase: Phase,
) {
  enum class Phase {
    STARTED,
    COMPLETED,
  }
}

internal fun Describers.billingEvent(trigger: Trigger): BillingEvent? {
  val expression =
      when (trigger) {
        is OnGainOf -> trigger.expression
        is OnRemoveOf -> trigger.expression
        else -> return null
      }
  if (expression.refinement != null) return null
  if (!expressions.isBilling(expression.className)) return null
  val resolved = resolveExpression(expression) ?: return null
  if (expression.className != BILLING && resolved.dependency(PROVIDER) == null) return null
  val provider =
      resolved.dependency(PROVIDER)?.let {
        it.rootClass.className.expression.copy(refinement = it.refinement)
      }
  val resource =
      resolved.sourceDependency(RESOURCE)?.let {
        resolved.dependency(RESOURCE)?.representedClass?.className?.expression
      }
  val phase =
      if (trigger is OnRemoveOf) BillingEvent.Phase.COMPLETED else BillingEvent.Phase.STARTED
  return BillingEvent(provider, resource, phase)
}

private val BILLING = cn("Billing")
private val PROVIDER = Key(cn("ActionBilling"), 0)
private val RESOURCE = Key(BILLING, 0)
