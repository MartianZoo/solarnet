package dev.martianzoo.tfm.petaform.api

import com.google.common.collect.Lists

sealed class Predicate : PetaformNode() {

  data class Min(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override val children = listOf(qe)
    override fun toString() = "$qe"
    override val hasProd = false
  }

  data class Max(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override val children = listOf(qe)
    override fun toString() = "MAX ${qe.petaform(forceScalar = true)}"
    override val hasProd = false
  }

  data class Or(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override val children = predicates
    override fun toString() = predicates.joinToString(" OR ") {
      // precedence is against us
      if (it is And) "(${it})" else "$it"
    }
    override val hasProd = hasZeroOrOneProd(predicates)
  }

  data class And(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override val children = predicates
    override fun toString() = predicates.joinToString()
    override val hasProd = hasZeroOrOneProd(predicates)
  }

  data class Prod(val predicate: Predicate) : Predicate() {
    init { require(!predicate.hasProd) }
    override val children = listOf(predicate)
    override fun toString() = "PROD[${predicate}]"
    override val hasProd = true
  }

  companion object {
    fun and(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else And(predicates)

    fun or(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else Or(predicates)
  }
}
