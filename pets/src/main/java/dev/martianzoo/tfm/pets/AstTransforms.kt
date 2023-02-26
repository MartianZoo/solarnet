package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNER
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.SpecialClassNames.USE_ACTION
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

  internal fun actionsToEffects(actions: List<Action>): List<Effect> =
      actions.withIndex().map { (index0Ref, action) ->
        actionToEffect(action, index1Ref = index0Ref + 1)
      }

  internal fun immediateToEffect(instruction: Instruction): Effect {
    return Effect(WhenGain, instruction, automatic = false)
  }

  // TODO check if this really what callers want to do
  public fun <P : PetNode> replaceTypes(node: P, from: TypeExpr, to: TypeExpr): P {
    val xer =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P): P =
              if (node == from) {
                @Suppress("UNCHECKED_CAST")
                to as P
              } else {
                defaultTransform(node)
              }
        }
    return xer.transform(node)
  }

  /** Transform any `PROD[...]` sections in a subtree to the equivalent subtree. */
  public fun <P : PetNode> deprodify(node: P, producible: Set<ClassName>): P {
    // TODO is there some way this could act on Types instead of TypeExprs?
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
                  else -> defaultTransform(node)
                }
            @Suppress("UNCHECKED_CAST") return rewritten as P
          }
        }
    return xer.transform(node)
  }

  /**
   * For any type expression whose root type is in [ownedClasses] but does not already have anything
   * in [ownerClasses] as a type argument, adds `Owner` as a type argument. This is implementing
   * what the code `class Owned { DEFAULT This<Owner> ... }` is already trying to express, but I
   * haven't gotten that working in a general way yet.
   */
  // TODO redo this in terms of Type not TypeExpr
  public fun <P : PetNode> insertDefaultPlayer(
      node: P,
      ownerClasses: Set<ClassName>,
      ownedClasses: Set<ClassName>,
  ): P {
    fun hasOwner(typeExprs: List<TypeExpr>) =
        typeExprs.map { it.className }.intersect(ownerClasses).any()

    return object : PetTransformer() {
      override fun <Q : PetNode> transform(node: Q): Q {
        return if (node !is TypeExpr) {
          defaultTransform(node)
        } else if (node.className == CLASS) {
          node // don't descend; it's perfect how it is
        } else if (node.className in ownedClasses && !hasOwner(node.arguments)) {
          val xd = defaultTransform(node)
          val withOwnerPrepended = xd.replaceArgs(listOf(OWNER.type) + xd.arguments)
          @Suppress("UNCHECKED_CAST")
          withOwnerPrepended as Q
        } else {
          defaultTransform(node)
        }
      }
    }.transform(node)
  }
}
