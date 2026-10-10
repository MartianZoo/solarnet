package dev.martianzoo.pets.ast

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.util.suf

/**
 * A rule a player may invoke, like `Plant -> 7 MC`, as defined by
 * [section 7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions).
 * In practice these are used by the Pets classes `StandardProject`, `ActionCard`, and
 * `RequiredAction`.
 *
 * An action is an optional cost, an arrow, and an instruction ([rule
 * L7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)). It
 * means: spend the cost, then do the result — `cost -> I` denotes `-cost! THEN I`, with nothing
 * added beyond
 * [rule L2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)'s
 * `THEN` ([rule
 * L7-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)).
 * Actions are eventually lowered into triggered [Effect]s keyed to the action's position on its
 * class ([rule
 * L7-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)).
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
   * L7-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)).
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
   * [rule L7-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)
   * a cost is a scaled expression, optionally scaled by a metric, optionally inside a transform
   * block — a comma-separated or gated cost is rejected, because alternative costs are written as
   * separate actions, each one thing a player can choose to do.
   */
  public sealed class Cost private constructor() : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Cost::class

    internal abstract fun toInstruction(): InstructionTree

    /**
     * Gives up [scaledEx]; it lowers to a mandatory removal ([rule
     * L7-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)).
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
        if (cost is Per)
            throw PetSyntaxException("action costs cannot contain nested `PER` metrics")
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost, metric)

      override fun toString(): String = "${groupPartIfNeeded(cost)} / ${groupPartIfNeeded(metric)}"

      override fun precedence(): Int = 5

      override fun toInstruction(): Instruction =
          Instruction.Per(cost.toInstruction() as Instruction, metric)
    }

    /**
     * A [cost] marked for rewriting by the handler named by [transformKind], as
     * [rule L8-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks)
     * allows on an action cost.
     */
    public data class Transform(val cost: Cost, override val transformKind: String) :
        Cost(), TransformNode<Cost> {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(cost)

      override fun toString(): String = "$transformKind[$cost]"

      override fun toInstruction(): InstructionTree = Transform(cost.toInstruction(), transformKind)

      override fun extract(): Cost = cost
    }
  }
}
