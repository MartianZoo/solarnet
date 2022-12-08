package dev.martianzoo.tfm.petaform

import com.google.common.collect.Lists

sealed class Predicate : PetaformNode() {

  data class Min(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override fun toString() = "$qe"
    override val children = listOf(qe)
  }

  data class Max(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override fun toString() = "MAX ${qe.petaform(forceScalar = true)}"
    override val children = listOf(qe)
  }

  data class Exact(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override fun toString() = "=${qe.petaform(forceScalar = true)}"
    override val children = listOf(qe)
  }

  data class Or(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override fun toString() = predicates.joinToString(" OR ") {
      it.toStringWithin(this)
    }
    override fun precedence() = 3
    override val children = predicates
  }

  data class And(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override fun toString() = predicates.joinToString() {
      it.toStringWithin(this)
    }
    override fun precedence() = 1
    override val children = predicates
  }

  data class Prod(val predicate: Predicate) : Predicate() {
    override fun toString() = "PROD[${predicate}]"
    override val children = listOf(predicate)
    override fun countProds() = super.countProds() + 1
  }

  companion object {
    fun and(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else And(predicates)

    fun or(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else Or(predicates)
  }
}
