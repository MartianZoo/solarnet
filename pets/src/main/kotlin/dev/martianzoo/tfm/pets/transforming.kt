package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.GenericTransform
import dev.martianzoo.tfm.pets.ast.TypeExpr

internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
  require(index1Ref >= 1) { index1Ref }
  val instruction = instructionFromAction(action.cost?.toInstruction(), action.instruction)
  val trigger = OnGain(cn("$USE_ACTION$index1Ref").addArgs(THIS))
  return Effect(trigger, instruction, automatic = false)
}

private fun instructionFromAction(lhs: Instruction?, rhs: Instruction): Instruction {
  if (lhs == null) return rhs

  // Handle the Ants case (TODO intensity?)
  if (lhs is Remove && rhs is Gain && lhs.sat.scalar == rhs.sat.scalar) {
    return Transmute(SimpleFrom(rhs.sat.typeExpr, lhs.sat.typeExpr))
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
  return Effect(OnGain(THIS.type), instruction, automatic = false)
}

fun <P : PetNode> replaceThis(node: P, resolveTo: TypeExpr) =
    node.replaceTypes(THIS.type, resolveTo) // TODO liteals

fun <P : PetNode> P.replaceTypes(from: TypeExpr, to: TypeExpr): P {
  return replaceTypesIn(this, from, to)
}

internal fun <P : PetNode> replaceTypesIn(node: P, from: TypeExpr, to: TypeExpr) =
    node.transform(
        object : PetTransformer() {
          override fun <P : PetNode> doTransform(node: P): P =
              if (node == from) {
                @Suppress("UNCHECKED_CAST")
                to as P
              } else {
                defaultTransform(node)
              }
        })

fun <P : PetNode> deprodify(node: P, producible: Set<ClassName>): P {
  val deprodifier =
      object : PetTransformer() {
        var inProd: Boolean = false

        override fun <P : PetNode> doTransform(node: P): P {
          val rewritten: PetNode =
              when {
                node is GenericTransform<*> &&
                    node.transform == "PROD" -> { // TODO: support multiple better
                  require(!inProd)
                  inProd = true
                  x(node.extract()).also { inProd = false }
                }
                inProd && node is TypeExpr && node.className in producible ->
                    PRODUCTION.addArgs(node.arguments + CLASS.addArgs(node.className))
                else -> defaultTransform(node)
              }
          @Suppress("UNCHECKED_CAST") return rewritten as P
        }
      }
  return node.transform(deprodifier)
}
