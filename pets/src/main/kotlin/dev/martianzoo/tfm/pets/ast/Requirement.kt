package dev.martianzoo.tfm.pets.ast

import com.google.common.collect.Lists.asList
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.util.toSetStrict

sealed class Requirement : PetsNode() {

  data class Min(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression? = null, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
    init {
      if ((qe.scalar ?: 1) == 0) {
        throw PetsException("This requirement is always true (${qe})")
      }
    }
    override fun toString() = "$qe"
    override val children = setOf(qe)
  }

  data class Max(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { if(qe.scalar == null) throw PetsException("'MAX <thing>' is confusing; use 'MAX 1 <thing>'") }
    override fun toString() = "MAX $qe"
    override val children = setOf(qe)
  }

  data class Exact(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    init { if(qe.scalar == null) throw PetsException("Use '=1 <thing>', not '=<thing>'") }
    override fun toString() = "=$qe"
    override val children = setOf(qe)
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest).toSetStrict())
    init { if(requirements.size < 2) throw PetsException("$requirements") }
    override fun toString() = requirements.joinToString(" OR ") {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 3
    override val children = requirements
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest))
    init { require(requirements.size >= 2) }
    override fun toString() = requirements.joinToString() {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 1
    override val children = requirements
  }

  data class Prod(val requirement: Requirement) : Requirement(), ProductionBox<Requirement> {
    override fun toString() = "PROD[${requirement}]"
    override val children = setOf(requirement)
    override fun countProds() = super.countProds() + 1
    override fun extract() = requirement
  }

  companion object {
    fun and(requirements: List<Requirement>) =
        if (requirements.size == 1) requirements[0] else And(requirements)
    fun and(p1: Requirement, p2: Requirement, vararg rest: Requirement) =
        and(asList(p1, p2, rest))

    fun or(requirements: Set<Requirement>): Requirement {
      return if (requirements.size == 1) requirements.first() else Or(requirements)
    }
    fun or(requirements: List<Requirement>) = or(requirements.toSet()) // careful??
    fun or(p1: Requirement, p2: Requirement, vararg rest: Requirement) = or(asList(p1, p2, rest))
  }
}
