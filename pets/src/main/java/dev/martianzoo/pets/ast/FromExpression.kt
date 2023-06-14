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
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

/**
 * The main part of a "transmute" instruction, without the scalar or intensity. Examples: `Foo<Bar>
 * FROM Foo<Qux>`, and (equivalent) `Foo<Bar FROM Qux>`.
 */
public sealed class FromExpression : PetNode() {
  override val kind = FromExpression::class

  abstract val toExpression: Expression
  abstract val fromExpression: Expression

  data class ExpressionAsFrom(val expression: Expression) : FromExpression() {
    override val toExpression by this::expression
    override val fromExpression by this::expression

    override fun visitChildren(visitor: Visitor) = visitor.visit(expression)
    override fun toString() = "$expression"
  }

  data class SimpleFrom(
      override val toExpression: Expression,
      override val fromExpression: Expression,
  ) : FromExpression() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(toExpression, fromExpression)
    override fun toString() = "$toExpression FROM $fromExpression"
  }

  data class ComplexFrom(
      val className: ClassName,
      val arguments: List<FromExpression> = listOf(),
      val refinement: Requirement? = null,
  ) : FromExpression() {
    init {
      if (arguments.count { it is SimpleFrom || it is ComplexFrom } != 1) {
        throw PetSyntaxException("Can only have one FROM in an expression")
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments + className + refinement)

    override val toExpression = className.of(arguments.map { it.toExpression })
    override val fromExpression = className.of(arguments.map { it.fromExpression }).has(refinement)

    override fun toString() =
        "$className${arguments.joinOrEmpty(wrap = "<>")}${refinement.wrap("(HAS ", ")")}"
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<FromExpression> {
      return parser {
        val expressionAsFrom = Expression.parser() map FromExpression::ExpressionAsFrom
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
        val refinement = group(skip(_has) and Requirement.parser())
        val complexFrom =
            ClassName.parser() and
            arguments and
            optional(refinement) map { (name, args, refs) ->
              ComplexFrom(name, args, refs)
            }

        simpleFrom or complexFrom
      }
    }
  }
}
