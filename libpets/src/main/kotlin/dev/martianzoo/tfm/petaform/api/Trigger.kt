package dev.martianzoo.tfm.petaform.api

sealed interface Trigger : PetaformObject {
  data class OnGain(val expression: Expression) : Trigger {
    override val petaform = expression.petaform
  }

  data class OnRemove(val expression: Expression) : Trigger {
    override val petaform = "-${expression.petaform}"
  }

  data class Prod(val trigger: Trigger) : Trigger {
    override val petaform = "PROD[${trigger.petaform}]"
  }
}
