package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.api.GameApi

sealed class Requirement : PetsNode() {
  abstract fun evaluate(game: GameApi): Boolean

  data class Min(val qe: QuantifiedExpression) : Requirement() {
    override fun toString() = "$qe"

    override fun evaluate(game: GameApi) = game.count(qe.expression) >= qe.scalar
  }

  data class Max(val qe: QuantifiedExpression) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "MAX ${qe.toString(true, true)}" // no "MAX 5" or "MAX Heat"

    override fun evaluate(game: GameApi) = game.count(qe.expression) <= qe.scalar
  }

  data class Exact(val qe: QuantifiedExpression) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "=${qe.toString(true, true)}" // no "=5" or "=Heat"

    override fun evaluate(game: GameApi) = game.count(qe.expression) == qe.scalar
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    override fun toString() = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun precedence() = 3

    override fun evaluate(game: GameApi) = requirements.any { it.evaluate(game) }
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    override fun toString() = requirements.joinToString { groupPartIfNeeded(it) }
    override fun precedence() = 1

    override fun evaluate(game: GameApi) = requirements.all { it.evaluate(game) }
  }

  data class Transform(val requirement: Requirement, override val transform: String) :
      Requirement(), GenericTransform<Requirement> {
    override fun toString() = "$transform[${requirement}]"
    override fun extract() = requirement

    override fun evaluate(game: GameApi) = error("shoulda been transformed by now")
  }

  override val kind = "Requirement"
}
