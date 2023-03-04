package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

public sealed class From : PetNode() {
  override val kind = From::class.simpleName!!

  abstract val toExpression: Expression
  abstract val fromExpression: Expression

  data class ExpressionAsFrom(val expression: Expression) : From() {
    override val toExpression by this::expression
    override val fromExpression by this::expression

    override fun visitChildren(visitor: Visitor) = visitor.visit(expression)
    override fun toString() = "$expression"
  }

  data class SimpleFrom(
      override val toExpression: Expression,
      override val fromExpression: Expression,
  ) : From() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(toExpression, fromExpression)
    override fun toString() = "$toExpression FROM $fromExpression"
  }

  data class ComplexFrom(
      val className: ClassName,
      val arguments: List<From> = listOf(),
      val refinement: Requirement? = null,
  ) : From() {
    init {
      if (arguments.count { it is SimpleFrom || it is ComplexFrom } != 1) {
        throw PetException("Can only have one FROM in an expression")
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments + className + refinement)

    override val toExpression = className.addArgs(arguments.map { it.toExpression })
    override val fromExpression = className.addArgs(arguments.map { it.fromExpression }).refine(refinement)

    override fun toString() =
        "$className${arguments.joinOrEmpty(wrap = "<>")}${refinement.wrap("(HAS ", ")")}"
  }

  companion object : PetParser() {
    fun from(text: String): From = Parsing.parse(parser(), text)

    internal fun parser(): Parser<From> {
      return parser {
        val expressionAsFrom = Expression.parser() map ::ExpressionAsFrom
        val simpleFrom =
            Expression.parser() and
            skip(_from) and
            Expression.parser() map { (to, from) -> SimpleFrom(to, from) }

        val argumentList =
            zeroOrMore(expressionAsFrom and skipChar(',')) and
            parser() and
            zeroOrMore(skipChar(',') and expressionAsFrom) map { (before, from, after) ->
              before + from + after
            }
        val arguments = skipChar('<') and argumentList and skipChar('>')
        val complexFrom =
            className and
            arguments and
            optional(Expression.refinement()) map { (name, args, refs) ->
              ComplexFrom(name, args, refs)
            }

        simpleFrom or complexFrom
      }
    }
  }
}
