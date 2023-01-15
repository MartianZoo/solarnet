package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.pets.PetParser

sealed class Requirement : PetNode() {
  abstract fun evaluate(game: GameState): Boolean

  data class Min(val sat: ScalarAndType) : Requirement() {
    override fun toString() = "$sat"

    override fun evaluate(game: GameState) = game.count(sat.type) >= sat.scalar
  }

  data class Max(val sat: ScalarAndType) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "MAX ${sat.toString(true, true)}" // no "MAX 5" or "MAX Heat"

    override fun evaluate(game: GameState) = game.count(sat.type) <= sat.scalar
  }

  data class Exact(val sat: ScalarAndType) : Requirement() {
    // could remove this but make it parseable
    override fun toString() = "=${sat.toString(true, true)}" // no "=5" or "=Heat"

    override fun evaluate(game: GameState) = game.count(sat.type) == sat.scalar
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    override fun toString() = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun precedence() = 3

    override fun evaluate(game: GameState) = requirements.any { it.evaluate(game) }
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    override fun toString() = requirements.joinToString { groupPartIfNeeded(it) }
    override fun precedence() = 1

    override fun evaluate(game: GameState) = requirements.all { it.evaluate(game) }
  }

  data class Transform(val requirement: Requirement, override val transform: String) :
      Requirement(), GenericTransform<Requirement> {
    override fun toString() = "$transform[${requirement}]"
    override fun extract() = requirement

    override fun evaluate(game: GameState) = error("shoulda been transformed by now")
  }

  override val kind = "Requirement"

  companion object : PetParser() {
    internal fun atomParser(): Parser<Requirement> { //
      return parser {
        val sat = ScalarAndType.parser()
        val min = sat map ::Min
        val max = skip(_max) and sat map ::Max
        val exact = skipChar('=') and sat map ::Exact
        val transform = transform(parser()) map { (node, type) ->
          Transform(node, type)
        }
        min or max or exact or transform or group(parser())
      }
    }

    internal fun parser(): Parser<Requirement> {
      return parser {
        val orReq = separatedTerms(atomParser(), _or) map {
          val set = it.toSet()
          if (set.size == 1) set.first() else Or(set)
        }
        commaSeparated(orReq) map {
          if (it.size == 1) it.first() else And(it)
        }
      }
    }
  }
}
