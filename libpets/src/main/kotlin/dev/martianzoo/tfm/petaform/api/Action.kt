package dev.martianzoo.tfm.petaform.api

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformNode() {
  override val children = listOfNotNull(cost) + instruction
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction

  sealed class Cost : PetaformNode() {
    data class Spend(val qe: QuantifiedExpression) : Cost() {
      constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
      override val children = listOf(qe)
      override fun toString() = qe.toString()
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override val children = costs
      override fun toString() = costs.joinToString()
    }

    data class Or(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override val children = costs
      override fun toString() = costs.joinToString(" OR ") {
        // precedence is against us ...
        if (it is Multi) "(${it})" else "$it"
      }
    }

    data class Prod(val cost: Cost) : Cost(), ProdBox {
      override val children = listOf(cost)
      override fun toString() = "PROD[${cost}]"
      override fun countProds() = super.countProds() + 1
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init { require(qe.scalar != 0) }
      override val children = listOf(cost, qe)
      override fun toString() = "$cost / $qe" // parens
    }
  }

  companion object {
    fun and(vararg costs: Cost) = and(costs.toList())
    fun and(costs: List<Cost>): Cost =
        if (costs.size == 1) {
          costs[0]
        } else {
          Cost.Multi(costs.flatMap {
            if (it is Cost.Multi) it.costs else listOf(it)
          })
        }

    fun or(vararg costs: Cost) = or(costs.toList())
    fun or(costs: List<Cost>) =
        if (costs.size == 1) {
          costs[0]
        } else {
          Cost.Or(costs.flatMap {
            if (it is Cost.Or) it.costs else listOf(it)
          })
        }
  }
}
