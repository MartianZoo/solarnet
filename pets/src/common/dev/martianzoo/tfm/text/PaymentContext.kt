package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The subject, denomination, and operation identified by a payment scope. */
internal data class PaymentContext(
    val provider: Expression?,
    val resource: Expression?,
    val operation: Expression?,
    val phase: Phase,
) {
  enum class Phase {
    STARTED,
    COMPLETED,
  }
}

internal fun Describers.paymentContext(trigger: Trigger): PaymentContext? {
  val expression =
      when (trigger) {
        is OnGainOf -> trigger.expression
        is OnRemoveOf -> trigger.expression
        else -> return null
      }
  if (expression.refinement != null) return null
  if (!expressions.isPaymentScope(expression.className)) return null
  val resolved = resolveExpression(expression, PROVIDER) ?: return null
  val provider =
      resolved.dependency(PROVIDER)?.let {
        val sourceExpression = resolved.sourceDependency(PROVIDER)
        val source = sourceExpression?.let(::resolveExpression)
        if (sourceExpression == Expression(CLASS, listOf(thisExpression))) return@let thisExpression
        it.representedClass
            ?.className
            ?.expression
            ?.copy(
                typeVariableName = source?.sourceDependency(Key(CLASS, 0))?.typeVariableName,
                refinement = sourceExpression?.refinement ?: it.refinement,
            )
            ?.takeUnless { provider ->
              provider.className == COMPONENT && provider.refinement == null
            }
      }
  val resource =
      resolved.sourceDependency(RESOURCE)?.let {
        resolved.dependency(RESOURCE)?.representedClass?.className?.expression
      }
  val operation =
      resolved.sourceDependency(OPERATION)?.let {
        resolved.dependency(OPERATION)?.representedClass?.className?.expression
      }
  val phase =
      if (trigger is OnRemoveOf) PaymentContext.Phase.COMPLETED else PaymentContext.Phase.STARTED
  return PaymentContext(provider, resource, operation, phase)
}

private val PAYMENT_SCOPE = cn("PaymentScope")
private val PROVIDER = Key(PAYMENT_SCOPE, 0)
private val OPERATION = Key(PAYMENT_SCOPE, 2)
private val RESOURCE = Key(PAYMENT_SCOPE, 1)
