package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.UserException.PetSyntaxException
import dev.martianzoo.tfm.pets.PetTokenizer

/**
 * A way of computing a non-negative integer based on a game state. Metrics appear after a slash in
 * instructions, and also belong to `Award`s.
 */
sealed class Metric : PetElement() {
  override val kind = Metric::class.simpleName!!

  data class Count(val expression: Expression) : Metric() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(expression)
    override fun toString() = "$expression"
    override fun precedence() = 12
  }

  data class Scaled(val unit: Int, val metric: Metric) : Metric() {
    init {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = if (unit == 1) "$metric" else "$unit ${groupPartIfNeeded(metric)}"
    override fun precedence() = 11
  }

  data class Max(val metric: Metric, val maximum: Int) : Metric() {
    init {
      if (metric is Max) throw PetSyntaxException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = "${groupPartIfNeeded(metric)} MAX $maximum"
    override fun precedence() = 10
  }

  data class Plus(val metrics: List<Metric>) : Metric() {
    init {
      if (metrics.any { it is Plus }) {
        // how did we get around this problem for other things??
        throw PetSyntaxException("Having a plus inside a plus causes problems...")
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

  data class Transform(val metric: Metric, override val transformKind: String) :
      Metric(), TransformNode<Metric> {
    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = "$transformKind[$metric]"
    override fun extract() = metric
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Metric> {
      return parser {
        val count: Parser<Count> = Expression.parser() map ::Count

        val transform: Parser<Metric> =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }

        val atom: Parser<Metric> = transform or count or group(parser())

        val scaled: Parser<Metric> =
            optional(rawScalar) and atom map { (scal, met) -> scal?.let { Scaled(it, met) } ?: met }

        val max: Parser<Metric> =
            scaled and
                optional(skip(_max) and rawScalar) map
                { (met, limit) ->
                  limit?.let { Max(met, it) } ?: met
                }

        val result =
            max and
                zeroOrMore(skipChar('+') and max) map
                { (met, addon) ->
                  if (addon.any()) Plus(listOf(met) + addon) else met
                }
        result
      }
    }
  }
}
