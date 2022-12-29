package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun <P : PetsNode> applyDefaultsIn(node: P, table: PetClassTable) =
    Defaulter(table).transform(node)

private class Defaulter(val table: PetClassTable): AstTransformer() {
  override fun <P : PetsNode?> transform(node: P): P {
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
            node.qe.copy(transform(newTypeExpr)),
            node.intensity ?: defaults.gainIntensity)
      }
      else -> super.transform(node)
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
