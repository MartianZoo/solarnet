package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.GameApi

sealed class Requirement : PetsNode() {
  abstract fun evaluate(game: GameApi): Boolean

  data class Min(val qe: QuantifiedExpression) : Requirement() {
    override fun toString() = "$qe"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) = game.count(qe.type) >= qe.scalar
  }

  data class Max(val qe: QuantifiedExpression) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "MAX ${qe.toString(true, true)}" // no "MAX 5" or "MAX Heat"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) = game.count(qe.type) <= qe.scalar
  }

  data class Exact(val qe: QuantifiedExpression) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "=${qe.toString(true, true)}" // no "=5" or "=Heat"
    override val children = setOf(qe)

    override fun evaluate(game: GameApi) = game.count(qe.type) == qe.scalar
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    override fun toString() = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun precedence() = 3
    override val children = requirements

    override fun evaluate(game: GameApi) =
        requirements.any { it.evaluate(game) }
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
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
