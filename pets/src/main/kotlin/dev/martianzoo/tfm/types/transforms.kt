package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.NodeVisitor
import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun <P : PetsNode> applyDefaultsIn(node: P, table: PetClassTable) =
    Defaulter(table).s(node)

private class Defaulter(val table: PetClassTable): NodeVisitor() {
  override fun <P : PetsNode?> s(node: P): P {
    return when {
      node == THIS.type -> node

      node is TypeExpression && node.isClassOnly() -> {
        val defs = table[node.className].defaults
        defs.typeExpression?.copy(className = node.className) ?: node
      }

      node is Instruction.Gain -> {
        val te = node.qe.typeExpression!!
        if (te == THIS.type) return node // TODO hmmmmm
        val defs = table[te.className].defaults

        val gain = if (te.isClassOnly()) {
          val fixedTe = s(defs.typeExpression?.copy(className = te.className) ?: te)
          node.copy(qe = node.qe.copy(fixedTe))
        } else {
          node
        }
        gain.copy(intensity = listOfNotNull(gain.intensity, defs.gainIntensity).firstOrNull())
      }

      node is Instruction.Remove -> {
        val te = node.qe.typeExpression!!
        if (te == THIS.type) return node // TODO hmmmmm
        val defs = table[te.className].defaults

        val remove = if (te.isClassOnly()) {
          val fixedTe = s(defs.typeExpression?.copy(className = te.className) ?: te)
          node.copy(qe = node.qe.copy(typeExpression = fixedTe))
        } else {
          node
        }
        remove.copy(intensity = listOfNotNull(remove.intensity, defs.removeIntensity).firstOrNull())
      }

      else -> super.s(node)
    } as P
  }
}
