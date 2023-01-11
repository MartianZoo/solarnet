package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.SpecialComponent.Production
import dev.martianzoo.tfm.pets.SpecialComponent.This
import dev.martianzoo.tfm.pets.SpecialComponent.UseAction
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.PetsNode.GenericTransform
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
  require(index1Ref >= 1) { index1Ref }
  val instruction = instructionFromAction(action.cost?.toInstruction(), action.instruction)
  val trigger = OnGain(gte("$UseAction$index1Ref", This.type))
  return Effect(trigger, instruction, automatic = false)
}

private fun instructionFromAction(lhs: Instruction?, rhs: Instruction): Instruction {
  if (lhs == null) return rhs

  // Handle the Ants case (TODO intensity?)
  if (lhs is Remove && rhs is Gain && lhs.qe.scalar == rhs.qe.scalar) {
    return Transmute(SimpleFrom(rhs.qe.expression, lhs.qe.expression))
  }

  // Nested THENs are just silly
  val allInstructions = when (rhs) {
    is Then -> listOf(lhs) + rhs.instructions
    else -> listOf(lhs, rhs)
  }
  return Then(allInstructions)
}

internal fun actionsToEffects(actions: Collection<Action>) =
    actions.withIndex().map { (index0Ref, action) ->
      actionToEffect(action, index1Ref = index0Ref + 1)
    }

internal fun immediateToEffect(instruction: Instruction): Effect {
  return Effect(OnGain(This.type), instruction, automatic = false)
}

fun <P : PetsNode> replaceThis(node: P, resolveTo: GenericTypeExpression) =
    node.replaceTypes(This.type, resolveTo)
        .replaceTypes(ClassLiteral(This.className), ClassLiteral(resolveTo.className))

fun <P : PetsNode> P.replaceTypes(from: TypeExpression, to: TypeExpression): P {
  return replaceTypesIn(this, from, to)
}

internal fun <P : PetsNode> replaceTypesIn(node: P, from: TypeExpression, to: TypeExpression) =
    TypeReplacer(from, to).transform(node)

private class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P) = if (node == from) {
    @Suppress("UNCHECKED_CAST")
    to as P
  } else {
    super.transform(node)
  }
}

fun <P : PetsNode> deprodify(node: P, producible: Set<ClassName>): P {
  val deprodifier = object : AstTransformer() {
    var inProd: Boolean = false

    override fun <P : PetsNode?> transform(node: P): P {
      val rewritten = when {
        node is GenericTransform<*> && node.transform == "PROD" -> { // TODO: support multiple better
          require(!inProd)
          inProd = true
          transform(node.extract()).also { inProd = false }
        }

        inProd && node is GenericTypeExpression && node.className in producible ->
          Production.type.copy(specs = node.specs + ClassLiteral(node.className))

        else -> super.transform(node)
      }
      @Suppress("UNCHECKED_CAST")
      return rewritten as P
    }
  }
  return deprodifier.transform(node)
}
