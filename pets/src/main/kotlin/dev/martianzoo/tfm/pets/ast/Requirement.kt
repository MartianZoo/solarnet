package dev.martianzoo.tfm.pets.ast

import com.google.common.collect.Lists.asList
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.util.toSetStrict

sealed class Requirement : PetsNode() {
  abstract fun evaluate(game: GameApi): Boolean

  data class Min(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression? = null, scalar: Int? = null) : this(QuantifiedExpression(expr, scalar))
    override fun toString() = "$qe"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) =
        game.count(qe.type!!) >= qe.scalar!!
  }

  data class Max(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    // could remove this but make it parseable
    init { if(qe.scalar == null) throw PetsException("use 'MAX 1 ${qe.type}'") }
    override fun toString() = "MAX $qe"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) =
        game.count(qe.type!!) <= qe.scalar!!
  }

  data class Exact(val qe: QuantifiedExpression) : Requirement() {
    constructor(expr: TypeExpression, scalar: Int) : this(QuantifiedExpression(expr, scalar))
    // could remove this but make it parseable
    init { if(qe.scalar == null) throw PetsException("Use '=1 ${qe.type}'") }
    override fun toString() = "=$qe"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) =
        game.count(qe.type!!) == qe.scalar!!
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest).toSetStrict())
    override fun toString() = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun precedence() = 3
    override val children = requirements

    override fun evaluate(game: GameApi) =
        requirements.any { it.evaluate(game) }
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    constructor(reqt1: Requirement, reqt2: Requirement, vararg rest: Requirement) :
        this(asList(reqt1, reqt2, rest))
    override fun toString() = requirements.joinToString { groupPartIfNeeded(it) }
    override fun precedence() = 1
    override val children = requirements

    override fun evaluate(game: GameApi) =
        requirements.all { it.evaluate(game) }
  }

  data class Prod(val requirement: Requirement) : Requirement(), ProductionBox<Requirement> {
    override fun toString() = "PROD[${requirement}]"
    override val children = setOf(requirement)
    override fun extract() = requirement

    override fun evaluate(game: GameApi) = error("shoulda been deprodified by now")
  }

  override val kind = "Requirement"
}
