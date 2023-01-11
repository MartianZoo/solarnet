package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetNodeVisitor
import kotlin.reflect.KClass

/**
 * An API object that can be represented as PETS source code.
 */
sealed class PetNode {
  abstract val kind: String

  fun nodeCount(): Int {
    class NodeCounter : PetNodeVisitor() {
      var count = 0
      override fun <P : PetNode?> transform(node: P): P {
        if (node != null) count++
        return super.transform(node)
      }
    }

    val nc = NodeCounter()
    nc.transform(this)
    return nc.count
  }

  inline fun <reified P : PetNode> childNodesOfType(): Set<P> =
      childNodesOfType(P::class)

  fun <P : PetNode> childNodesOfType(type: KClass<P>): Set<P> {
    val found = mutableSetOf<P>()

    class Finder : PetNodeVisitor() {
      override fun <Q : PetNode?> transform(node: Q): Q {
        if (type.isInstance(node)) {
          found += node as P
        }
        return super.transform(node)
      }
    }
    Finder().transform(this)
    return found
  }

  fun groupPartIfNeeded(part: PetNode) = if (part.shouldGroupInside(this)) "($part)" else "$part"

  open fun shouldGroupInside(container: PetNode) = precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  interface GenericTransform<P : PetNode> {
    val transform: String
    fun extract(): P
  }
}
