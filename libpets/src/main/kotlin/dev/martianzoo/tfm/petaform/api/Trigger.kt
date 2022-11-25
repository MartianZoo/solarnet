package dev.martianzoo.tfm.petaform.api

sealed class Trigger : PetaformObject() {
  data class OnGain(val expression: Expression) : Trigger() {
    override fun toString() = "$expression"
    override val hasProd = false
  }

  data class OnRemove(val expression: Expression) : Trigger() {
    override fun toString() = "-${expression}"
    override val hasProd = false
  }

  data class Prod(val trigger: Trigger) : Trigger() {
    init { require(!trigger.hasProd) }
    override fun toString() = "PROD[${trigger}]"
    override val hasProd = true
  }
}
