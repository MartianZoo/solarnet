package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Prod
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.toSetCarefulP

data class Action(val cost: Cost?, val instruction: Instruction) : PetsNode() {
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
  override val children = setOfNotNull(cost) + instruction

  sealed class Cost : PetsNode() {
    abstract fun toInstruction(): Instruction

    data class Spend(val qe: QuantifiedExpression) : Cost() {
      constructor(expr: TypeExpression?, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
      init {
        if ((qe.scalar ?: 1) == 0)
          throw PetsException("Cannot spend zero (omit the cost instead)")
      }
      override fun toString() = qe.toString()
      override val children = setOf(qe)
      override fun toInstruction() = Remove(qe, MANDATORY)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init {
        if ((qe.scalar ?: 1) <= 0)
          throw PetsException("Can't do something 'per' a non-positive amount")
        if (qe.typeExpression == null)
          throw PetsException("Write '/ 2 Megacredit', not just '/ 2'")

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
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString(" OR ") {
        it.toStringWhenInside(this)
      }
      override fun precedence() = 3
      override val children = costs
      override fun toInstruction() = Instruction.or(costs.map(Cost::toInstruction))
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString() {
        it.toStringWhenInside(this)
      }
      override fun precedence() = 1
      override val children = costs
      override fun toInstruction() = Instruction.multi(costs.map(Cost::toInstruction))
    }

    data class Prod(val cost: Cost) : Cost(), ProductionBox<Cost> {
      override fun toString() = "PROD[${cost}]"
      override val children = setOf(cost)
      override fun countProds() = super.countProds() + 1
      override fun toInstruction() = Prod(cost.toInstruction())
      override fun extract() = cost
    }

    companion object {
      fun and(vararg costs: Cost) = and(costs.toList())
      fun and(costs: List<Cost>): Cost = if (costs.size == 1) {
        costs.first()
      } else {
        Multi(costs.flatMap { if (it is Multi) it.costs else listOf(it) })
      }

      fun or(costs: List<Cost>): Cost = or(costs.toSetCarefulP())
      fun or(vararg costs: Cost): Cost = or(costs.toList())
      fun or(costs: Set<Cost>) = if (costs.size == 1) {
        costs.first()
      } else {
        Or(costs.flatMap { if (it is Or) it.costs else setOf(it) }.toSetCarefulP())
      }
    }
  }
}
