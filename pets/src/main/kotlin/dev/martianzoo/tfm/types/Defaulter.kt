package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.NodeVisitor
import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun <P : PetsNode> applyDefaultsIn(node: P, table: PetClassTable) =
    Defaulter(table).s(node)

private class Defaulter(val table: PetClassTable): NodeVisitor() {
  override fun <P : PetsNode?> s(node: P): P {
    return when {
      node is TypeExpression && node.isClassOnly() -> {
        val defs = table[node.className].defaults
        defs.typeExpression?.copy(className = node.className) ?: node
      }

      node is QuantifiedExpression -> node.copy(
          s(node.typeExpression ?: MEGACREDIT.type),
          node.scalar ?: 1
      )

      node is Instruction.Gain -> {
        val te = node.qe.explicit().typeExpression!!
        val defs = table[te.className].defaults

        val gain = if (te.isClassOnly()) {
          node.copy(
              qe = node.qe.copy(
                  typeExpression = s(defs.typeExpression?.copy(className = te.className) ?: te)
              )
          )
        } else {
          node
        }
        gain.copy(intensity = listOfNotNull(gain.intensity, defs.gainIntensity).firstOrNull())
      }

      node is Instruction.Remove -> {
        val te = node.qe.explicit().typeExpression!!
        val defs = table[te.className].defaults

        val remove = if (te.isClassOnly()) {
          node.copy(
              qe = node.qe.copy(
                  typeExpression = s(defs.typeExpression?.copy(className = te.className) ?: te)
              )
          )
        } else {
          node
        }
        remove.copy(
            intensity = listOfNotNull(
                remove.intensity,
                defs.removeIntensity
            ).firstOrNull()
        )
      }

      else -> node
    } as P
  }
}
