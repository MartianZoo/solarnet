package dev.martianzoo.pets

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PetNode.Companion.replacer

/**
 * Various functions for transforming Pets syntax trees. Many more interesting transformers require
 * a class table, and therefore are found in the `engine` module's `Transformers` class.
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
        override fun transformNode(node: PetNode): PetNode {
          if (
              node is Expression &&
                  node.className == OWNER &&
                  node.arguments.isEmpty() &&
                  node.refinement == null
          ) {
            return node.copy(className = owner.className)
          }
          return transformChildren(node)
        }
      }

  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    val whichAction = whichAction(index1Ref)
    val instruction = action.toInstruction()
    val trigger = OnGainOf.create(USE_ACTION.of(THIS, whichAction))
    return Effect(trigger, instruction, automatic = false)
  }

  internal fun actionListToEffects(actions: Collection<Action>): List<Effect> =
      actions.withIndex().map { (index0Ref, action) ->
        actionToEffect(action, index1Ref = index0Ref + 1)
      }

  internal fun actionSelectors(actions: Collection<Action>): Set<ClassName> =
      actions.indices.mapTo(linkedSetOf()) { whichAction(it + 1) }

  private fun whichAction(index1Ref: Int): ClassName =
      listOf(cn("First"), cn("Second"), cn("Third")).getOrNull(index1Ref - 1)
          ?: throw IllegalArgumentException("A component can offer only three actions: $index1Ref")

  internal fun immediateToEffect(
      instruction: InstructionTree,
      effectIsAutomatic: Boolean = false,
  ): Effect? {
    val syntaxTree =
        if (instruction is InstructionGroup) {
          InstructionGroup.createTree(instruction.instructions)
        } else {
          instruction
        }
    return if (syntaxTree == NoOp) {
      null
    } else {
      Effect(WhenGain, syntaxTree, effectIsAutomatic)
    }
  }
}
