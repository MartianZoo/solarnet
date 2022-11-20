package dev.martianzoo.tfm.petaform.api

import com.google.common.collect.Lists

sealed interface Predicate : PetaformObject {

  data class MinPredicate(val qe: QuantifiedExpression) : Predicate {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val asSource = qe.asSource
  }

  data class MaxPredicate(val qe: QuantifiedExpression) : Predicate {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val asSource = qe.asSourceWithMandatoryScalar()
  }

  data class OrPredicate(val predicates: List<Predicate>) : Predicate {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override val asSource = predicates.joinToString(" OR ") { it.asSource }
  }

  data class AndPredicate(val predicates: List<Predicate>) : Predicate {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }

    override val asSource = predicates.joinToString { it.asSource }
  }

  companion object {
    fun and(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else AndPredicate(predicates)

    fun or(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else OrPredicate(predicates)
  }
}
