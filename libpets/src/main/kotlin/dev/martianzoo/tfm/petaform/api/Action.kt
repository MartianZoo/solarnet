package dev.martianzoo.tfm.petaform.api

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformObject() {
  override val hasProd = hasZeroOrOneProd(cost, instruction)

  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
}
