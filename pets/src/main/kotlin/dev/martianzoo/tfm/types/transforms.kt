package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

fun <P : PetsNode> applyDefaultsIn(node: P, table: PetClassTable): P {
  return Defaulter(table).transform(node).also {
    println("2. applied defaults to a ${node.kind}: $it")
  }
}

private class Defaulter(val table: PetClassTable) : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P): P {
    val rewritten: PetsNode? = when (node) {
      null -> null
      THIS.type -> node // leave This alone!

      is Gain -> {
        val writtenType = node.qe.expression
        if (writtenType !is GenericTypeExpression) {
          node
        } else {
          val petClass = table[writtenType.className]
          val defaults = petClass.defaults
          val newTypeExpr = applyDefaultSpecs(
              writtenType,
              petClass,
              defaults.gainOnlyDependencies
          )
          node.copy(
              node.qe.copy(expression = transform(newTypeExpr)),
              node.intensity ?: defaults.gainIntensity
          )
        }
      }

      is GenericTypeExpression -> {
        val petClass = table[node.className]
        // TODO should we be recursing?
        applyDefaultSpecs(node, petClass, petClass.defaults.allCasesDependencies)
      }

      else -> super.transform(node)
    }

    @Suppress("UNCHECKED_CAST") return rewritten as P
  }

  fun applyDefaultSpecs(
      original: GenericTypeExpression,
      petClass: PetClass,
      defaultDeps: DependencyMap,
  ): GenericTypeExpression {
    val explicitStatedDeps = petClass.resolveSpecializations(original.specs)
    val mergedDeps = explicitStatedDeps.overlayOn(defaultDeps)

    //// TODO: a little weird that we're going backwards here?
    return PetGenericType(petClass, mergedDeps).toTypeExpressionFull()
  }
}
