package dev.martianzoo.pets.ast

import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Instruction.Transform

/**
 * A subtree marked for rewriting by a named handler, the common example being `PROD[...]`, as
 * defined by
 * [section 10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks).
 * [Rule
 * L10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
 * admits a block on an instruction, an action cost, a metric, a requirement and a trigger; each of
 * those (e.g. [Instruction.Transform]) implements this interface.
 *
 * A block whose kind has no handler is preserved verbatim, so a source may carry marks a later
 * stage will interpret ([rule
 * L10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)).
 */
public interface TransformNode<P : PetNode> {
  /**
   * The all-caps word identifying this kind of transform, e.g. `"PROD"` ([rule
   * L2-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-names)).
   */
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

    /**
     * Returns [node] wrapped in a block of kind [kind], or [node] itself if it already is one.
     * Nesting a block inside a block of the same kind is representable but not processable, so
     * [rule L10-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
     * rejects it here rather than interpreting a second mark that could only mean what the first
     * already means.
     */
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
