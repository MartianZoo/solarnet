package dev.martianzoo.tfm.petaform

import dev.martianzoo.util.toSetCarefulP

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformNode() {
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
  override val children = setOfNotNull(cost) + instruction

  sealed class Cost : PetaformNode() {
    data class Spend(val qe: QuantifiedExpression) : Cost() {
      constructor(expr: TypeExpression?, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
      init {
        if ((qe.scalar ?: 1) == 0)
          throw PetaformException("Cannot spend zero (omit the cost instead)")
      }
      override fun toString() = qe.toString()
      override val children = setOf(qe)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init {
        if ((qe.scalar ?: 1) <= 0)
          throw PetaformException("Can't do something 'per' a non-positive amount")
        if (qe.typeExpression == null)
          throw PetaformException("Write '/ 2 Megacredit', not just '/ 2'")

        when (cost) {
          is Or, is Multi -> throw PetaformException("Break into separate Per instructions")
          is Per -> throw PetaformException("Might support in future?")
          else -> {}
        }
      }
      override fun toString() = "$cost / $qe" // parens
      override fun precedence() = 5
      override val children = setOf(cost, qe)
    }

    data class Or(var costs: Set<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString(" OR ") {
        it.toStringWhenInside(this)
      }
      override fun precedence() = 3
      override val children = costs
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString() {
        it.toStringWhenInside(this)
      }
      override fun precedence() = 1
      override val children = costs
    }

    data class Prod(val cost: Cost) : Cost() {
      override fun toString() = "PROD[${cost}]"
      override val children = setOf(cost)
      override fun countProds() = super.countProds() + 1
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
