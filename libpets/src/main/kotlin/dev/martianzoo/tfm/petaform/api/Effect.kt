package dev.martianzoo.tfm.petaform.api

import dev.martianzoo.tfm.petaform.api.Instruction.Gated

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetaformNode() {
  override val children = listOf(trigger, instruction)

  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "${trigger}${if (immediate) "::" else ":"} $instext"
  }

  sealed class Trigger : PetaformNode() {
    data class OnGain(val expression: Expression) : Trigger() {
      override val children = listOf(expression)
      override fun toString() = "$expression"
    }

    data class OnRemove(val expression: Expression) : Trigger() {
      override val children = listOf(expression)
      override fun toString() = "-${expression}"
    }

    data class Prod(val trigger: Trigger) : Trigger() {
      override val children = listOf(trigger)
      override fun toString() = "PROD[${trigger}]"
      override fun countProds() = super.countProds() + 1
    }

    data class Conditional(val trigger: Trigger, val predicate: Predicate) : Trigger() {
      init {
        if (trigger is Conditional) {
          throw PetaformException()
        }
      }
      override val children = listOf(trigger, predicate)
      override fun toString() = "$trigger IF $predicate"
    }
  }
}
