package dev.martianzoo.tfm.petaform.api

sealed class Trigger : PetaformObject() {
  data class OnGain(val expression: Expression) : Trigger() {
    override fun toString() = "$expression"
  }

  data class OnRemove(val expression: Expression) : Trigger() {
    override fun toString() = "-${expression}"
  }

  data class Prod(val trigger: Trigger) : Trigger() {
    override fun toString() = "PROD[${trigger}]"
  }
}
