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
import kotlin.reflect.KClass

/** The main part of a transmutation instruction, without its scalar or intensity. */
public sealed class FromExpression : PetNode() {
  override val kind: KClass<out PetNode> = FromExpression::class

  public abstract val toExpression: Expression
  public abstract val fromExpression: Expression

  /** An argument retained unchanged by a compact transmutation. */
  public data class Unchanged(public val expression: Expression) : FromExpression() {
    override val toExpression: Expression by this::expression
    override val fromExpression: Expression by this::expression

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

    override fun toString(): String = "$expression"
  }

  /** A transmutation whose source and destination are both written in full. */
  public data class Full(
      override val toExpression: Expression,
      override val fromExpression: Expression,
  ) : FromExpression() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(toExpression, fromExpression)

    override fun toString(): String = "$toExpression FROM $fromExpression"
  }

  /** A same-Class transmutation with exactly one changed argument. */
  public data class Compact(
      public val className: ClassName,
      public val arguments: List<FromExpression>,
      public val refinement: Expression.Refinement? = null,
  ) : FromExpression() {
    init {
      if (arguments.count { it !is Unchanged } != 1) {
        throw PetSyntaxException("A compact transmutation must contain exactly one FROM")
      }
    }

    override val toExpression: Expression =
        className.of(arguments.map { it.toExpression }).copy(refinement = refinement)
    override val fromExpression: Expression = className.of(arguments.map { it.fromExpression })

    override fun visitChildren(visitor: Visitor): Unit =
        visitor.visit(arguments + className + listOfNotNull(refinement))

    override fun toString(): String = buildString {
      append(className).append(arguments.joinToString(", ", "<", ">"))
      refinement?.let { append("(").append(it).append(")") }
    }
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<FromExpression> {
      return parser {
        val unchanged = Expression.parser() map FromExpression::Unchanged
        val full =
            Expression.parser() and
                skip(_from) and
                Expression.parser() map
                { (to, from) ->
                  Full(to, from)
                }

        val argumentList =
            zeroOrMore(unchanged and skipChar(',')) and
                parser() and
                zeroOrMore(skipChar(',') and unchanged) map
                { (before, from, after) ->
                  before + from + after
                }
        val compact =
            ClassName.parser() and
                (skipChar('<') and argumentList and skipChar('>')) and
                optional(Expression.refinementParser()) map
                { (name, arguments, refinement) ->
                  Compact(name, arguments, refinement)
                }

        full or compact
      }
    }
  }
}
