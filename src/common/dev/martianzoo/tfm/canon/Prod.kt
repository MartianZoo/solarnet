package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.Expression.Refinement.And
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.canon.TfmClasses.STANDARD_RESOURCE

internal object Prod {
  /** Creates the `PROD[...]` handler for one game class table. */
  public fun handler(classTable: ClassTable): TransformHandler =
      handler(findResourceClassNames(classTable))

  private fun findResourceClassNames(classTable: ClassTable): Set<ClassName> {
    val standardResource = classTable.findInhabitedClass(STANDARD_RESOURCE) ?: return emptySet()
    if (!classTable.isInhabited(PRODUCTION)) return emptySet()
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
          return PRODUCTION.of(node.arguments + node.toClassSelector())
        }
      }

  private fun Expression.toClassSelector(
      expectedArguments: List<Expression> = arguments
  ): Expression {
    if (arguments != expectedArguments) {
      throw PetSyntaxException(
          "PROD cannot represent a resource difference with different dependencies: $this"
      )
    }
    return className
        .classExpression()
        .copy(refinement = refinement?.let { toClassRefinement(it, expectedArguments) })
  }

  private fun toClassRefinement(
      refinement: Refinement,
      resourceArguments: List<Expression>,
  ): Refinement =
      when (refinement) {
        is Has -> refinement
        is Not -> Not(refinement.excluded.toClassSelector(resourceArguments))
        is And ->
            Refinement.create(
                refinement.refinements.map { toClassRefinement(it, resourceArguments) }
            )
      }
}
