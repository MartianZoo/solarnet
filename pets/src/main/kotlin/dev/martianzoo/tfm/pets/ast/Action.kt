package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.PetVisitor
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.util.suf

data class Action(val cost: Cost?, val instruction: Instruction) : PetNode() {
  override val kind = "Action"

  override fun toString() = "${cost.suf(' ')}-> $instruction"
  override fun visitChildren(v: PetVisitor) = v.visit(cost, instruction)

  sealed class Cost : PetNode() {
    override val kind = "Cost"

    abstract fun toInstruction(): Instruction

    data class Spend(val sat: ScalarAndType) : Cost() {
      override fun visitChildren(v: PetVisitor) = v.visit(sat)
      override fun toString() = sat.toString()

      init {
        if (sat.scalar == 0) {
          throw PetException("Can't spend zero")
        }
      }

      // I believe Ants/Predators are the reasons for MANDATORY here
      override fun toInstruction() = Remove(sat, MANDATORY)
    }

    // can't do non-prod per prod yet
    data class Per(val cost: Cost, val sat: ScalarAndType) : Cost() {
      init {
        if (sat.scalar == 0) {
          throw PetException("Can't do something 'per' a non-positive amount")
        }
        when (cost) {
          is Or,
          is Multi -> throw PetException("Break into separate Per instructions")
          is Per -> throw PetException("Might support in future?")
          else -> {}
        }
      }
      override fun visitChildren(v: PetVisitor) = v.visit(cost, sat)

      override fun toString() = "$cost / ${sat.toString(forceType = true)}"
      override fun precedence() = 5

      override fun toInstruction() = Instruction.Per(cost.toInstruction(), sat)
    }

    data class Or(var costs: Set<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toSet())

      init {
        require(costs.size >= 2)
      }
      override fun visitChildren(v: PetVisitor) = v.visit(costs)

      override fun toString() = costs.joinToString(" OR ") { groupPartIfNeeded(it) }
      override fun precedence() = 3

      override fun toInstruction() = Instruction.Or(costs.map { it.toInstruction() }.toSet())
    }

    data class Multi(var costs: List<Cost>) : Cost() {
      constructor(vararg costs: Cost) : this(costs.toList())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(v: PetVisitor) = v.visit(costs)
      override fun toString() = costs.joinToString { groupPartIfNeeded(it) }
      override fun precedence() = 1

      override fun toInstruction() = Instruction.Multi(costs.map { it.toInstruction() })
    }

    // TODO rename transformName all
    data class Transform(val cost: Cost, override val transform: String) :
        Cost(), GenericTransform<Cost> {
      override fun visitChildren(v: PetVisitor) = v.visit(cost)
      override fun toString() = "$transform[$cost]"

      override fun toInstruction() = Instruction.Transform(cost.toInstruction(), transform)
      override fun extract() = cost
    }

    companion object : PetParser() {
      fun cost(text: String): Cost = Parsing.parse(parser(), text)

      fun parser(): Parser<Cost> {
        return parser {
          val sat = ScalarAndType.parser()
          val cost: Parser<Cost> = parser { parser() }
          val spend = sat map Cost::Spend

          val maybeTransform =
              spend or
              (transform(cost) map { (node, transformName) ->
                Transform(node, transformName)
              })

          val perCost =
              maybeTransform and
              optional(skipChar('/') and sat) map { (cost, sat) ->
                if (sat == null) cost else Per(cost, sat)
              }

          val orCost = separatedTerms(perCost or group(cost), _or) map {
            val set = it.toSet()
            if (set.size == 1) set.first() else Or(set)
          }

          commaSeparated(orCost or group(cost)) map { if (it.size == 1) it.first() else Multi(it) }
        }
      }
    }
  }

  companion object : PetParser() {
    fun action(text: String): Action = Parsing.parse(parser(), text)

    internal fun parser(): Parser<Action> =
        optional(Cost.parser()) and
        skip(_arrow) and
        Instruction.parser() map { (c, i) ->
          Action(c, i)
        }
  }
}
