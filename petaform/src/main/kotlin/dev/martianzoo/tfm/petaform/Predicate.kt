package dev.martianzoo.tfm.petaform

import com.google.common.collect.Lists
import dev.martianzoo.util.toSetCareful
import dev.martianzoo.util.toSetCarefulP

sealed class Predicate : PetaformNode() {

  data class Min(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression? = null, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
    init {
      if ((qe.scalar ?: 1) == 0) {
        throw PetaformException("This predicate is always true (${qe})")
      }
    }
    override fun toString() = "$qe"
    override val children = setOf(qe)
  }

  data class Max(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { if(qe.scalar == null) throw PetaformException("'MAX <thing>' is confusing; use 'MAX 1 <thing>'") }
    override fun toString() = "MAX $qe"
    override val children = setOf(qe)
  }

  data class Exact(val qe: QuantifiedExpression) : Predicate() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { if(qe.scalar == null) throw PetaformException("Use '=1 <thing>', not '=<thing>'") }
    override fun toString() = "=$qe"
    override val children = setOf(qe)
  }

  data class Or(val predicates: Set<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest).toSetCareful())
    init { if(predicates.size < 2) throw PetaformException("$predicates") }
    override fun toString() = predicates.joinToString(" OR ") {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 3
    override val children = predicates
  }

  data class And(val predicates: List<Predicate>) : Predicate() {
    constructor(pred1: Predicate, pred2: Predicate, vararg rest: Predicate) :
        this(Lists.asList(pred1, pred2, rest))
    init { require(predicates.size >= 2) }
    override fun toString() = predicates.joinToString() {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 1
    override val children = predicates
  }

  data class Prod(val predicate: Predicate) : Predicate() {
    override fun toString() = "PROD[${predicate}]"
    override val children = setOf(predicate)
    override fun countProds() = super.countProds() + 1
  }

  companion object {
    fun and(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates[0] else And(predicates)
    fun and(p1: Predicate, p2: Predicate, vararg rest: Predicate) =
        and(Lists.asList(p1, p2, rest))

    // TODO why is deduping effing up
    fun or(predicates: Set<Predicate>) = or(predicates.toList())
    fun or(predicates: List<Predicate>) =
        if (predicates.size == 1) predicates.first() else Or(predicates.toSet())
    fun or(p1: Predicate, p2: Predicate, vararg rest: Predicate) =
        or(Lists.asList(p1, p2, rest).toSetCarefulP())
  }
}
