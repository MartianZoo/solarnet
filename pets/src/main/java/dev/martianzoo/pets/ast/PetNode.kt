package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.ast.Instruction.Gain
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/** An API object that can be represented as PETS source code. */
public sealed class PetNode {
  /**
   * A string describing the high-level element kind, not the specific node type. For example, the
   * [Gain] class returns `"Instruction"`, not `"Gain"`.
   */
  public abstract val kind: KClass<out PetNode>

  protected fun groupPartIfNeeded(part: PetNode) =
      if (part.safeToNestIn(this)) "$part" else "($part)"

  /**
   * Can this node be nested inside [container] as-is, without inserting parentheses? Unless
   * overridden, this returns `true` when this node has the larger [precedence].
   */
  protected open fun safeToNestIn(container: PetNode) = precedence() > container.precedence()

  /**
   * Returns an arbitrary integer for the sole purpose of determining [safeToNestIn] behavior. For
   * example, [Instruction.Multi] returns a very low number, since *anything* else binds more
   * tightly than it. [Metric]s return high values, since essentially everything after the `/` of an
   * instruction is part of the metric.
   */
  protected open fun precedence(): Int = Int.MAX_VALUE

  /**
   * Invokes [Visitor.maybeVisit] for each immediate child node of this [PetNode] (but not for this
   * node).
   */
  protected abstract fun visitChildren(visitor: Visitor)

  /**
   * Passes every node of a subtree to [visitor], including this. [visitor] should return `true` if
   * it wants child subtrees to be traversed.
   */
  public fun visitDescendants(visitor: (PetNode) -> Boolean): Unit = Visitor(visitor).visit(this)

  /** Returns the total number of [PetNode]s in this subtree, including this. */
  public fun descendantCount(): Int {
    var count = 0
    visitDescendants {
      count++
      true
    }
    return count
  }

  /** Returns every child node (including this) that is of type [P]. */
  public inline fun <reified P : PetNode> descendantsOfType(): List<P> = descendantsOfType(P::class)

  /** Non-reified form of [descendantsOfType]. */
  public fun <P : PetNode> descendantsOfType(type: KClass<P>): List<P> {
    val found = mutableListOf<P?>()
    visitDescendants {
      found += type.safeCast(it)
      true
    }
    return found.filterNotNull()
  }

  /**
   * Does this subtree contain [node], at any depth? A depth of zero counts; that is, if [node] *is*
   * this node, `true` is returned.
   */
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
    public fun visit(nodes: Iterable<PetNode?>) = nodes.forEach(::maybeVisit)
    public fun visit(vararg nodes: PetNode?): Unit = visit(nodes.toList())

    private fun maybeVisit(node: PetNode?) {
      node?.let { if (shouldContinue(it)) it.visitChildren(this) }
    }
  }

  public companion object {
    /**
     * Returns this tree with each node matching [from] replaced with [to]. Note that [from] and
     * [to] are treated as atomic units, not descended into.
     */
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
