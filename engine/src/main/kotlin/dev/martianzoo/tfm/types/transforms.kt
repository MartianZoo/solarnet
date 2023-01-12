package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

internal fun <P : PetNode> applyDefaultsIn(node: P, loader: PetClassLoader): P {
  return Defaulter(loader).transform(node)
}

private class Defaulter(val loader: PetClassLoader) : PetNodeVisitor() {
  override fun <P : PetNode?> transform(node: P): P {
    val rewritten: PetNode? = when (node) {
      null -> null
      THIS.baseType -> node // leave This alone!

      is Gain -> {
        val writtenType = node.qe.expression
        if (writtenType !is GenericTypeExpression) {
          node
        } else {
          val petClass = loader.load(writtenType.className)
          val defaults = petClass.defaults
          val newTypeExpr = applyDefaultSpecs(writtenType, petClass, defaults.gainOnlyDependencies)
          node.copy(
              node.qe.copy(expression = transform(newTypeExpr)),
              node.intensity ?: defaults.gainIntensity,
          )
        }
      }

      is GenericTypeExpression -> {
        val petClass = loader.load(node.className)
        // TODO should we be recursing?
        applyDefaultSpecs(node, petClass, petClass.defaults.allCasesDependencies)
      }

      else -> super.transform(node)
    }

    @Suppress("UNCHECKED_CAST") return rewritten as P
  }

  private fun applyDefaultSpecs(
      original: GenericTypeExpression,
      petClass: PetClass,
      defaultDeps: DependencyMap,
  ): GenericTypeExpression {
    val explicitStatedDeps = petClass.resolveSpecializations(original.specs)
    val mergedDeps = explicitStatedDeps.overlayOn(defaultDeps)

    // TODO: a little weird that we're going backwards here?
    return PetGenericType(petClass, mergedDeps, original.refinement).toTypeExpressionFull()
  }
}
