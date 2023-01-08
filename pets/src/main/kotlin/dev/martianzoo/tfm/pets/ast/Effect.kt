package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.util.iff

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val automatic: Boolean,
) : PetsNode() {

  override val kind = "Effect"

  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "$trigger:${iff(automatic, ":")} $instext"
  }

  sealed class Trigger : PetsNode() {
    override val kind = "Trigger"

    data class OnGain(val expression: TypeExpression) : Trigger() {
      override fun toString() = "$expression"
    }

    data class OnRemove(val expression: TypeExpression) : Trigger() {
      override fun toString() = "-${expression}"
    }

    data class Prod(val trigger: Trigger) : Trigger(), ProductionBox<Trigger> {
      init {
        if (trigger !is OnGain && trigger !is OnRemove) {
          throw PetsException("only gain/remove trigger can go in prod block")
        }
      }

      override fun toString() = "PROD[${trigger}]"

      override fun extract() = trigger
    }
  }
}
