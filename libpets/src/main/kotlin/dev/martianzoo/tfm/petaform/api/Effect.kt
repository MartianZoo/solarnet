package dev.martianzoo.tfm.petaform.api

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetaformNode() {
  override val children = listOf(trigger, instruction)
  override fun toString() = "${trigger}${if (immediate) "::" else ":"} ${instruction}"
  override val hasProd = hasZeroOrOneProd(trigger, instruction)

  sealed class Trigger : PetaformNode() {
    data class OnGain(val expression: Expression) : Trigger() {
      override val children = listOf(expression)
      override fun toString() = "$expression"
      override val hasProd = false
    }

    data class OnRemove(val expression: Expression) : Trigger() {
      override val children = listOf(expression)
      override fun toString() = "-${expression}"
      override val hasProd = false
    }

    data class Prod(val trigger: Trigger) : Trigger() {
      init { require(!trigger.hasProd) }
      override val children = listOf(trigger)
      override fun toString() = "PROD[${trigger}]"
      override val hasProd = true
    }
  }
}
