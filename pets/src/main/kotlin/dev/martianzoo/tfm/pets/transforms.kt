package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialComponent.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.SpecialComponent.USE_ACTION
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.PetsNode.ProductionBox
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.toSetStrict

internal fun actionToEffect(action: Action, index: Int) : Effect {
  val merged = if (action.cost == null) {
    action.instruction
  } else {
    Instruction.Then(listOf(action.cost.toInstruction(), action.instruction))
  }
  return Effect(PetsParser.parse("$USE_ACTION${index + 1}<$THIS>"), merged)
}

internal fun actionsToEffects(actions: Collection<Action>) =
    actions.withIndex().map { (i, act) -> actionToEffect(act, i) }

internal fun immediateToEffect(instr: Instruction) = Effect(OnGain(THIS.type), instr)

internal fun <P : PetsNode> replaceTypesIn(node: P, from: TypeExpression, to: TypeExpression) =
    TypeReplacer(from, to).s(node)

private class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : NodeVisitor() {
  override fun <P : PetsNode?> s(node: P) =
      if (node == from) {
        @Suppress("UNCHECKED_CAST")
        to as P
      } else {
        super.s(node)
      }
}

internal fun <P : PetsNode> spellOutQes(node: P) = QeSpellerOuter.s(node)

private object QeSpellerOuter : NodeVisitor() {
  override fun <P : PetsNode?> s(node: P): P {
    return when {
      node is QuantifiedExpression -> node.explicit() as P
      else -> super.s(node)
    }
  }
}

internal fun <P : PetsNode> deprodify(node: P, producibleClassNames: Set<String>): P {
  return Deprodifier(producibleClassNames).s(node)
}

private class Deprodifier(val producible: Set<String>) : NodeVisitor() {
  var inProd : Boolean = false

  override fun <P : PetsNode?> s(node: P): P =
    when {
      node is ProductionBox<*> -> {
        require(!inProd)
        inProd = true
        s(node.extract()).also { inProd = false }
      }
      inProd && node is TypeExpression && node.className in producible ->
        PRODUCTION.type.copy(specializations=listOf(node))

      else -> super.s(node)
    } as P
}
