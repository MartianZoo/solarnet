package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.SpecialClassNames.USE_ACTION
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
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.GenericTransform
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.toSetStrict

internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
  require(index1Ref >= 1) { index1Ref }
  val instruction = instructionFromAction(action.cost?.toInstruction(), action.instruction)
  val trigger = OnGain(gte("$USE_ACTION$index1Ref", THIS.type))
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

internal fun actionsToEffects(actions: Set<Action>): Set<Effect> =
    actionsToEffects(actions as Collection<Action>).toSetStrict()

internal fun actionsToEffects(actions: Collection<Action>): List<Effect> =
    actions.withIndex().map { (index0Ref, action) ->
      actionToEffect(action, index1Ref = index0Ref + 1)
    }

internal fun immediateToEffect(instruction: Instruction): Effect {
  return Effect(OnGain(THIS.type), instruction, automatic = false)
}

fun <P : PetNode> replaceThis(node: P, resolveTo: GenericTypeExpression) =
    node.replaceTypes(THIS.type, resolveTo)
        .replaceTypes(ClassLiteral(THIS), ClassLiteral(resolveTo.className))

fun <P : PetNode> P.replaceTypes(from: TypeExpression, to: TypeExpression): P {
  return replaceTypesIn(this, from, to)
}

internal fun <P : PetNode> replaceTypesIn(node: P, from: TypeExpression, to: TypeExpression) =
    TypeReplacer(from, to).transform(node)

private class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : PetNodeVisitor() {
  override fun <P : PetNode?> transform(node: P) = if (node == from) {
    @Suppress("UNCHECKED_CAST")
    to as P
  } else {
    super.transform(node)
  }
}

fun <P : PetNode> deprodify(node: P, producible: Set<ClassName>): P {
  val deprodifier = object : PetNodeVisitor() {
    var inProd: Boolean = false

    override fun <P : PetNode?> transform(node: P): P {
      val rewritten = when {
        node is GenericTransform<*> && node.transform == "PROD" -> { // TODO: support multiple better
          require(!inProd)
          inProd = true
          transform(node.extract()).also { inProd = false }
        }

        inProd && node is GenericTypeExpression && node.className in producible ->
          PRODUCTION.type.copy(specs = node.specs + ClassLiteral(node.className))

        else -> super.transform(node)
      }
      @Suppress("UNCHECKED_CAST")
      return rewritten as P
    }
  }
  return deprodifier.transform(node)
}
