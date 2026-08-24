package dev.martianzoo.tfm.data

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.ClassTable

public object Prod {
  /** Creates the `PROD[...]` handler for one active class table. */
  public fun handler(classTable: ClassTable): TransformHandler =
      handler(findResourceClassNames(classTable))

  private fun findResourceClassNames(classTable: ClassTable): Set<ClassName> {
    val standardResource = classTable.findActiveClass(STANDARD_RESOURCE) ?: return emptySet()
    if (!classTable.isActive(PRODUCTION)) return emptySet()
    return classTable.allSubclasses(standardResource).mapTo(mutableSetOf()) { it.className }
  }

  private fun handler(resourceClassNames: Set<ClassName>): TransformHandler {
    if (resourceClassNames.isEmpty()) return TransformHandler { null }
    val lowerer = resourceLowerer(resourceClassNames)
    return TransformHandler { inner ->
      lowerer.transformWithoutKindCheck(inner).also { lowered ->
        if (lowered == inner) {
          throw PetSyntaxException("No standard resources found in PROD box: $inner")
        }
      }
    }
  }

  private fun resourceLowerer(resourceClassNames: Set<ClassName>): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression && node.className == CLASS) return node
          if (node !is Expression || node.className !in resourceClassNames) {
            return transformChildren(node)
          }
          // Production represents its resource kind with a Class dependency, so the resource
          // selector's refinement belongs on that represented class after lowering.
          val resourceClass = node.className.classExpression().copy(refinement = node.refinement)
          return PRODUCTION.of(node.arguments + resourceClass)
        }
      }
}
