package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.util.suf

/**
 * A rule a player may invoke, like `Plant -> 7 MC`, as defined by
 * [section 9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions).
 * In practice these are used by the Pets classes `StandardAction`, `StandardProject`, `ActionCard`,
 * and `RequiredAction`.
 *
 * An action is an optional cost, an arrow, and an instruction ([rule
 * L9-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)). It
 * means: spend the cost, then do the result — `cost -> I` denotes `-cost! THEN I`, with nothing
 * added beyond
 * [rule L6-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)'s
 * `THEN` ([rule
 * L9-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
 * Actions are eventually lowered into triggered [Effect]s keyed to the action's position on its
 * class ([rule
 * L9-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
 */
public data class Action(
    /** What is given up, written without a minus sign, or null for a costless action. */
    val cost: Cost?,

    /** What the player gets. */
    val instruction: InstructionTree,
) : PetElement() {
  override val kind: kotlin.reflect.KClass<out PetNode> = Action::class

  override fun toString(): String = "${cost.suf(' ')}-> $instruction"

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, instruction)

  /**
   * Converts this action into the instruction performed when the action is used: `-cost! THEN
   * instruction`, or just the instruction when there is no cost ([rule
   * L9-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
   */
  internal fun toInstruction(): InstructionTree {
    val lhs = cost?.toInstruction() ?: return instruction
    val allInstructions =
        when (instruction) {
          is Then -> listOf(lhs) + instruction.instructions
          else -> listOf(lhs, instruction)
        }
    val lowered = Then.createTree(allInstructions) as Then
    val nestedScope = (instruction as? Then)?.typeVariables
    return lowered.withTypeVariables(
        if (nestedScope == null) typeVariables else typeVariables + nestedScope
    )
  }

  /**
   * What an [Action] gives up. Per
   * [rule L9-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)
   * a cost is a scaled expression, optionally scaled by a metric, optionally inside a transform
   * block — a comma-separated or gated cost is rejected, because alternative costs are written as
   * separate actions, each one thing a player can choose to do.
   */
  public sealed class Cost : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Cost::class

    internal abstract fun toInstruction(): InstructionTree

    /**
     * Gives up [scaledEx]; it lowers to a mandatory removal ([rule
     * L9-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
     */
    public data class Spend(val scaledEx: ScaledExpression) : Cost() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

      override fun toString(): String = scaledEx.toString()

      init {
        checkNonzero(scaledEx.scalar)
      }

      // I believe Ants/Predators are the reasons for MANDATORY here
      override fun toInstruction() = remove(scaledEx)
    }

    /** Scales [cost] by the value of [metric], mirroring [Instruction.Per]. */
    // can't do non-prod per prod yet
    public data class Per(val cost: Cost, val metric: Metric) : Cost() {
      init {
        if (cost is Per) throw PetSyntaxException("Might support in future?")
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, metric)

      override fun toString(): String = "${groupPartIfNeeded(cost)} / ${groupPartIfNeeded(metric)}"

      override fun precedence(): Int = 5

      override fun toInstruction(): Instruction =
          Instruction.Per(cost.toInstruction() as Instruction, metric)
    }

    /**
     * A [cost] marked for rewriting by the handler named by [transformKind], as
     * [rule L10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
     * allows on an action cost.
     */
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

          perCost
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
