package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Prod
import dev.martianzoo.tfm.pets.ast.Instruction.Remove

data class Action(val cost: Cost?, val instruction: Instruction) : PetsNode() {
  override val kind = "Action"

  override fun toString() = (cost?.let { "$cost -> " } ?: "-> ") + instruction

  sealed class Cost : PetsNode() {
    override val kind = "Cost"

    abstract fun toInstruction(): Instruction

    data class Spend(val qe: QuantifiedExpression) : Cost() {
      override fun toString() = qe.toString()

      // I believe Ants/Predators are the reasons for MANDATORY here
      override fun toInstruction() = Remove(qe, MANDATORY)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init {
        if (qe.scalar == 0) {
          throw PetsException("Can't do something 'per' a non-positive amount")
        }
        when (cost) {
          is Or, is Multi -> throw PetsException("Break into separate Per instructions")
          is Per -> throw PetsException("Might support in future?")
          else -> {}
        }
      }

      override fun toString() = "$cost / ${qe.toString(forceType = true)}" // no "/ 2" but "/ Heat" is fine
      override fun precedence() = 5

      override fun toInstruction() = Per(cost.toInstruction(), qe)
    }

    data class Or(var costs: Set<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toSet())

      override fun toString() = costs.joinToString(" OR ") { groupPartIfNeeded(it) }
      override fun precedence() = 3

      override fun toInstruction() = Instruction.Or(costs.map(Cost::toInstruction).toSet())
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toList())

      override fun toString() = costs.joinToString { groupPartIfNeeded(it) }
      override fun precedence() = 1

      override fun toInstruction() = Instruction.Multi(costs.map(Cost::toInstruction))
    }

    data class Prod(val cost: Cost) : Cost(), ProductionBox<Cost> {
      override fun toString() = "PROD[${cost}]"

      override fun toInstruction() = Prod(cost.toInstruction())

      override fun extract() = cost
    }
  }
}
