package dev.martianzoo.pets.ast

import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Instruction.Transform

/**
 * Several kinds of nodes (instructions, requirements, etc.) support "transforms", the common
 * example being `PROD[...]`. Each of those (e.g., [Instruction.Transform]) implements this
 * interface.
 */
public interface TransformNode<P : PetNode> {
  /** The string that identifies this kind of transform, e.g. `"PROD"`. */
  public val transformKind: String

  /** The node this transform node is wrapping. */
  public fun extract(): P

  public companion object {
    internal fun wrap(node: Cost, kind: String): Cost =
        wrap(node, kind) { Cost.Transform(it, kind) }

    internal fun wrap(node: InstructionTree, kind: String): InstructionTree =
        wrap(node, kind) { Transform(it, kind) }

    internal fun wrap(node: Instruction, kind: String): Instruction =
        wrap(node, kind) { Transform(it, kind) }

    internal fun wrap(node: Metric, kind: String): Metric =
        wrap(node, kind) { Metric.Transform(it, kind) }

    internal fun wrap(node: Requirement, kind: String): Requirement =
        wrap(node, kind) { Requirement.Transform(it, kind) }

    internal fun wrap(node: Trigger, kind: String): Trigger =
        wrap(node, kind) { Trigger.Transform(it, kind) }

    private fun <P : PetNode> wrap(node: P, kind: String, wrapper: (P) -> P): P {
      fun isThisKind(candidate: PetNode) = (candidate as? TransformNode<*>)?.transformKind == kind

      if (isThisKind(node)) return node
      require(node.descendantsOfType<PetNode>().none(::isThisKind)) {
        "already has a $kind component: $node"
      }
      return wrapper(node)
    }
  }
}
