package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Transform
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

      override fun <P : PetNode> transform(node: P): P {
        val rewritten: PetNode =
            when {
              node is Multi -> {
                val badIndex =
                    node.instructions.indexOfFirst {
                      it is Transform && it.transformKind == PROD && it.instruction is Multi
                    }
                val xed = transformChildren(node)
                if (badIndex == -1) {
                  xed
                } else {
                  Multi.create(
                      xed.instructions.subList(0, badIndex) +
                          (xed.instructions[badIndex] as Multi).instructions +
                          xed.instructions.subList(badIndex + 1, xed.instructions.size),
                  )
                }
              }
              node is TransformNode<*> && node.transformKind == PROD -> {
                if (inProd) throw PetSyntaxException("PROD boxes cannot be nested")
                inProd = true
                val inner =
                    try {
                      transform(node.extract())
                    } finally {
                      inProd = false
                    }
                if (inner == node.extract()) {
                  throw PetSyntaxException("No standard resources found in PROD box: $inner")
                }
                inner
              }
              inProd && node is Expression && node.className == CLASS -> node
              inProd && node is Expression && node.className in resourceClassNames -> {
                // Production represents its resource kind with a Class dependency, so the
                // resource selector's refinement belongs on that represented class after lowering.
                val resourceClass =
                    node.className.classExpression().copy(refinement = node.refinement)
                PRODUCTION.of(node.arguments + resourceClass)
              }
              else -> transformChildren(node)
            }
        @Suppress("UNCHECKED_CAST")
        return rewritten as P
      }
    }
  }
}
