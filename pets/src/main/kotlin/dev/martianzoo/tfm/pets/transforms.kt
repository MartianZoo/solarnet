package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialComponent.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.SpecialComponent.USE_ACTION
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.PetsNode.ProductionBox
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te

internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
  require(index1Ref >= 1) { index1Ref }
  val instruction = instructionFromAction(action.cost?.toInstruction(), action.instruction)
  val trigger = OnGain(te("$USE_ACTION$index1Ref", THIS.type))
  return Effect(trigger, instruction).also {
    println("Converted from Action: $it")
  }
}

private fun instructionFromAction(lhs: Instruction?, rhs: Instruction): Instruction {
  if (lhs == null) return rhs

  // Handle the Ants case (TODO intensity?)
  if (lhs is Remove && rhs is Gain && lhs.qe.scalar == rhs.qe.scalar) {
    return Transmute(SimpleFrom(
        rhs.qe.type ?: MEGACREDIT.type,
        lhs.qe.type ?: MEGACREDIT.type
    ))
  }

  // Nested THENs are just silly
  val allInstructions = when (rhs) {
    is Then -> listOf(lhs) + rhs.instructions
    else -> listOf(lhs, rhs)
  }
  return Then(allInstructions)
}

internal fun actionsToEffects(actions: Collection<Action>) =
    actions.withIndex().map {
      (index0Ref, action) -> actionToEffect(action, index1Ref = index0Ref + 1)
    }

internal fun immediateToEffect(immediate: Instruction): Effect {
  return Effect(OnGain(THIS.type), immediate)
}

// had to use an ungrammatical name
internal fun <P : PetsNode> resolveSpecialThisType(node: P, resolveTo: TypeExpression): P {
  return replaceTypesIn(node, THIS.type, resolveTo).also {
    println("Resolved `This` to `$resolveTo` in ${node.kind}: $it")
  }
}

internal fun <P : PetsNode> replaceTypesIn(node: P, from: TypeExpression, to: TypeExpression) =
    TypeReplacer(from, to).transform(node)

private class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P) = if (node == from) {
    to as P
  } else {
    super.transform(node)
  }
}

internal fun <P : PetsNode> spellOutQes(node: P): P {
  return QeSpellerOuter.transform(node).also {
    if (it != node) {
      println("spelled out QEs in ${node.kind}: $it")
    }
  }
}

private object QeSpellerOuter : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P): P {
    return when {
      node is QuantifiedExpression -> node.explicit() as P
      else -> super.transform(node)
    }
  }
}

internal fun <P : PetsNode> deprodify(node: P, producibleClassNames: Set<String>): P {
  return Deprodifier(producibleClassNames).transform(node).also {
    if (it != node) {
      println("Deprodified a ${node.kind}: $it")
    }
  }
}

private class Deprodifier(val producible: Set<String>) : AstTransformer() {
  var inProd: Boolean = false

  override fun <P : PetsNode?> transform(node: P): P = when {
    node is ProductionBox<*> -> {
      require(!inProd)
      inProd = true
      transform(node.extract()).also { inProd = false }
    }

    inProd && node is TypeExpression && node.className in producible -> PRODUCTION.type.copy(
        specializations = listOf(node)
    )

    else -> super.transform(node)
  } as P
}
