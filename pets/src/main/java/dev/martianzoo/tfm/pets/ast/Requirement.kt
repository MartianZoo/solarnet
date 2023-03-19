package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.BaseTokenizer
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar

sealed class Requirement : PetNode() {
  open fun requiresThis() = false // TODO kick this out
  override fun safeToNestIn(container: PetNode) =
      super.safeToNestIn(container) || container is IfTrigger

  data class Min(val scaledEx: ScaledExpression) : Requirement() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun toString() = "$scaledEx"

    init {
      Scalar.checkNonzero(scaledEx.scalar)
    }

    override fun requiresThis() = this.scaledEx == ScaledExpression.scaledEx(1, THIS.expr)
  }

  data class Max(val scaledEx: ScaledExpression) : Requirement() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun toString() = "MAX ${scaledEx.toFullString()}" // no "MAX 5" or "MAX Heat"
  }

  data class Exact(val scaledEx: ScaledExpression) : Requirement() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun toString() = "=${scaledEx.toFullString()}" // no "=5" or "=Heat"

    override fun requiresThis() = this.scaledEx == ScaledExpression.scaledEx(1, THIS.expr)
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

    override fun requiresThis() = requirements.any { it.requiresThis() }

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

  companion object : BaseTokenizer() {
    fun requirement(text: String) = Parsing.parse(parser(), text)

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
        val scaledEx = ScaledExpression.parser()
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
