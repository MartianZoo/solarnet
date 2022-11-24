package dev.martianzoo.tfm.petaform.api

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformObject {
  override val petaform = (cost?.let { "${cost.petaform} -> " } ?: "-> ") + instruction.petaform
}
