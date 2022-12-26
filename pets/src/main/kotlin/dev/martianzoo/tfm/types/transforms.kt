package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.NodeVisitor
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun <P : PetsNode> applyDefaultsIn(node: P, table: PetClassTable) =
    Defaulter(table).s(node)

private class Defaulter(val table: PetClassTable): NodeVisitor() {
  override fun <P : PetsNode?> s(node: P): P {
    val rewritten: PetsNode? = when(node) {
      null -> null
      THIS.type -> node
      is TypeExpression -> {
        val petClass = table[node.className]
        // TODO should we be recursing?
        applyDefaultSpecs(node, petClass, petClass.defaults.allDeps, petClass.defaults.allReqs)
      }
      is Gain -> {
        val statedTypeExpr = node.qe.typeExpression!!
        val petClass = table[statedTypeExpr.className]
        val defaults = petClass.defaults
        val newTypeExpr = applyDefaultSpecs(
            statedTypeExpr,
            petClass,
            defaults.gainDeps,
            defaults.gainReqs)
        node.copy(
            node.qe.copy(s(newTypeExpr)),
            node.intensity ?: defaults.gainIntensity)
      }
      else -> super.s(node)
    }

    @Suppress("UNCHECKED_CAST")
    return rewritten as P
  }

  internal fun applyDefaultSpecs(
      original: TypeExpression,
      petClass: PetClass,
      defaultDeps: DependencyMap,
      reqs: Requirement?
  ): TypeExpression {
    val explicitStatedDeps =
        petClass.resolveSpecializations(original.specializations)
    val mergedDeps = explicitStatedDeps.overlayOn(defaultDeps)

    //// TODO: a little weird that we're going backwards here?
    return PetType(petClass, mergedDeps).toTypeExpression()
  }
}
