package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser

sealed class Metric : PetNode() {
  override val kind = Metric::class.simpleName!!

  override fun visitChildren(visitor: Visitor) {
    TODO("Not yet implemented")
  }

  data class Count(val scaledType: ScaledTypeExpr) : Metric() {
    init {
      if (scaledType.scalar < 1) throw PetException()
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledType)
    override fun toString() = scaledType.toString(forceScalar = false, forceType = true)
  }

  data class Max(val metric: Metric, val maximum: Int) : Metric() {
    init {
      if (metric is Max) throw PetException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric)
    override fun toString() = "$metric MAX $maximum"
  }

  companion object : PetParser() {
    fun metric(text: String): Metric = Parsing.parse(parser(), text)

    fun parser(): Parser<Metric> {
      return parser {
        val count: Parser<Count> = ScaledTypeExpr.parser() map ::Count

        val max: Parser<Max> =
            count and skip(_max) and scalar map { (met, limit) -> Max(met, limit) }

        max or count
      }
    }
  }
}
