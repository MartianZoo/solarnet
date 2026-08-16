package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.tfm.data.TfmClasses.PROD
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.ClassTable

public object Prod {
  public fun deprodify(classTable: ClassTable): PetTransformer {
    return deprodify(findResourceClassNames(classTable))
  }

  internal fun findResourceClassNames(classTable: ClassTable): Set<ClassName> {
    val standardResource = classTable.findActiveClass(STANDARD_RESOURCE) ?: return emptySet()
    if (!classTable.isActive(PRODUCTION)) return emptySet()
    return standardResource.allSubclasses().flatMapTo(mutableSetOf()) {
      setOf(it.className)
    }
  }

  /**
   * Rewrites `PROD` boxes, recognizing exactly the full or short class names in
   * [resourceClassNames].
   */
  public fun deprodify(resourceClassNames: Set<ClassName>): PetTransformer {
    if (resourceClassNames.isEmpty()) return PetTransformer.noOp()

    return object : PetTransformer() {
      private var inProd = false

      override fun transformNode(node: PetNode): PetNode {
        val rewritten: PetNode =
            when {
              node is TransformNode<*> && node.transformKind == PROD -> {
                if (inProd) throw PetSyntaxException("PROD boxes cannot be nested")
                inProd = true
                val inner =
                    try {
                      transformNode(node.extract())
                    } finally {
                      inProd = false
                    }
                if (inner == node.extract()) {
                  throw PetSyntaxException("No standard resources found in PROD box: $inner")
                }
                inner
              }
              inProd && node is Expression && node.className == CLASS -> node
              inProd && node is Expression && node.className in resourceClassNames ->
                  PRODUCTION.of(node.arguments + node.className.classExpression())
              else -> transformChildren(node)
            }
        return rewritten
      }
    }
  }
}
