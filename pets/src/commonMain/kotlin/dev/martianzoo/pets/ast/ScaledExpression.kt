package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/** The combination of a positive integer (or `X`) with an [Expression]. */
@ConsistentCopyVisibility
public data class ScaledExpression
private constructor(
    val expression: Expression,
    val scalar: Scalar,
) : PetNode() {
  public companion object {
    // Identity, rather than the name, makes this parse-only marker impossible to author.
    private val denominationlessClass = cn("Denominationless")
    private val denominationlessExpression = denominationlessClass.expression
    private const val denominationlessAmountMessage =
        "Denominationless money amounts are no longer supported; write MC explicitly"

    /** Returns [expression] scaled by [scalar]. */
    public fun scaledEx(expression: HasExpression, scalar: Scalar): ScaledExpression =
        ScaledExpression(expression.expression, scalar)

    /** Returns [expression] scaled by [count]. */
    public fun scaledEx(expression: HasExpression, count: Int = 1): ScaledExpression =
        scaledEx(expression, ActualScalar(count))

    internal fun scalar(): Parser<Scalar> = Parsers.scalar()

    internal fun parser(): Parser<ScaledExpression> = Parsers.parser()

    internal fun denominationless(scalar: Scalar): ScaledExpression =
        ScaledExpression(denominationlessExpression, scalar)

    internal fun rejectIfDenominationless(expression: Expression) {
      if (expression.className === denominationlessClass) {
        throw PetSyntaxException(denominationlessAmountMessage)
      }
    }
  }

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scalar, expression)

  override fun toString(): String = toString(forceScalar = false)

  internal fun toFullString() = toString(forceScalar = true)

  internal operator fun times(multiple: Int) = copy(scalar = scalar * multiple)

  private fun toString(forceScalar: Boolean = false) =
      when {
        !forceScalar && scalar == ActualScalar(1) -> "$expression"
        else -> "$scalar $expression"
      }

  override val kind: kotlin.reflect.KClass<out PetNode> = ScaledExpression::class

  public sealed class Scalar : PetNode(), Specification<Scalar> {
    override val kind: kotlin.reflect.KClass<out PetNode> = Scalar::class

    override fun visitChildren(visitor: Visitor): Unit = Unit

    internal abstract operator fun times(multiple: Int): Scalar

    override fun isAbstract(info: TypeInfo): Boolean = abstract

    public abstract val abstract: Boolean

    /** Replaces an authored X with [value], retaining its written coefficient. */
    internal fun bindX(value: Int): Scalar =
        when (this) {
          is ActualScalar -> this
          is XScalar -> ActualScalar(value * multiple)
        }

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

    public data class XScalar public constructor(val multiple: Int) : Scalar() {
      init {
        require(multiple > 0)
      }

      override val abstract: Boolean = true

      override fun ensureNarrows(that: Scalar, info: TypeInfo) {
        if (this != that) throw NarrowingException("$this / $that")
      }

      override fun times(multiple: Int) = copy(multiple = this.multiple * multiple)

      override fun toString(): String = if (multiple == 1) "X" else "${multiple}X"
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
        val scalarAndOptionalExpression = scalar() and optional(Expression.parser())
        val optionalScalarAndExpression = optional(scalar()) and Expression.parser()

        scalarAndOptionalExpression or
            optionalScalarAndExpression map
            { (scalar, expression) ->
              val resolvedScalar = scalar ?: ActualScalar(1)
              if (expression == null) denominationless(resolvedScalar)
              else scaledEx(expression, resolvedScalar)
            }
      }
    }
  }
}
