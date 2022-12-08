package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.PetaformParser.QEs.scalar

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformNode() {
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
  override val children = listOfNotNull(cost) + instruction

  sealed class Cost : PetaformNode() {
    data class Spend(val qe: QuantifiedExpression) : Cost() {
      constructor(expr: TypeExpression?, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
      init {
        if ((scalar ?: 1) == 0) throw PetaformException()
      }
      override fun toString() = qe.toString()
      override val children = listOf(qe)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init {
        if ((qe.scalar ?: 1) <= 0)
          throw PetaformException()
        if (qe.typeExpression == null)
          throw PetaformException()

        when (cost) {
          is Or, is Multi, is Per -> throw PetaformException()
          else -> {}
        }
      }
      override fun toString() = "$cost / $qe" // parens
      override fun precedence() = 5
      override val children = listOf(cost, qe)
    }

    data class Or(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString(" OR ") {
        it.toStringWithin(this)
      }
      override fun precedence() = 3
      override val children = costs
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override fun toString() = costs.joinToString() {
        it.toStringWithin(this)
      }
      override fun precedence() = 1
      override val children = costs
    }

    data class Prod(val cost: Cost) : Cost() {
      override fun toString() = "PROD[${cost}]"
      override val children = listOf(cost)
      override fun countProds() = super.countProds() + 1
    }

    companion object {
      fun and(vararg costs: Cost) = and(costs.toList())
      fun and(costs: List<Cost>): Cost = if (costs.size == 1) {
        costs[0]
      } else {
        Multi(
            costs.flatMap {
              if (it is Multi) it.costs else listOf(it)
            }
        )
      }

      fun or(vararg costs: Cost) = or(costs.toList())
      fun or(costs: List<Cost>) = if (costs.size == 1) {
        costs[0]
      } else {
        Or(
            costs.flatMap {
              if (it is Or) it.costs else listOf(it)
            }
        )
      }
    }
  }
}
