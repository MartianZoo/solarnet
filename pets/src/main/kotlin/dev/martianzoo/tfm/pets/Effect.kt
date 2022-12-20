package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.Instruction.Gated

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetsNode() {
  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "${trigger}${if (immediate) "::" else ":"} $instext"
  }
  override val children = setOf(trigger, instruction)

  sealed class Trigger : PetsNode() {
    data class OnGain(val expression: TypeExpression) : Trigger() {
      override fun toString() = "$expression"
      override val children = setOf(expression)
    }

    data class OnRemove(val expression: TypeExpression) : Trigger() {
      override fun toString() = "-${expression}"
      override val children = setOf(expression)
    }

    data class Conditional(val trigger: Trigger, val requirement: Requirement) : Trigger() {
      init { if (trigger is Conditional) throw PetsException("And the conditions together instead") }
      override fun toString() = "$trigger IF $requirement"
      override val children = setOf(trigger, requirement)
    }

    data class Now(val requirement: Requirement) : Trigger() {
      override fun toString() = "NOW $requirement"
      override val children = setOf(requirement)
    }

    data class Prod(val trigger: Trigger) : Trigger(), ProductionBox<Trigger> {
      init {
        if (trigger !is OnGain && trigger !is OnRemove) {
          throw PetsException("only gain/remove trigger can go in prod block")
        }
      }
      override fun toString() = "PROD[${trigger}]"
      override val children = setOf(trigger)
      override fun countProds() = super.countProds() + 1
      override fun extract() = trigger
    }
  }
}
