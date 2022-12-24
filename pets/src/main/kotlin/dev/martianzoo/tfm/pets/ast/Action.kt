package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Prod
import dev.martianzoo.tfm.pets.ast.Instruction.Remove

data class Action(val cost: Cost?, val instruction: Instruction) : PetsNode() {
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
  override val children = setOfNotNull(cost) + instruction

  sealed class Cost : PetsNode() {
    abstract fun toInstruction(): Instruction

    data class Spend(val qe: QuantifiedExpression) : Cost() {
      override fun toString() = qe.toString()
      override val children = setOf(qe)
      override fun toInstruction() = Remove(qe, MANDATORY)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init {
        if ((qe.scalar ?: 1) <= 0)
          throw PetsException("Can't do something 'per' a non-positive amount")

        when (cost) {
          is Or, is Multi -> throw PetsException("Break into separate Per instructions")
          is Per -> throw PetsException("Might support in future?")
          else -> {}
        }
      }
      override fun toString() = "$cost / $qe" // parens
      override fun precedence() = 5
      override val children = setOf(cost, qe)
      override fun toInstruction() = Per(cost.toInstruction(), qe)
    }

    data class Or(var costs: Set<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toSet())
      override fun toString() = costs.map(::groupIfNeeded).joinToString(" OR ")
      override fun precedence() = 3
      override val children = costs
      override fun toInstruction() = Instruction.Or(costs.map(Cost::toInstruction).toSet())
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toList())
      override fun toString() = costs.map(::groupIfNeeded).joinToString()
      override fun precedence() = 1
      override val children = costs
      override fun toInstruction() = Instruction.Multi(costs.map(Cost::toInstruction))
    }

    data class Prod(val cost: Cost) : Cost(), ProductionBox<Cost> {
      override fun toString() = "PROD[${cost}]"
      override val children = setOf(cost)
      override fun toInstruction() = Prod(cost.toInstruction())
      override fun extract() = cost
    }
  }
}
