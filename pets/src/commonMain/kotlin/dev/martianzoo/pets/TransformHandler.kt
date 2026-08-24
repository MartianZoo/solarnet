package dev.martianzoo.pets

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.TransformNode

/** Rewrites the contents of one explicitly marked Pets transform. */
public fun interface TransformHandler {
  /** Returns a replacement for the unwrapped [inner] tree, or null to preserve the transform. */
  public fun transform(inner: PetNode): PetNode?

  public companion object {
    /** Creates a transformer that dispatches marked syntax to [handlers] by transform kind. */
    public fun dispatcher(handlers: Map<String, TransformHandler>): PetTransformer =
        Dispatcher(handlers)
  }

  private class Dispatcher(private val handlers: Map<String, TransformHandler>) : PetTransformer() {
    private val activeKinds = mutableSetOf<String>()

    override fun transformNode(node: PetNode): PetNode {
      if (node !is TransformNode<*>) return transformChildren(node)

      val kind = node.transformKind
      val handler = handlers[kind] ?: return transformChildren(node)
      if (!activeKinds.add(kind)) throw PetSyntaxException("$kind transforms cannot be nested")
      return try {
        val inner = transformWithoutKindCheck(node.extract())
        val replacement = handler.transform(inner) ?: return rewrap(node, inner, kind)
        if (!accepts(node, replacement)) {
          throw PetSyntaxException(
              "$kind handler returned ${replacement.kind.simpleName} for ${inner.kind.simpleName}"
          )
        }
        transformWithoutKindCheck(replacement)
      } finally {
        activeKinds.remove(kind)
      }
    }

    private fun accepts(zone: TransformNode<*>, replacement: PetNode): Boolean =
        when (zone) {
          is Cost.Transform -> replacement is Cost
          is Instruction.Transform -> replacement is InstructionTree
          is Metric.Transform -> replacement is Metric
          is Requirement.Transform -> replacement is Requirement
          is Trigger.Transform -> replacement is Trigger
          else -> false
        }

    private fun rewrap(zone: TransformNode<*>, inner: PetNode, kind: String): PetNode =
        when (zone) {
          is Cost.Transform -> TransformNode.wrap(inner as Cost, kind)
          is Instruction.Transform -> TransformNode.wrap(inner as InstructionTree, kind)
          is Metric.Transform -> TransformNode.wrap(inner as Metric, kind)
          is Requirement.Transform -> TransformNode.wrap(inner as Requirement, kind)
          is Trigger.Transform -> TransformNode.wrap(inner as Trigger, kind)
          else -> error("Unknown transform node: $zone")
        }
  }
}
