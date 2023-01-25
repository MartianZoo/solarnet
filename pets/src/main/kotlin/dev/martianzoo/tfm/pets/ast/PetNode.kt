package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetVisitor
import dev.martianzoo.tfm.pets.PetVisitor.Companion.transform
import kotlin.reflect.KClass

/** An API object that can be represented as PETS source code. */
sealed class PetNode {
  abstract val kind: String

  fun groupPartIfNeeded(part: PetNode) = if (part.shouldGroupInside(this)) "($part)" else "$part"

  open fun shouldGroupInside(container: PetNode) = precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  interface GenericTransform<P : PetNode> {
    val transform: String
    fun extract(): P
  }
}

fun countNodesInTree(node: PetNode): Int {
  var count = 0
  node.transform(object : PetVisitor() {
    override fun <P : PetNode> doTransform(node: P): P {
      count++
      return defaultTransform(node)
    }
  })
  return count
}

inline fun <reified P : PetNode> childNodesOfType(node: PetNode): Set<P> =
    childNodesOfType(P::class, node)

fun <P : PetNode> childNodesOfType(type: KClass<P>, node: PetNode): Set<P> {
  val found = mutableSetOf<P>()

  node.transform(object : PetVisitor() {
    override fun <Q : PetNode> doTransform(node: Q): Q {
      if (type.isInstance(node)) found += node as P
      return defaultTransform(node)
    }
  })
  return found
}
