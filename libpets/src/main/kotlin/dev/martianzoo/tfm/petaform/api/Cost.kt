package dev.martianzoo.tfm.petaform.api

sealed interface Cost : PetaformObject {
  val hasProd : Boolean

  data class Spend(val qe: QuantifiedExpression) : Cost {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val petaform = qe.petaform
    override val hasProd = false
  }

  data class Multi(var costs: List<Cost>) : Cost {
    init { require(costs.size >= 2) }
    override val petaform = costs.joinToString { it.petaform }
    override val hasProd = costs.any { it.hasProd }
  }

  data class Or(var costs: List<Cost>) : Cost {
    init { require(costs.size >= 2) }
    override val petaform = costs.joinToString(" OR ") {
      // precedence is against us ...
      if (it is Multi) "(${it.petaform})" else it.petaform
    }
    override val hasProd = costs.any { it.hasProd }
  }

  data class Prod(val cost: Cost) : Cost {
    init {
      require(!cost.hasProd)
    }
    override val petaform = "PROD[${cost.petaform}]"
    override val hasProd = true
  }

  // can't do non-prod per prod yet
  data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost {
    init { require(qe.scalar != 0) }

    override val petaform = "$cost / $qe" // parens
    override val hasProd = cost.hasProd
  }

  companion object {
    fun and(vararg costs: Cost) = and(costs.toList())
    fun and(costs: List<Cost>): Cost =
        if (costs.size == 1) {
          costs[0]
        } else {
          Multi(costs.flatMap {
            if (it is Multi) it.costs else listOf(it)
          })
        }

    fun or(vararg costs: Cost) = or(costs.toList())
    fun or(costs: List<Cost>) =
        if (costs.size == 1) {
          costs[0]
        } else {
          Or(costs.flatMap {
            if (it is Or) it.costs else listOf(it)
          })
        }
  }
}
