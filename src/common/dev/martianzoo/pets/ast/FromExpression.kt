package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import kotlin.reflect.KClass

/**
 * The main part of a transmutation instruction, without its scalar or quantifier — the `Foo FROM
 * Bar` of
 * [rule L6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions).
 */
public sealed class FromExpression : PetNode() {
  override val kind: KClass<out PetNode> = FromExpression::class

  /** What the components become. */
  public abstract val toExpression: Expression

  /** What the components were. */
  public abstract val fromExpression: Expression

  /** An argument retained unchanged by a compact transmutation. */
  public data class Unchanged(public val expression: Expression) : FromExpression() {
    override val toExpression: Expression
      get() = expression

    override val fromExpression: Expression
      get() = expression

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

  /**
   * A same-Class transmutation with exactly one changed argument — the compact spelling of
   * [rule L6-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions),
   * where `Foo<Same, Here, To FROM From>` means `Foo<Same, Here, To> FROM Foo<Same, Here, From>`.
   * The unchanged arguments occupy both roles.
   */
  public data class Compact(
      /** The class shared by both sides. */
      public val className: ClassName,

      /** The shared argument list, of which exactly one argument may change. */
      public val arguments: List<FromExpression>,

      /** A refinement carried by [toExpression] only. */
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

    override fun visitChildren(visitor: Visitor) {
      visitor.visit(arguments)
      visitor.visit(className)
      visitor.visit(refinement)
    }

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
