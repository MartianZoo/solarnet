package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The action provider and optional slot identified by an action-use event. */
internal data class ActionUseEvent(
    val provider: Expression,
    val slot: Expression?,
)

internal fun Describers.actionUseEvent(trigger: Trigger): ActionUseEvent? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null) return null
  if (triggerFrame(expression.className) != ComponentDescriber.TriggerFrame.UseAction) return null
  val providerKey = Key(expression.className, 0)
  val slotKey = Key(expression.className, 1)
  val resolved = resolveExpression(expression, providerKey) ?: return null
  val provider = resolved.sourceDependency(providerKey) ?: return null
  if (resolved.sourceDependencies.keys.any { it != providerKey && it != slotKey }) return null
  return ActionUseEvent(provider, resolved.sourceDependency(slotKey))
}
