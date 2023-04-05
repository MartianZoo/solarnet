package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.SpecialClassNames.USE_ACTION
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
import dev.martianzoo.tfm.pets.ast.TransformNode.Companion.unwrap
import dev.martianzoo.util.toSetStrict

/** Various functions for transforming Pets syntax trees. */
public object PureTransformers {
  public fun transformInSeries(xers: List<PetTransformer?>): PetTransformer =
      CompositeTransformer(xers.filterNotNull())
  public fun transformInSeries(vararg xers: PetTransformer?): PetTransformer =
      transformInSeries(xers.toList())

  internal open class CompositeTransformer(val transformers: List<PetTransformer>) :
      PetTransformer() {
    constructor(vararg transformers: PetTransformer) : this(transformers.toList())

    override fun <P : PetNode> transform(node: P): P { // TODO null passthru??
      var result = node
      for (xer in transformers) {
        result = xer.transform(result)
      }
      return result
    }
  }

  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    val unrapt = unwrap(action, RAW)
    require(index1Ref >= 1) { index1Ref }
    val instruction = actionToInstruction(unrapt)
    val trigger = OnGainOf.create(cn("$USE_ACTION$index1Ref").addArgs(THIS))
    val effect = Effect(trigger, instruction, automatic = false)
    return if (unrapt == action) effect else effect.raw() // TODO clunky
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
      Effect(WhenGain, instruction, automatic) // TODO ugh
    }
  }

  // TODO check if this really what callers want to do
  public fun <P : PetNode> P.replaceAll(from: PetNode, to: PetNode): P {
    if (from == to) return this
    return object : PetTransformer() {
          override fun <Q : PetNode> transform(node: Q): Q =
              if (node == from) {
                @Suppress("UNCHECKED_CAST")
                to as Q
              } else {
                transformChildren(node)
              }
        }
        .transform(this)
  }

  public fun replaceThisWith(contextType: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return node
            .replaceAll(THIS.classExpression(), contextType.className.classExpression())
            .replaceAll(THIS.expr, contextType)
      }
    }
  }

  public fun unreplaceThisWith(contextType: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return node
            .replaceAll(contextType, THIS.expr)
            .replaceAll(contextType.className.classExpression(), THIS.classExpression())
      }
    }
  }

  public fun replaceOwnerWith(owner: ClassName): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return node.replaceAll(SpecialClassNames.OWNER.expr, owner.expr)
      }
    }
  }
}
