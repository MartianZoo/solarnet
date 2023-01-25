package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass
import kotlin.reflect.cast

public fun visit(node: PetNode, visitor: (PetNode) -> Boolean) = PetVisitor(visitor).visit(node)

public fun visitAll(node: PetNode, visitor: (PetNode) -> Unit) =
    visit(node) {
      visitor(it)
      true
    }

public fun countNodesInTree(root: PetNode): Int {
  var count = 0
  visitAll(root) { count++ }
  return count
}

public inline fun <reified P : PetNode> childNodesOfType(root: PetNode): Set<P> =
    childNodesOfType(P::class, root)

public fun <P : PetNode> childNodesOfType(type: KClass<P>, root: PetNode): Set<P> {
  val found = mutableSetOf<P>()
  visitAll(root) { if (type.isInstance(it)) found += type.cast(it) }
  return found
}

public class PetVisitor(val shouldContiue: (PetNode) -> Boolean) {
  fun visit(node: PetNode?) {
    if (node != null) {
      if (shouldContiue(node)) {
        node.visitChildren(this)
      }
    }
  }

  fun visit(nodes: Iterable<PetNode?>) = nodes.forEach(::visit)
  fun visit(vararg nodes: PetNode?): Unit = visit(nodes.toList())
}
