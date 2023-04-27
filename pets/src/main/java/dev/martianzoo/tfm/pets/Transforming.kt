package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.PetTransformer.Companion.noOp
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.unraw
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

  /**
   * Reverses the effect of [replaceThisExpressionsWith] (though it will also turn [contextType]
   * expressions into `This` that didn't start out that way).
   */
  public fun replaceWithThisExpressions(contextType: Expression): PetTransformer =
      chain(
          replacer(contextType, THIS.expression),
          replacer(contextType.className.classExpression(), THIS.classExpression()),
      )

  /** Replaces each occurrence of `Owner` with the given player. */
  public fun replaceOwnerWith(owner: Player): PetTransformer =
      if (owner == ENGINE) noOp() else replacer(OWNER.expression, owner.expression)

  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    val unrapt = action.unraw()
    require(index1Ref >= 1) { index1Ref }
    val instruction = actionToInstruction(unrapt)
    val trigger = OnGainOf.create(cn("$USE_ACTION$index1Ref").addArgs(THIS))
    val effect = Effect(trigger, instruction, automatic = false)
    return if (unrapt == action) effect else effect.raw()
  }

  private fun actionToInstruction(action: Action): Instruction {
    val lhs = action.cost?.toInstruction()
    val rhs = action.instruction

    if (lhs == null) return rhs

    // Handle the Ants case
    Transmute.tryMerge(lhs, rhs)?.let {
      return it
    }

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
    return if (instruction == NoOp || instruction == NoOp.raw()) {
      null
    } else {
      Effect(WhenGain, instruction, automatic)
    }
  }
}
