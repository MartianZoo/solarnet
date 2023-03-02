package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.GenericTransform
import dev.martianzoo.tfm.pets.ast.TypeExpr

/** Various functions for transforming Pets syntax trees. */
public object AstTransforms {
  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    require(index1Ref >= 1) { index1Ref }
    val instruction = instructionFromAction(action.cost?.toInstruction(), action.instruction)
    val trigger = OnGainOf.create(cn("$USE_ACTION$index1Ref").addArgs(THIS))
    return Effect(trigger, instruction, automatic = false)
  }

  private fun instructionFromAction(lhs: Instruction?, rhs: Instruction): Instruction {
    if (lhs == null) return rhs

    // Handle the Ants case (TODO intensity?)
    if (lhs is Remove && rhs is Gain && lhs.scaledType.scalar == rhs.scaledType.scalar) {
      return Transmute(SimpleFrom(rhs.scaledType.typeExpr, lhs.scaledType.typeExpr))
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
      actions.withIndex().map { (index0Ref, action) ->
        actionToEffect(action, index1Ref = index0Ref + 1)
      }.toSet()

  internal fun immediateToEffect(instruction: Instruction): Effect {
    return Effect(WhenGain, instruction, automatic = false)
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
    }.transform(this)
  }

  /** Transform any `PROD[...]` sections in a subtree to the equivalent subtree. */
  public fun <P : PetNode> deprodify(node: P, producible: Set<ClassName>): P {
    // TODO is there some way this could act on Types instead of TypeExprs?
    // TODO eliminate unnecessary grouping
    val xer =
        object : PetTransformer() {
          var inProd: Boolean = false

          override fun <P : PetNode> transform(node: P): P {
            val rewritten: PetNode =
                when {
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
                  inProd && node is TypeExpr && node.className in producible ->
                      PRODUCTION.addArgs(node.arguments + CLASS.addArgs(node.className))
                  else -> transformChildren(node)
                }
            @Suppress("UNCHECKED_CAST") return rewritten as P
          }
        }
    return xer.transform(node)
  }
}
