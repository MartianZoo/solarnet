package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.ast.Instruction.Gain
import kotlin.reflect.KClass

/** An API object that can be represented as PETS source code. */
public sealed class PetNode {
  /**
   * This node's primary API kind: the stable abstraction clients should rely on, rather than its
   * concrete implementation type. For example, a [Gain] has kind [Instruction], not [Gain]. A node
   * may also be accepted through a broader kind such as [InstructionTree].
   */
  // TODO: Contract this temporary tfm-canon round-trip-test seam.
  public abstract val kind: KClass<out PetNode>

  protected fun groupPartIfNeeded(part: PetNode): String =
      if (part.safeToNestIn(this)) "$part" else "($part)"

  /**
   * Can this node be nested inside [container] as-is, without inserting parentheses? Unless
   * overridden, this returns `true` when this node has the larger [precedence].
   */
  protected open fun safeToNestIn(container: PetNode): Boolean =
      precedence() > container.precedence()

  /**
   * Returns an arbitrary integer for the sole purpose of determining [safeToNestIn] behavior. For
   * example, [InstructionGroup] returns a very low number, since *anything* else binds more tightly
   * than it. [Metric]s return high values, since essentially everything after the `/` of an
   * instruction is part of the metric.
   */
  protected open fun precedence(): Int = Int.MAX_VALUE

  /**
   * Invokes [Visitor.maybeVisit] for each immediate child node of this [PetNode] (but not for this
   * node).
   */
  protected abstract fun visitChildren(visitor: Visitor)

  /** Immediate children in the same stable order used by descendant traversal. */
  internal fun immediateChildren(): List<PetNode> = buildList {
    visitChildren(
        Visitor {
          add(it)
          false
        }
    )
  }

  /**
   * Passes every node of a subtree to [visitor], including this. [visitor] should return `true` if
   * it wants child subtrees to be traversed.
   */
  public fun visitDescendants(visitor: (PetNode) -> Boolean): Unit = Visitor(visitor).visit(this)

  /** Returns the total number of [PetNode]s in this subtree, including this. */
  internal fun descendantCount(): Int {
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
  public fun <P : PetNode> descendantsOfType(type: KClass<P>): List<P> = buildList {
    visitDescendants {
      @Suppress("UNCHECKED_CAST")
      if (type.isInstance(it)) {
        add(it as P)
      }
      true
    }
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
  protected class Visitor(private val shouldContinue: (PetNode) -> Boolean) {
    internal fun visit(nodes: Iterable<PetNode?>): Unit = nodes.forEach(::maybeVisit)

    internal fun visit(vararg nodes: PetNode?): Unit = visit(nodes.toList())

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
      override fun transformNode(node: PetNode): PetNode =
          if (node == from) {
            to
          } else {
            transformChildren(node)
          }
    }
  }
}
