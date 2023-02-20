package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Passes every node of a subtree to [visitor], which returns `true` or `false` to indicate whether
 * its child subtrees should also be traversed.
 */
public fun visit(node: PetNode, visitor: (PetNode) -> Boolean) = PetVisitor(visitor).visit(node)

/** Passes every node of a subtree to [visitor], always traversing child subtrees. */
public fun visitAll(node: PetNode, visitor: (PetNode) -> Unit) =
    visit(node) {
      visitor(it)
      true
    }

/** Returns the total number of [PetNode]s in the given subtree. */
public fun countNodesInTree(root: PetNode): Int {
  var count = 0
  visitAll(root) { count++ }
  return count
}

/** Returns every child node of [root] (including [root] itself) that is of type [P]. */
public inline fun <reified P : PetNode> childNodesOfType(root: PetNode): Set<P> =
    childNodesOfType(P::class, root)

/** Returns every child node of [root] (including [root] itself) that is of type [P]. */
public fun <P : PetNode> childNodesOfType(type: KClass<P>, root: PetNode): Set<P> {
  val found = mutableSetOf<P>()
  visitAll(root) { if (type.isInstance(it)) found += type.cast(it) }
  return found
}

/** See [PetNode.visitChildren]. */
public class PetVisitor(val shouldContinue: (PetNode) -> Boolean) {
  fun visit(node: PetNode?) {
    if (node != null) {
      if (shouldContinue(node)) {
        node.visitChildren(this)
      }
    }
  }

  fun visit(nodes: Iterable<PetNode?>) = nodes.forEach(::visit)
  fun visit(vararg nodes: PetNode?): Unit = visit(nodes.toList())
}