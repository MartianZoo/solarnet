package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
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

    public fun join(one: Requirement?, two: Requirement?): Requirement? {
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

  /** A requirement that counts (a min, max, or exact). */
  public sealed class Counting(public open val scaledEx: ScaledExpression) : Requirement() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

    public abstract val range: IntRange
  }

  public data class Min(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      Scalar.checkNonzero(scaledEx.scalar)
      if (scaledEx.scalar is XScalar) {
        throw PetSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString(): String = "$scaledEx"

    override val range: IntRange = (scaledEx.scalar as ActualScalar).value..Int.MAX_VALUE
  }

  public data class Max(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      if (scaledEx.scalar is XScalar) {
        throw PetSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString(): String = "MAX ${scaledEx.toFullString()}" // no "MAX 5" or "MAX Heat"

    override val range: IntRange = 0..(scaledEx.scalar as ActualScalar).value
  }

  public data class Exact(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      if (scaledEx.scalar is XScalar) {
        throw PetSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString(): String = "=${scaledEx.toFullString()}" // no "=5" or "=Heat"

    override val range: IntRange = (scaledEx.scalar as ActualScalar).value..scaledEx.scalar.value
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

    override fun safeToNestIn(container: PetNode): Boolean {
      return super.safeToNestIn(container) && container !is IfTrigger
    }

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
    internal constructor(
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
                scaledEx(ActualScalar(scalar ?: 1), expr)
              }
        }

        val min = scaledEx map Requirement::Min
        val max = skip(_max) and scaledEx map Requirement::Max
        val exact = skipChar('=') and scaledEx map Requirement::Exact
        val transform =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }
        transform or min or max or exact or group(parser())
      }
    }
  }
}
