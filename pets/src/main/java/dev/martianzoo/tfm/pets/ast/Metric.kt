package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.BaseTokenizer
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException

sealed class Metric : PetNode() {
  override val kind = Metric::class.simpleName!!

  override fun visitChildren(visitor: Visitor) {
    TODO("Not yet implemented")
  }

  data class Count(val expression: Expression) : Metric() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(expression)
    override fun toString() = "$expression"
    override fun precedence() = 12
  }

  data class Scaled(val unit: Int, val metric: Metric) : Metric() {
    init {
      if (unit < 1) throw PetException()
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = if (unit == 1) "$metric" else "$unit ${groupPartIfNeeded(metric)}"
    override fun precedence() = 11
  }

  data class Max(val metric: Metric, val maximum: Int) : Metric() {
    init {
      if (metric is Max) throw PetException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = "${groupPartIfNeeded(metric)} MAX $maximum"
    override fun precedence() = 10
  }

  data class Plus(val metrics: List<Metric>) : Metric() {
    init {
      if (metrics.any { it is Plus }) {
        // TODO how did we get around this problem for other things??
        throw PetException("Having a plus inside a plus causes problems...")
      }
    }
    companion object {
      fun create(metrics: List<Metric>): Metric? {
        return when (metrics.size) {
          0 -> null
          1 -> metrics.single()
          else -> Plus(metrics.flatMap { if (it is Plus) it.metrics else listOf(it) })
        }
      }
      fun create(first: Metric, vararg rest: Metric) =
          if (rest.none()) first else create(listOf(first) + rest)
    }

    init {
      require(metrics.size > 1)
    }
    override fun visitChildren(visitor: Visitor) = visitor.visit(metrics)
    override fun toString() = metrics.joinToString(" + ")
    override fun precedence() = 9
  }

  companion object : BaseTokenizer() {
    fun metric(text: String): Metric = Parsing.parse(parser(), text)

    fun parser(): Parser<Metric> {
      return parser {
        val count: Parser<Count> = Expression.parser() map ::Count
        val atom: Parser<Metric> = count or group(parser())

        val scaled: Parser<Metric> =
            optional(scalar) and
                atom map
                { (scal, met) ->
                  if (scal == null) met else Scaled(scal, met)
                }

        val max: Parser<Metric> =
            scaled and
                optional(skip(_max) and scalar) map
                { (met, limit) ->
                  if (limit == null) met else Max(met, limit)
                }

        max and
            zeroOrMore(skipChar('+') and parser()) map { (met, addon) ->
              Plus.create(listOf(met) + addon)!!
            }
      }
    }
  }
}
