package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transformInSeries
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.replaceAll
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.unraw
import dev.martianzoo.util.toSetStrict

/** Various functions for transforming Pets syntax trees. */
public object PureTransformers {
  public fun replaceThisWith(contextType: Expression): PetTransformer =
      transformInSeries(
          replacer(THIS.classExpression(), contextType.className.classExpression()),
          replacer(THIS.expression, contextType),
      )

  public fun unreplaceThisWith(contextType: Expression): PetTransformer =
      transformInSeries(
          replacer(contextType, THIS.expression),
          replacer(contextType.className.classExpression(), THIS.classExpression()),
      )

  public fun replaceOwnerWith(owner: ClassName): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return node.replaceAll(OWNER.expression, owner.expression)
      }
    }
  }

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
