package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.Instruction.Gated

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetsNode() {

  override val kind = "Effect"

  override val children = setOf(trigger, instruction)

  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "$trigger${if (immediate) "::" else ":"} $instext"
  }

  sealed class Trigger : PetsNode() {
    override val kind = "Trigger"

    data class OnGain(val expression: TypeExpression) : Trigger() {
      override val children = setOf(expression)
      override fun toString() = "$expression"
    }

    data class OnRemove(val expression: TypeExpression) : Trigger() {
      override val children = setOf(expression)
      override fun toString() = "-${expression}"
    }

    data class Prod(val trigger: Trigger) : Trigger(), ProductionBox<Trigger> {
      init {
        if (trigger !is OnGain && trigger !is OnRemove) {
          throw PetsException("only gain/remove trigger can go in prod block")
        }
      }
      override val children = setOf(trigger)
      override fun toString() = "PROD[${trigger}]"

      override fun extract() = trigger
    }
  }
}
