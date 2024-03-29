package dev.martianzoo.pets

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.util.toSetStrict

/**
 * Various functions for transforming Pets syntax trees. Many more interesting transformers require
 * a class table, and therefore are found in the `engine` module (`MClassTable.transformers`).
 */
public object Transforming {
  /**
   * Replaces each occurrence of the special `This` expression with [contextType], replacing
   * `Class<This>` appropriately as well.
   */
  public fun replaceThisExpressionsWith(contextType: Expression): PetTransformer =
      chain(
          replacer(THIS.classExpression(), contextType.className.classExpression()),
          replacer(THIS.expression, contextType),
      )

  /** Replaces each occurrence of `Owner` with the given player. */
  public fun replaceOwnerWith(owner: Player): PetTransformer =
      if (owner == ENGINE) noOp() else replacer(OWNER.expression, owner.expression)

  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    require(index1Ref >= 1) { index1Ref }
    val instruction = actionToInstruction(action)
    val trigger = OnGainOf.create(cn("$USE_ACTION$index1Ref").of(THIS))
    return Effect(trigger, instruction, automatic = false)
  }

  private fun actionToInstruction(action: Action): Instruction {
    val lhs = action.cost?.toInstruction()
    val rhs = action.instruction

    if (lhs == null) return rhs

    // Nested THENs are just silly
    val allInstructions =
        when (rhs) {
          is Then -> listOf(lhs) + rhs.instructions
          else -> listOf(lhs, rhs)
        }
    return Then(allInstructions)
  }

  internal fun actionListToEffects(actions: Collection<Action>): Set<Effect> =
      actions.withIndex().toSetStrict { (index0Ref, action) ->
        actionToEffect(action, index1Ref = index0Ref + 1)
      }

  internal fun immediateToEffect(instruction: Instruction, automatic: Boolean = false): Effect? {
    return if (instruction == NoOp) {
      null
    } else {
      Effect(WhenGain, instruction, automatic)
    }
  }
}
