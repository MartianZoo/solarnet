package dev.martianzoo.tfm.petaform.api

import com.google.common.collect.Lists

sealed class Predicate : PetaformNode() {

  data class Min(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override val children = listOf(qe)
    override fun toString() = "$qe"
  }

  data class Max(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override val children = listOf(qe)
    override fun toString() = "MAX ${qe.petaform(forceScalar = true)}"
  }

  data class Exact(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: Expression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { require(qe.scalar >= 0) }
    override val children = listOf(qe)
    override fun toString() = "=${qe.petaform(forceScalar = true)}"
  }

  data class Or(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override val children = predicates
    override fun toString() = predicates.joinToString(" OR ") {
      it.toStringWithin(this)
    }
    override fun precedence() = 3
  }

  data class And(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override val children = predicates
    override fun toString() = predicates.joinToString() {
      it.toStringWithin(this)
    }
    override fun precedence() = 1
  }

  data class Prod(val predicate: Predicate) : Predicate(), ProdBox {
    override val children = listOf(predicate)
    override fun toString() = "PROD[${predicate}]"
    override fun countProds() = super.countProds() + 1
  }

  companion object {
    fun and(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else And(predicates)

    fun or(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else Or(predicates)
  }

  open fun precedence(): Int = Int.MAX_VALUE
  fun toStringWithin(container: Predicate) = if (groupWithin(container)) "(${this})" else "$this"
  open fun groupWithin(container: Predicate) = precedence() <= container.precedence()
}
