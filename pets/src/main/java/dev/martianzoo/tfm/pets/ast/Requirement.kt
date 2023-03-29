package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.UserException.PetsSyntaxException
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar

sealed class Requirement : PetElement() {
  override fun safeToNestIn(container: PetNode) =
      super.safeToNestIn(container) || container is IfTrigger

  sealed class Counting(open val scaledEx: ScaledExpression) : Requirement() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)

    abstract val range: IntRange
  }

  data class Min(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      Scalar.checkNonzero(scaledEx.scalar)
      if (scaledEx.scalar is XScalar) {
        throw PetsSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString() = "$scaledEx"

    override val range = (scaledEx.scalar as ActualScalar).value..Int.MAX_VALUE
  }

  data class Max(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      if (scaledEx.scalar is XScalar) {
        throw PetsSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString() = "MAX ${scaledEx.toFullString()}" // no "MAX 5" or "MAX Heat"

    override val range = Int.MIN_VALUE..(scaledEx.scalar as ActualScalar).value
  }

  data class Exact(override val scaledEx: ScaledExpression) : Counting(scaledEx) {
    init {
      if (scaledEx.scalar is XScalar) {
        throw PetsSyntaxException("can't use X in requirements (yet?)")
      }
    }

    override fun toString() = "=${scaledEx.toFullString()}" // no "=5" or "=Heat"

    override val range = (scaledEx.scalar as ActualScalar).value..scaledEx.scalar.value
  }

  data class Or(val requirements: Set<Requirement>) : Requirement() {
    constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement,
    ) : this(setOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(requirements)
    override fun toString() = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun precedence() = 3

    override fun safeToNestIn(container: PetNode): Boolean {
      return super.safeToNestIn(container) && container !is IfTrigger
    }
  }

  data class And(val requirements: List<Requirement>) : Requirement() {
    constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement
    ) : this(listOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(requirements)
    override fun toString() = requirements.joinToString { groupPartIfNeeded(it) }
    override fun precedence() = 1

    override fun safeToNestIn(container: PetNode): Boolean {
      return super.safeToNestIn(container) && container !is IfTrigger
    }
  }

  data class Transform(val requirement: Requirement, override val transformKind: String) :
      Requirement(), GenericTransform<Requirement> {
    override fun visitChildren(visitor: Visitor) = visitor.visit(requirement)
    override fun toString() = "$transformKind[$requirement]"
    override fun extract() = requirement
  }

  override val kind = Requirement::class.simpleName!!

  companion object : PetTokenizer() {

    fun split(requirement: Iterable<Requirement>) = requirement.flatMap { split(it) }

    fun split(requirement: Requirement): List<Requirement> =
        if (requirement is And) {
          split(requirement.requirements)
        } else {
          listOf(requirement)
        }

    internal fun parser(): Parser<Requirement> {
      return parser {
        val orReq =
            separatedTerms(atomParser(), _or) map
                {
                  val set = it.toSet()
                  if (set.size == 1) set.first() else Or(set)
                }
        commaSeparated(orReq) map { if (it.size == 1) it.first() else And(it) }
      }
    }

    /** A requirement suitable for being nested directly in something else. */
    internal fun atomParser(): Parser<Requirement> {
      return parser {
        val scaledEx = parser {
          val scalarAndOptionalEx = rawScalar and optional(Expression.parser())
          val optionalScalarAndEx = optional(rawScalar) and Expression.parser()

          scalarAndOptionalEx or optionalScalarAndEx map { (scalar, expr) ->
            scaledEx(ActualScalar(scalar ?: 1), expr)
          }
        }
        val min = scaledEx map ::Min
        val max = skip(_max) and scaledEx map ::Max
        val exact = skipChar('=') and scaledEx map ::Exact
        val transform =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }
        transform or min or max or exact or group(parser())
      }
    }
  }
}
