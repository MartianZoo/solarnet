package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.api.UserException.PetsSyntaxException
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.util.Reifiable

data class ScaledExpression
constructor(
    val scalar: Scalar,
    val expression: Expression = MEGACREDIT,
) : PetNode() {
  public companion object {
    public fun scaledEx(scalar: Scalar, expression: Expression? = null) =
      ScaledExpression(scalar, expression ?: MEGACREDIT)
    public fun scaledEx(value: Int? = null, expression: Expression? = null) =
      scaledEx(ActualScalar(value ?: 1), expression)

    public fun scaledEx(scalar: Scalar, hasEx: HasExpression) = scaledEx(scalar, hasEx.expression)

    public fun scaledEx(value: Int? = null, hasEx: HasExpression) = scaledEx(value, hasEx.expression)

    internal fun scalar(): Parser<Scalar> = Parsers.scalar()
    internal fun parser(): Parser<ScaledExpression> = Parsers.parser()

    internal val MEGACREDIT = ClassName.cn("Megacredit").expression
  }

  override fun visitChildren(visitor: Visitor) = visitor.visit(scalar, expression)

  override fun toString() = toString(forceScalar = false, forceExpression = false)
  fun toFullString() = toString(forceScalar = true, forceExpression = true)

  operator fun times(multiple: Int) = copy(scalar = scalar * multiple)

  fun toString(forceScalar: Boolean = false, forceExpression: Boolean = false) =
      when {
        !forceExpression && expression == MEGACREDIT -> "$scalar"
        !forceScalar && scalar == ActualScalar(1) -> "$expression"
        else -> "$scalar $expression"
      }

  override val kind = ScaledExpression::class.simpleName!!

  sealed class Scalar : PetNode(), Reifiable<Scalar> {
    override val kind = "Scalar"

    override fun visitChildren(visitor: Visitor) {}
    abstract operator fun times(multiple: Int): Scalar

    companion object {
      fun checkNonzero(s: Scalar) {
        if (s == ActualScalar(0)) throw PetsSyntaxException("Can't do zero")
      }
    }

    data class ActualScalar(val value: Int) : Scalar() {
      init {
        require(value >= 0)
      }

      override val abstract = false

      override fun times(multiple: Int) = copy(value = value * multiple)

      override fun ensureNarrows(that: Scalar) {
        when {
          that is XScalar && (value % that.multiple != 0) ->
              throw InvalidReificationException("$value isn't a multiple of ${that.multiple}")
          that is ActualScalar && value != that.value ->
              throw InvalidReificationException("can't change value")
        }
      }

      override fun toString() = "$value"
    }

    data class XScalar(val multiple: Int) : Scalar() {
      init {
        require(multiple > 0)
      }

      override val abstract = true

      override fun ensureNarrows(that: Scalar) {
        if (this != that) throw InvalidReificationException("")
      }

      override fun times(multiple: Int) = copy(multiple = this.multiple * multiple)

      override fun toString() = if (multiple == 1) "X" else "${multiple}X"
    }
  }

  private object Parsers : PetTokenizer() {
    fun scalar(): Parser<Scalar> {
      val actual: Parser<ActualScalar> = rawScalar map ::ActualScalar
      val xScalar: Parser<XScalar> = optional(rawScalar) and skip(_x) map { XScalar(it ?: 1) }
      return xScalar or actual
    }

    fun parser(): Parser<ScaledExpression> {
      return parser {
        val scalarAndOptionalEx = scalar() and optional(Expression.parser())
        val optionalScalarAndEx = optional(scalar()) and Expression.parser()

        scalarAndOptionalEx or optionalScalarAndEx map { (scalar, expr) ->
          scaledEx(scalar ?: ActualScalar(1), expr)
        }
      }
    }
  }
}
