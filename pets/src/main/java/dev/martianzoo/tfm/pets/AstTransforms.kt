package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.api.SpecialClassNames.PRODUCTION
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
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.GenericTransform

/** Various functions for transforming Pets syntax trees. */
public object AstTransforms {
  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    require(index1Ref >= 1) { index1Ref }
    val instruction = actionToInstruction(action)
    val trigger = OnGainOf.create(cn("$USE_ACTION$index1Ref").addArgs(THIS))
    return Effect(trigger, instruction, automatic = false)
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
      actions
          .withIndex()
          .map { (index0Ref, action) -> actionToEffect(action, index1Ref = index0Ref + 1) }
          .toSet()

  internal fun immediateToEffect(instruction: Instruction, automatic: Boolean = false): Effect {
    return Effect(WhenGain, instruction, automatic = automatic)
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

  /** Transform any `PROD[...]` sections in a subtree to the equivalent subtree. */
  public fun <P : PetNode> deprodify(node: P, producible: Collection<ClassName>): P {
    // TODO is there some way this could act on Types instead of Expressions?
    // TODO eliminate unnecessary grouping
    val xer =
        object : PetTransformer() {
          var inProd: Boolean = false

          override fun <P : PetNode> transform(node: P): P {
            val rewritten: PetNode =
                when {
                  node is Multi -> {
                    val badIndex = node.instructions.indexOfFirst {
                      it is Instruction.Transform &&
                          it.transformKind == "PROD" &&
                          it.instruction is Multi
                    }
                    val xed = transformChildren(node)
                    if (badIndex == -1) {
                      xed
                    } else {
                      Multi.create(xed.instructions.subList(0, badIndex) +
                          (xed.instructions[badIndex] as Multi).instructions +
                          xed.instructions.subList(badIndex + 1, xed.instructions.size))!!
                    }
                  }
                  node is GenericTransform<*> && node.transformKind == "PROD" -> {
                    require(!inProd)
                    inProd = true
                    val inner = x(node.extract())
                    inProd = false
                    if (inner == node.extract()) {
                      throw RuntimeException("No standard resources found in PROD box: $inner")
                    }
                    inner
                  }
                  inProd && node is Expression && node.className in producible ->
                      PRODUCTION.addArgs(node.arguments + node.className.classExpression())
                  else -> transformChildren(node)
                }
            @Suppress("UNCHECKED_CAST") return rewritten as P
          }
        }
    return xer.transform(node)
  }
}
