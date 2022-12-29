package dev.martianzoo.tfm.pets.ast

import com.google.common.collect.Lists.asList
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.util.toSetStrict

sealed class Requirement : PetsNode() {
  data class Min(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression? = null, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
    override fun toString() = "$qe"
    override val children = setOf(qe)
  }

  data class Max(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    // could remove this but make it parseable
    init { if(qe.scalar == null) throw PetsException("use 'MAX 1 ${qe.type}'") }
    override fun toString() = "MAX $qe"
    override val children = setOf(qe)
  }

  data class Exact(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    // could remove this but make it parseable
    init { if(qe.scalar == null) throw PetsException("Use '=1 ${qe.type}'") }
    override fun toString() = "=$qe"
    override val children = setOf(qe)
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest).toSetStrict())
    override fun toString() = requirements.map(::groupIfNeeded).joinToString(" OR ")
    override fun precedence() = 3
    override val children = requirements
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest))
    override fun toString() = requirements.map(::groupIfNeeded).joinToString()
    override fun precedence() = 1
    override val children = requirements
  }

  data class Prod(val requirement: Requirement) : Requirement(), ProductionBox<Requirement> {
    override fun toString() = "PROD[${requirement}]"
    override val children = setOf(requirement)
    override fun extract() = requirement
  }

  override val kind = "Requirement"
}
