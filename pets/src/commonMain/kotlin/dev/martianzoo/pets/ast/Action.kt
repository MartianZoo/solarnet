package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.TypeLinking
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.util.suf

/**
 * Classes can offer actions like `Plant -> 7` for players to manually trigger. In practice these
 * are used by the Pets classes `StandardAction`, `StandardProject`, `ActionCard`, and `Mandate`.
 *
 * Actions eventually get converted into triggered [Effect]s.
 */
public data class Action(val cost: Cost?, val instruction: InstructionTree) : PetElement() {
  override val kind: kotlin.reflect.KClass<out PetNode> = Action::class

  override fun toString(): String = "${cost.suf(' ')}-> $instruction"

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, instruction)

  /** Converts this action into the instruction performed when the action is used. */
  internal fun toInstruction(): InstructionTree {
    val lhs = cost?.toInstruction() ?: return instruction
    val allInstructions =
        when (instruction) {
          is Then -> listOf(lhs) + instruction.instructions
          else -> listOf(lhs, instruction)
        }
    val actionSources = TypeLinking.sourcesAcrossRegions(this)
    val resultSources = (instruction as? Then)?.linkedTypeSources.orEmpty()
    val result = Then.createTree(allInstructions) as Then
    return result.withLinkedTypeSources(actionSources + resultSources)
  }

  public sealed class Cost : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Cost::class

    internal abstract fun toInstruction(): InstructionTree

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

      override fun toInstruction(): InstructionTree =
          Instruction.Gated.createTree(gate, cost.toInstruction())
    }

    // can't do non-prod per prod yet
    public data class Per(val cost: Cost, val metric: Metric) : Cost() {
      init {
        when (cost) {
          is Cost.Multi -> throw PetSyntaxException("Break into separate Per instructions")
          is Per -> throw PetSyntaxException("Might support in future?")
          else -> {}
        }
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, metric)

      override fun toString(): String = "${groupPartIfNeeded(cost)} / ${groupPartIfNeeded(metric)}"

      override fun precedence(): Int = 5

      override fun toInstruction(): Instruction =
          Instruction.Per(cost.toInstruction() as Instruction, metric)
    }

    public data class Multi(var costs: List<Cost>) : Cost() {
      private constructor(vararg costs: Cost) : this(costs.toList())

      init {
        require(costs.size >= 2)
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(costs)

      override fun toString(): String = costs.joinToString { groupPartIfNeeded(it) }

      override fun precedence(): Int = 1

      override fun toInstruction(): InstructionTree =
          InstructionGroup.createTree(costs.map { it.toInstruction() })
    }

    public data class Transform(val cost: Cost, override val transformKind: String) :
        Cost(), TransformNode<Cost> {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost)

      override fun toString(): String = "$transformKind[$cost]"

      override fun toInstruction(): InstructionTree = Transform(cost.toInstruction(), transformKind)

      override fun extract(): Cost = cost
    }

    internal companion object : PetTokenizer() {
      fun parser(): Parser<Cost> {
        return parser {
          val spend = ScaledExpression.parser() map Cost::Spend
          val transform = transform(parser()) map { (node, tname) -> Transform(node, tname) }
          val atomCost = transform or spend or group(parser())

          val perCost =
              atomCost and
                  optional(skipChar('/') and Metric.subtractionParser()) map
                  { (cost, met) ->
                    if (met == null) cost else Per(cost, met)
                  }

          val gatedCost =
              optional(Requirement.atomParser() and skipChar(':')) and
                  (perCost or group(parser())) map
                  { (gate, cost) ->
                    if (gate == null) cost else Gated(gate, cost)
                  }

          commaSeparated(gatedCost) map { if (it.size == 1) it.first() else Multi(it) }
        }
      }
    }
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Action> =
        optional(Cost.parser()) and
            skip(_arrow) and
            InstructionTree.parser() map
            { (c, i) ->
              Action(c, i)
            }
  }
}
