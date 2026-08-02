package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.PetTokenizer

/**
 * A way of computing a non-negative integer based on a world. Metrics appear after a slash in
 * instructions, and also belong to `Award`s.
 */
public sealed class Metric : PetElement() {
  public companion object {
    /** Returns [inner] scaled by [unit], omitting the meaningless wrapper when [unit] is one. */
    public fun scaled(unit: Int, inner: Metric): Metric {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
      return if (unit == 1) inner else Scaled(unit, inner)
    }

    internal fun parser(): Parser<Metric> = Parsers.parser()

    internal fun atomParser(): Parser<Metric> = Parsers.atomParser()
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Metric::class

  public data class Count(val expression: Expression) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

    override fun toString(): String = "$expression"

    override fun precedence(): Int = 12
  }

  /** Counts one unit for each complete group of [unit] counted by [inner]. */
  @ConsistentCopyVisibility
  public data class Scaled internal constructor(val unit: Int, val inner: Metric) : Metric() {
    init {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$unit ${groupPartIfNeeded(inner)}"

    override fun precedence(): Int = 11
  }

  public data class Max(val inner: Metric, val maximum: Int) : Metric() {
    init {
      if (inner is Max) throw PetSyntaxException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "${groupPartIfNeeded(inner)} MAX $maximum"

    override fun precedence(): Int = 10
  }

  @ConsistentCopyVisibility
  public data class Or internal constructor(val metrics: List<Metric>) : Metric() {
    init {
      if (metrics.any { it is Or }) {
        throw PetSyntaxException("Nested metric OR must be flattened")
      }
    }

    public companion object {
      public fun create(metrics: List<Metric>): Metric? {
        return when (metrics.size) {
          0 -> null
          1 -> metrics.single()
          else -> Or(metrics.flatMap { if (it is Or) it.metrics else listOf(it) }.toList())
        }
      }

      internal fun create(first: Metric, vararg rest: Metric) =
          if (rest.none()) first else create(listOf(first) + rest)
    }

    init {
      require(metrics.size > 1)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metrics)

    override fun toString(): String = metrics.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun precedence(): Int = 4
  }

  public data class Transform(val inner: Metric, override val transformKind: String) :
      Metric(), TransformNode<Metric> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$transformKind[$inner]"

    override fun extract(): Metric = inner
  }

  private object Parsers : PetTokenizer() {
    fun parser(): Parser<Metric> {
      return parser {
        val atom = atomParser()
        atom and
            zeroOrMore(skip(_or) and atom) map
            { (met, addon) ->
              if (addon.any()) Or.create(listOf(met) + addon)!! else met
            }
      }
    }

    /** A metric suitable for being nested directly after `/` in an instruction or cost. */
    fun atomParser(): Parser<Metric> {
      return parser {
        val count: Parser<Count> = Expression.parser() map Metric::Count

        val transform: Parser<Metric> =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }

        val atom: Parser<Metric> = transform or count or group(parser())

        val scaled: Parser<Metric> =
            optional(rawScalar) and atom map { (scal, met) -> scal?.let { scaled(it, met) } ?: met }

        val max: Parser<Metric> =
            scaled and
                optional(skip(_max) and rawScalar) map
                { (met, limit) ->
                  limit?.let { Max(met, it) } ?: met
                }

        max
      }
    }
  }
}
