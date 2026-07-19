package dev.martianzoo.pets

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.pets.PetTransformer.Companion.chain
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

  /** Replaces each occurrence of the contextual `Owner` placeholder with [owner]. */
  @Suppress("ComplexCondition") // TODO fix
  public fun replaceOwnerWith(owner: HasClassName): PetTransformer =
      object : PetTransformer() {
        override fun <P : dev.martianzoo.pets.ast.PetNode> transform(node: P): P {
          if (
              node is Expression &&
                  node.className == OWNER &&
                  node.arguments.isEmpty() &&
                  node.refinement == null
          ) {
            @Suppress("UNCHECKED_CAST")
            return node.copy(className = owner.className) as P
          }
          return transformChildren(node)
        }
      }

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

  internal fun actionListToEffects(actions: Collection<Action>): List<Effect> =
      actions.withIndex().map { (index0Ref, action) ->
        actionToEffect(action, index1Ref = index0Ref + 1)
      }

  internal fun immediateToEffect(
      instruction: Instruction,
      effectIsAutomatic: Boolean = false,
  ): Effect? {
    return if (instruction == NoOp) {
      null
    } else {
      Effect(WhenGain, instruction, effectIsAutomatic)
    }
  }
}
