package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.BaseTokenizer
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.util.suf

public data class Action(val cost: Cost?, val instruction: Instruction) : PetNode() {
  override val kind = Action::class.simpleName!!

  override fun toString() = "${cost.suf(' ')}-> $instruction"
  override fun visitChildren(visitor: Visitor) = visitor.visit(cost, instruction)

  sealed class Cost : PetNode() {
    override val kind = Cost::class.simpleName!!

    abstract fun toInstruction(): Instruction

    data class Spend(val scaledEx: ScaledExpression) : Cost() {
      override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
      override fun toString() = scaledEx.toString()

      init {
        if (scaledEx.scalar == 0) {
          throw PetException("Can't spend zero")
        }
      }

      // I believe Ants/Predators are the reasons for MANDATORY here
      override fun toInstruction() = Remove(scaledEx, MANDATORY)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val metric: Metric) : Cost() {
      init {
        when (cost) {
          is Or,
          is Multi -> throw PetException("Break into separate Per instructions")
          is Per -> throw PetException("Might support in future?")
          else -> {}
        }
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(cost, metric)

      override fun toString() = "$cost / $metric"
      override fun precedence() = 5

      override fun toInstruction() = Instruction.Per(cost.toInstruction(), metric)
    }

    data class Or(var costs: Set<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toSet())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(costs)

      override fun toString() = costs.joinToString(" OR ") { groupPartIfNeeded(it) }
      override fun precedence() = 3

      override fun toInstruction() = Instruction.Or(costs.map { it.toInstruction() })
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toList())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(costs)
      override fun toString() = costs.joinToString { groupPartIfNeeded(it) }
      override fun precedence() = 1

      override fun toInstruction() = Instruction.Multi(costs.map { it.toInstruction() })
    }

    data class Transform(val cost: Cost, override val transformKind: String) :
        Cost(), GenericTransform<Cost> {
      override fun visitChildren(visitor: Visitor) = visitor.visit(cost)
      override fun toString() = "$transformKind[$cost]"

      override fun toInstruction() = Instruction.Transform(cost.toInstruction(), transformKind)
      override fun extract() = cost
    }

    companion object : BaseTokenizer() {
      fun cost(text: String): Cost = Parsing.parse(parser(), text)

      fun parser(): Parser<Cost> {
        return parser {
          val spend = ScaledExpression.parser() map Cost::Spend
          val transform = transform(parser()) map { (node, tname) -> Transform(node, tname) }
          val atomCost = transform or spend

          val perCost =
              atomCost and
              optional(skipChar('/') and Metric.parser()) map { (cost, met) ->
                if (met == null) cost else Per(cost, met)
              }

          val orCost =
              separatedTerms(perCost or group(parser()), _or) map {
                val set = it.toSet()
                if (set.size == 1) set.first() else Or(set)
              }

          commaSeparated(orCost or group(parser())) map {
            if (it.size == 1) it.first() else Multi(it)
          }
        }
      }
    }
  }

  companion object : BaseTokenizer() {
    fun action(text: String): Action = Parsing.parse(parser(), text)

    internal fun parser(): Parser<Action> =
        optional(Cost.parser()) and
        skip(_arrow) and
        Instruction.parser() map { (c, i) -> Action(c, i) }
  }
}
