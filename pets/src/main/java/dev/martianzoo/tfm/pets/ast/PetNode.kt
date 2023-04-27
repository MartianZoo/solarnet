package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.noOp
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
  public inline fun <reified P : PetNode> descendantsOfType(): List<P> = descendantsOfType(P::class)

  /** Returns every child node (including `this`) that is of type [P]. */
  public fun <P : PetNode> descendantsOfType(type: KClass<P>): List<P> {
    val found = mutableListOf<P?>()
    visitDescendants {
      found += type.safeCast(it)
      true
    }
    return found.filterNotNull()
  }

  public operator fun contains(node: PetNode): Boolean {
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

  public companion object {
    public fun <P : PetNode> P.raw(): P = TransformNode.wrap(this, RAW)
    public fun <P : PetNode> P.unraw(): P = TransformNode.unwrap(this, RAW)

    // Instead of making `replaceAll` a regular method of PetNode, this trick allows it to return
    // the same type as the receiver
    public fun <P : PetNode> P.replaceAll(from: PetNode, to: PetNode): P =
        replacer(from, to).transform(this)

    public fun replacer(from: PetNode, to: PetNode): PetTransformer =
        if (from == to) noOp() else Replacer(from, to)

    private class Replacer(val from: PetNode, val to: PetNode) : PetTransformer() {
      override fun <Q : PetNode> transform(node: Q): Q =
          if (node == from) {
            @Suppress("UNCHECKED_CAST")
            to as Q
          } else {
            transformChildren(node)
          }
    }
  }
}
