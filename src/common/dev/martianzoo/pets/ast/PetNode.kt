package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.ast.Instruction.Gain
import kotlin.reflect.KClass

/**
 * An API object that can be represented as Pets source code — any piece of Pets syntax, where a
 * [PetElement] is one of the six major kinds an author writes.
 *
 * Rendering is normalized, never verbatim: whitespace is discarded, each node is written in its own
 * canonical form, and enough parentheses are inserted that re-parsing the result yields the same
 * node. "Enough" is not "the fewest" — see [precedence] and [safeToNestIn]. Every rule of
 * [the language specification](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md)
 * saying an element *round-trips* means exactly that: rendering then re-parsing is the identity —
 * [rule L1-11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations)
 * for a declaration,
 * [L4-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements),
 * [L5-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics),
 * [L6-13](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)
 * and
 * [L8-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)
 * for the elements.
 */
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
   * Returns an arbitrary integer for the sole purpose of determining [safeToNestIn] behavior. These
   * values rank the language's operators but are not themselves part of the language: only their
   * relative order matters. For example, [InstructionGroup] returns a very low number, since
   * *anything* else binds more tightly than it. [Metric]s return high values, since essentially
   * everything after the `/` of an instruction is part of the metric.
   */
  protected open fun precedence(): Int = Int.MAX_VALUE

  /**
   * Invokes [Visitor.maybeVisit] for each immediate child node of this [PetNode] (but not for this
   * node).
   */
  protected abstract fun visitChildren(visitor: Visitor)

  /** Immediate children in the same stable order used by descendant traversal. */
  public fun immediateChildren(): List<PetNode> = buildList {
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

    internal fun visit(node: PetNode?): Unit = maybeVisit(node)

    internal fun visit(first: PetNode?, second: PetNode?) {
      maybeVisit(first)
      maybeVisit(second)
    }

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
