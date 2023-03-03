package dev.martianzoo.tfm.pets.ast

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/** An API object that can be represented as PETS source code. */
public sealed class PetNode {
  internal abstract val kind: String

  protected fun groupPartIfNeeded(part: PetNode) =
      if (part.safeToNestIn(this)) "$part" else "($part)"

  protected open fun safeToNestIn(container: PetNode) = precedence() > container.precedence()

  protected open fun precedence(): Int = Int.MAX_VALUE

  /** Invokes [Visitor.visit] for each direct child node of this [PetNode]. */
  protected abstract fun visitChildren(visitor: Visitor)

  /**
   * Passes every node of a subtree to [visitor], which returns `true` or `false` to indicate
   * whether its child subtrees should also be traversed.
   */
  public fun visitDescendants(visitor: (PetNode) -> Boolean): Unit = Visitor(visitor).visit(this)

  /** Returns the total number of [PetNode]s in this subtree. */
  public fun descendantCount(): Int {
    var count = 0
    visitDescendants {
      count++
      true
    }
    return count
  }

  /** Returns every child node (including `this`) that is of type [P]. */
  public inline fun <reified P : PetNode> descendantsOfType(): Set<P> = descendantsOfType(P::class)

  /** Returns every child node (including `this`) that is of type [P]. */
  public fun <P : PetNode> descendantsOfType(type: KClass<P>): Set<P> {
    val found = mutableSetOf<P?>()
    visitDescendants {
      found += type.safeCast(it)
      true
    }
    found -= null
    @Suppress("UNCHECKED_CAST") return found as Set<P>
  }

  public operator fun contains(node: PetNode): Boolean {
    // TODO why can't I return from inside the lambda?
    var found = false
    visitDescendants {
      if (it == node) found = true
      true
    }
    return found
  }

  /** See [PetNode.visitChildren]. */
  protected class Visitor(val shouldContinue: (PetNode) -> Boolean) {
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

  interface GenericTransform<P : PetNode> {
    val transformKind: String
    fun extract(): P
  }
}
