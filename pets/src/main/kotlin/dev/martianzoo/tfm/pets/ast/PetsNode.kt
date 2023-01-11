package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.AstTransformer
import kotlin.reflect.KClass

/**
 * An API object that can be represented as PETS source code.
 */
sealed class PetsNode {
  abstract val kind: String

  fun nodeCount(): Int {
    class NodeCounter : AstTransformer() {
      var count = 0
      override fun <P : PetsNode?> transform(node: P): P {
        if (node != null) count++
        return super.transform(node)
      }
    }

    val nc = NodeCounter()
    nc.transform(this)
    return nc.count
  }

  inline fun <reified P : PetsNode> childNodesOfType(): Set<P> =
      childNodesOfType(P::class)

  fun <P : PetsNode> childNodesOfType(type: KClass<P>): Set<P> {
    val found = mutableSetOf<P>()

    class Finder : AstTransformer() {
      override fun <Q : PetsNode?> transform(node: Q): Q {
        if (type.isInstance(node)) {
          found += node as P
        }
        return super.transform(node)
      }
    }
    Finder().transform(this)
    return found
  }

  fun groupPartIfNeeded(part: PetsNode) = if (part.shouldGroupInside(this)) "($part)" else "$part"

  open fun shouldGroupInside(container: PetsNode) = precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  interface GenericTransform<P : PetsNode> {
    val transform: String
    fun extract(): P
  }
}
