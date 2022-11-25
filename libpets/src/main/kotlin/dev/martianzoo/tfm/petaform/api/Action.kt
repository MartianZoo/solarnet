package dev.martianzoo.tfm.petaform.api

import java.util.*
import kotlin.jvm.optionals.toList
import kotlin.jvm.optionals.toSet

data class Action(val cost: Cost?, val instruction: Instruction) : PetaformObject() {
  override val children = listOfNotNull(cost) + instruction
  override fun toString() = (cost?.let { "${cost} -> " } ?: "-> ") + instruction
  override val hasProd = hasZeroOrOneProd(cost, instruction)

  sealed class Cost : PetaformObject() {
    data class Spend(val qe: QuantifiedExpression) : Cost() {
      constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
      override val children = listOf(qe)
      override fun toString() = qe.toString()
      override val hasProd = false
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override val children = costs
      override fun toString() = costs.joinToString()
      override val hasProd = hasZeroOrOneProd(costs)
    }

    data class Or(var costs: List<Cost>) : Cost() {
      init { require(costs.size >= 2) }
      override val children = costs
      override fun toString() = costs.joinToString(" OR ") {
        // precedence is against us ...
        if (it is Multi) "(${it})" else "$it"
      }
      override val hasProd = hasZeroOrOneProd(costs)
    }

    data class Prod(val cost: Cost) : Cost() {
      override val children = listOf(cost)
      init { require(!cost.hasProd) }
      override fun toString() = "PROD[${cost}]"
      override val hasProd = true
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val qe: QuantifiedExpression) : Cost() {
      init { require(qe.scalar != 0) }
      override val children = listOf(cost, qe)
      override fun toString() = "$cost / $qe" // parens
      override val hasProd = cost.hasProd
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
