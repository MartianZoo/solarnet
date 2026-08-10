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
import dev.martianzoo.pets.TypeLinking
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.util.suf

/**
 * Classes can offer actions like `Steel OR Plant -> 7` for players to manually trigger. In practice
 * these are used by the Pets classes `StandardAction`, `StandardProject`, `ActionCard`, and
 * `Mandate`.
 *
 * Actions eventually get converted into triggered [Effect]s; the example above would become
 * `UseAction1<ElectroCatapult>: (-Steel OR -Plant) THEN 7`.
 */
public data class Action(val cost: Cost?, val instruction: Instruction) : PetElement() {
  override val kind: kotlin.reflect.KClass<out PetNode> = Action::class

  override fun toString(): String = "${cost.suf(' ')}-> $instruction"

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, instruction)

  /** Converts this action into the instruction performed when the action is used. */
  internal fun toInstruction(): Instruction {
    val lhs = cost?.toInstruction() ?: return instruction
    val allInstructions =
        when (instruction) {
          is Then -> listOf(lhs) + instruction.instructions
          else -> listOf(lhs, instruction)
        }
    val actionSources = TypeLinking.sourcesAcrossRegions(this)
    val resultSources = (instruction as? Then)?.linkedTypeSources.orEmpty()
    return Then(allInstructions).withLinkedTypeSources(actionSources + resultSources)
  }

  public sealed class Cost : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Cost::class

    internal abstract fun toInstruction(): Instruction

    public data class Spend(val scaledEx: ScaledExpression) : Cost() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

      override fun toString(): String = scaledEx.toString()

      init {
        checkNonzero(scaledEx.scalar)
      }

      // I believe Ants/Predators are the reasons for MANDATORY here
      override fun toInstruction() = remove(scaledEx)
    }

    public data class Gated(val gate: Requirement, val cost: Cost) : Cost() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(gate, cost)

      override fun toString(): String = "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(cost)}"

      override fun precedence(): Int = 4

      override fun safeToNestIn(container: PetNode): Boolean =
          super.safeToNestIn(container) && container !is Or

      override fun toInstruction(): Instruction =
          Instruction.Gated.create(gate, cost.toInstruction())
    }

    // can't do non-prod per prod yet
    internal data class Per(val cost: Cost, val metric: Metric) : Cost() {
      init {
        when (cost) {
          is Or,
          is Multi -> throw PetSyntaxException("Break into separate Per instructions")
          is Per -> throw PetSyntaxException("Might support in future?")
          else -> {}
        }
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(cost, metric)

      override fun toString() = "$cost / ${groupPartIfNeeded(metric)}"

      override fun precedence() = 5

      override fun toInstruction() = Per(cost.toInstruction(), metric)
    }

    internal data class Or(var costs: Set<Cost>) : Cost() {
      internal constructor(vararg costs: Cost) : this(costs.toSet())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(costs)

      override fun toString() = costs.joinToString(" OR ") { groupPartIfNeeded(it) }

      override fun precedence() = 3

      override fun toInstruction() = Or(costs.map { it.toInstruction() })
    }

    internal data class Multi(var costs: List<Cost>) : Cost() {
      internal constructor(vararg costs: Cost) : this(costs.toList())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(costs)

      override fun toString() = costs.joinToString { groupPartIfNeeded(it) }

      override fun precedence() = 1

      override fun toInstruction() = Multi(costs.map { it.toInstruction() })
    }

    internal data class Transform(val cost: Cost, override val transformKind: String) :
        Cost(), TransformNode<Cost> {
      override fun visitChildren(visitor: Visitor) = visitor.visit(cost)

      override fun toString() = "$transformKind[$cost]"

      override fun toInstruction() = Transform(cost.toInstruction(), transformKind)

      override fun extract() = cost
    }

    internal companion object : PetTokenizer() {
      fun parser(): Parser<Cost> {
        return parser {
          val spend = ScaledExpression.parser() map Cost::Spend
          val transform = transform(parser()) map { (node, tname) -> Transform(node, tname) }
          val atomCost = transform or spend or group(parser())

          val perCost =
              atomCost and
                  optional(skipChar('/') and Metric.atomParser()) map
                  { (cost, met) ->
                    if (met == null) cost else Per(cost, met)
                  }

          val orCost =
              separatedTerms(perCost or group(parser()), _or) map
                  {
                    val set = it.toSet()
                    if (set.size == 1) set.first() else Or(set)
                  }

          val gatedCost =
              optional(Requirement.atomParser() and skipChar(':')) and
                  orCost map
                  { (gate, cost) ->
                    if (gate == null) cost else Gated(gate, cost)
                  }

          commaSeparated(gatedCost) map
              {
                if (it.size == 1) it.first() else Multi(it)
              }
        }
      }
    }
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Action> =
        optional(Cost.parser()) and
            skip(_arrow) and
            Instruction.parser() map
            { (c, i) ->
              Action(c, i)
            }
  }
}
