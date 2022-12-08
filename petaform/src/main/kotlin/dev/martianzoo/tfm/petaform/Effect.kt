package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.Instruction.Gated

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetaformNode() {
  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "${trigger}${if (immediate) "::" else ":"} $instext"
  }
  override val children = listOf(trigger, instruction)

  sealed class Trigger : PetaformNode() {
    data class OnGain(val expression: TypeExpression) : Trigger() {
      override fun toString() = "$expression"
      override val children = listOf(expression)
    }

    data class OnRemove(val expression: TypeExpression) : Trigger() {
      override fun toString() = "-${expression}"
      override val children = listOf(expression)
    }

    data class Conditional(val trigger: Trigger, val predicate: Predicate) : Trigger() {
      init { if (trigger is Conditional) throw PetaformException("And the conditions together instead") }
      override fun toString() = "$trigger IF $predicate"
      override val children = listOf(trigger, predicate)
    }

    data class Prod(val trigger: Trigger) : Trigger() {
      override fun toString() = "PROD[${trigger}]"
      override val children = listOf(trigger)
      override fun countProds() = super.countProds() + 1
    }
  }
}
