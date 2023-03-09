package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.ChangeEvent
import dev.martianzoo.tfm.data.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction

data class ActiveEffect(
    val contextComponent: Component,
    val trigger: ActiveTrigger,
    val automatic: Boolean,
    val instruction: Instruction
) {
  companion object {
    fun from(it: Effect, contextComponent: Component) =
        ActiveEffect(contextComponent, ActiveTrigger.from(it.trigger), it.automatic, it.instruction)
  }

  fun onChangeToSelf(triggerEvent: ChangeEvent, game: Game): FiredEffect? {
    val hit = trigger.matchSelf(triggerEvent, game) ?: return null
    val alteredInstr = hit.fixer(instruction)
    val newCause = Cause(triggerEvent.ordinal, contextComponent.expressionFull, doer = null)
    return FiredEffect(alteredInstr * hit.count, newCause, automatic)
  }

  fun onChangeToOther(triggerEvent: ChangeEvent, game: Game): FiredEffect? {
    val hit = trigger.matchOther(triggerEvent, game) ?: return null
    val alteredInstr = hit.fixer(instruction)
    val newCause = Cause(triggerEvent.ordinal, contextComponent.expressionFull, doer = null)
    return FiredEffect(alteredInstr * hit.count, newCause, automatic)
  }

  data class FiredEffect(val instruction: Instruction, val cause: Cause, val automatic: Boolean) {
    fun withDoer(doer: ClassName) = copy(cause = cause.copy(doer = doer))
  }
}
