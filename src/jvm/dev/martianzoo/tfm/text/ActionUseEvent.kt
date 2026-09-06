package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

/** The action provider and optional slot identified by a UseAction event. */
internal data class ActionUseEvent(
    val provider: Expression,
    val slot: Expression?,
)

internal fun Describers.actionUseEvent(trigger: Trigger): ActionUseEvent? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (triggerFrame(expression.className) != ComponentDescriber.TriggerFrame.UseAction) return null
  val resolved = resolveExpression(expression, PROVIDER) ?: return null
  val provider = resolved.sourceDependency(PROVIDER) ?: return null
  if (resolved.sourceDependencies.keys.any { it != PROVIDER && it != SLOT }) return null
  return ActionUseEvent(provider, resolved.sourceDependency(SLOT))
}

private val USE_ACTION = cn("UseAction")
private val PROVIDER = Key(USE_ACTION, 0)
private val SLOT = Key(USE_ACTION, 1)
