package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/**
 * Expresses a condition which is deterministically either true or false in any particular game
 * state, for example, `MAX 4 OxygenStep`.
 */
public sealed class Requirement : PetElement() {
  public companion object {
    public fun split(requirement: Iterable<Requirement>): List<Requirement> = requirement.flatMap {
      split(it)
    }

    /** Recursively breaks apart any [And] requirements. */
    public fun split(requirement: Requirement): List<Requirement> =
        if (requirement is And) {
          split(requirement.requirements)
        } else {
          listOf(requirement)
        }

    internal fun join(one: Requirement?, two: Requirement?): Requirement? {
      val x = setOfNotNull(one, two)
      return when (x.size) {
        0 -> null
        1 -> x.first()
        else -> And(x.toList())
      }
    }

    internal fun parser(): Parser<Requirement> = Parsers.parser()

    internal fun atomParser(): Parser<Requirement> = Parsers.atomParser()
  }

  override fun safeToNestIn(container: PetNode): Boolean =
      super.safeToNestIn(container) || container is IfTrigger

  /** Evaluates this requirement using [count] for each metric it needs. */
  public fun isMetBy(count: (Metric) -> Int): Boolean =
      when (this) {
        is Counting -> {
          val actual = count(metric)
          when (this) {
            is Min -> actual >= target
            is Max -> actual <= target
            is Exact -> actual == target
          }
        }
        is Or -> requirements.any { it.isMetBy(count) }
        is And -> requirements.all { it.isMetBy(count) }
        is Eval -> error("requirement property evaluation was not expanded: $this")
        is Transform -> throw ExpressionException("unhandled requirement transform: $this")
      }

  /** Includes a concrete Requirement property's syntax in the surrounding class effect. */
  public data class Eval(val property: Property) : Requirement() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(property)

    override fun toString(): String = "EVAL $property"

    override fun precedence(): Int = 12
  }

  /**
   * A requirement comparing [target] with [metric]. The target is independent of any unit scaling
   * inside the metric.
   */
  public sealed class Counting(
      public val target: Int,
      public val metric: Metric,
  ) : Requirement() {
    init {
      require(target >= 0)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metric)

    public abstract val range: IntRange

    protected fun countingString(prefix: String = "", fullSimpleMetric: Boolean = false): String {
      val countedMetric = metric
      val counted =
          when (countedMetric) {
            is Metric.Count ->
                scaledEx(countedMetric.expression, ActualScalar(target)).let {
                  if (fullSimpleMetric) it.toFullString() else it.toString()
                }
            is Property ->
                if (prefix.isEmpty() && target == 1) "$countedMetric" else "$target $countedMetric"
            is Metric.Transform -> "$target $countedMetric"
            else -> "$target ($countedMetric)"
          }
      return prefix + counted
    }
  }

  public data class Min(private val minimum: Int, private val countedMetric: Metric) :
      Counting(minimum, countedMetric) {
    public constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    init {
      Scalar.checkNonzero(ActualScalar(target))
    }

    override fun toString(): String = countingString()

    override val range: IntRange = target..Int.MAX_VALUE
  }

  public data class Max(val maximum: Int, val countedMetric: Metric) :
      Counting(maximum, countedMetric) {
    internal constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    override fun toString(): String = countingString("MAX ", fullSimpleMetric = true)

    override val range: IntRange = 0..target
  }

  public data class Exact(private val expected: Int, private val countedMetric: Metric) :
      Counting(expected, countedMetric) {
    internal constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    override fun toString(): String = countingString("=", fullSimpleMetric = true)

    override val range: IntRange = target..target
  }

  @ConsistentCopyVisibility
  public data class Or internal constructor(val requirements: Set<Requirement>) : Requirement() {
    internal constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement,
    ) : this(setOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirements)

    override fun toString(): String = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun precedence(): Int = 3

    public companion object {
      public fun create(requirements: Collection<Requirement>): Requirement {
        require(requirements.isNotEmpty())
        val distinct = requirements.toSet()
        return if (distinct.size == 1) distinct.single() else Or(distinct)
      }
    }
  }

  @ConsistentCopyVisibility
  public data class And internal constructor(val requirements: List<Requirement>) : Requirement() {
    private constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement,
    ) : this(listOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirements)

    override fun toString(): String = requirements.joinToString { groupPartIfNeeded(it) }

    override fun precedence(): Int = 1

    override fun safeToNestIn(container: PetNode): Boolean {
      return super.safeToNestIn(container) && container !is IfTrigger
    }

    public companion object {
      public fun create(requirements: Collection<Requirement>): Requirement {
        require(requirements.isNotEmpty())
        return if (requirements.size == 1) requirements.single() else And(requirements.toList())
      }
    }
  }

  public data class Transform(val requirement: Requirement, override val transformKind: String) :
      Requirement(), TransformNode<Requirement> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirement)

    override fun toString(): String = "$transformKind[$requirement]"

    override fun extract(): Requirement = requirement
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Requirement::class

  private object Parsers : PetTokenizer() {
    fun parser(): Parser<Requirement> {
      return parser {
        val orReq =
            separatedTerms(atomParser(), _or) map
                {
                  val set = it.toSet()
                  Or.create(set)
                }

        commaSeparated(orReq) map And.Companion::create
      }
    }

    /**
     * A requirement suitable for being nested directly in something else. Used by gated
     * instructions and conditional triggers.
     */
    fun atomParser(): Parser<Requirement> {
      return parser {
        val scaledEx = parser {
          val scalarAndOptionalEx = rawScalar and optional(Expression.parser())
          val optionalScalarAndEx = optional(rawScalar) and Expression.parser()

          scalarAndOptionalEx or
              optionalScalarAndEx map
              { (scalar, expr) ->
                scaledEx(expr, ActualScalar(scalar ?: 1))
              }
        }

        val countedMetric = rawScalar and Metric.atomParser()

        val propertyMin: Parser<Requirement> = Property.parser() map { Min(1, it) }
        val min =
            propertyMin or
                (countedMetric map { (target, metric) -> Min(target, metric) }) or
                (scaledEx map Requirement::Min)
        val max =
            skip(_max) and
                ((countedMetric map { (target, metric) -> Max(target, metric) }) or
                    (scaledEx map Requirement::Max))
        val exact =
            skipChar('=') and
                ((countedMetric map { (target, metric) -> Exact(target, metric) }) or
                    (scaledEx map Requirement::Exact))
        val transform =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }
        val eval: Parser<Requirement> = skip(_eval) and Property.parser() map ::Eval
        eval or transform or min or max or exact or group(parser())
      }
    }
  }
}

private fun ScaledExpression.actualScalar(): Int =
    when (val scalar = scalar) {
      is ActualScalar -> scalar.value
      is XScalar -> throw PetSyntaxException("can't use X in requirements (yet?)")
    }
