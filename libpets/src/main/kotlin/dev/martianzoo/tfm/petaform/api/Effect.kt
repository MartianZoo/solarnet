package dev.martianzoo.tfm.petaform.api

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val immediate: Boolean = false,
) : PetaformObject() {
  override fun toString() = "${trigger}${if (immediate) "::" else ":"} ${instruction}"
}
