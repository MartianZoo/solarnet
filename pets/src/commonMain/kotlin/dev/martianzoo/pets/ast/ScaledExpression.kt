package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.util.Reifiable

/** The combination of a positive integer (or `X`) with an [Expression]. */
@ConsistentCopyVisibility
public data class ScaledExpression
internal constructor(
    val expression: Expression = MEGACREDIT.of(),
    val scalar: Scalar,
) : PetNode() {
  public companion object {
    /** Returns [expression] scaled by [scalar], defaulting the expression to `Megacredit`. */
    public fun scaledEx(expression: HasExpression? = null, scalar: Scalar): ScaledExpression =
        ScaledExpression(expression?.expression ?: MEGACREDIT.of(), scalar)

    /** Returns [expression] scaled by [count], defaulting to one `Megacredit`. */
    public fun scaledEx(expression: HasExpression? = null, count: Int = 1): ScaledExpression =
        scaledEx(expression, ActualScalar(count))

    internal fun scalar(): Parser<Scalar> = Parsers.scalar()

    internal fun parser(): Parser<ScaledExpression> = Parsers.parser()
  }

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scalar, expression)

  override fun toString(): String = toString(forceScalar = false, forceExpression = false)

  internal fun toFullString() = toString(forceScalar = true, forceExpression = true)

  internal operator fun times(multiple: Int) = copy(scalar = scalar * multiple)

  internal fun toString(forceScalar: Boolean = false, forceExpression: Boolean = false) =
      when {
        !forceExpression && expression == MEGACREDIT.of() -> "$scalar"
        !forceScalar && scalar == ActualScalar(1) -> "$expression"
        else -> "$scalar $expression"
      }

  override val kind: kotlin.reflect.KClass<out PetNode> = ScaledExpression::class

  public sealed class Scalar : PetNode(), Reifiable<Scalar> {
    override val kind: kotlin.reflect.KClass<out PetNode> = Scalar::class

    override fun visitChildren(visitor: Visitor): Unit = Unit

    internal abstract operator fun times(multiple: Int): Scalar

    internal companion object {
      internal fun checkNonzero(s: Scalar) {
        if (s == ActualScalar(0)) throw PetSyntaxException("Can't do zero")
      }
    }

    public data class ActualScalar(val value: Int) : Scalar() {
      init {
        require(value >= 0)
      }

      override val abstract: Boolean = false

      override fun times(multiple: Int) = copy(value = value * multiple)

      override fun ensureNarrows(that: Scalar, info: TypeInfo) {
        when {
          that is XScalar && (value % that.multiple != 0) ->
              throw NarrowingException("$value isn't a multiple of ${that.multiple}")
          that is ActualScalar && value != that.value ->
              throw NarrowingException("can't change value from ${that.value} to $value")
        }
      }

      override fun toString(): String = "$value"
    }

    internal data class XScalar(val multiple: Int) : Scalar() {
      init {
        require(multiple > 0)
      }

      override val abstract = true

      override fun ensureNarrows(that: Scalar, info: TypeInfo) {
        if (this != that) throw NarrowingException("$this / $that")
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

        scalarAndOptionalEx or
            optionalScalarAndEx map
            { (scalar, expr) ->
              scaledEx(expr, scalar ?: ActualScalar(1))
            }
      }
    }
  }
}
