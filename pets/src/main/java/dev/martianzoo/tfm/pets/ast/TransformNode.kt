package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Effect.Trigger

/**
 * Several kinds of nodes (instructions, requirements, etc.) support "transforms", the common
 * example being `PROD[...]`. Each of those (e.g., [Instruction.Transform]) implements this
 * interface.
 */
interface TransformNode<P : PetNode> {
  /** The string that identifies this kind of transform, e.g. `"PROD"`. */
  val transformKind: String

  /** The node this transform node is wrapping. */
  fun extract(): P

  companion object {
    /** Returns [node] wrapped as a [TransformNode] *if* that kind of node is supported. */
    fun <P : PetNode?> wrap(node: P, kind: String): P {
      fun <P : PetNode?> isThisKind(node: P) = (node as? TransformNode<*>)?.transformKind == kind

      if (node == null || isThisKind(node)) return node
      require(node.descendantsOfType<PetNode>().none(::isThisKind)) {
        "already has a RAW component: $node"
      }

      val wrapped =
          when (node) {
            is Cost -> Cost.Transform(node, kind)
            is Instruction -> Instruction.Transform(node, kind)
            is Metric -> Metric.Transform(node, kind)
            is Requirement -> Requirement.Transform(node, kind)
            is Trigger -> Trigger.Transform(node, kind)
            is Action -> // TODO this is clunky
            node.copy(
                    cost = wrap(node.cost, kind),
                    instruction = wrap(node.instruction, kind),
                )
            is Effect ->
                node.copy(
                    trigger = wrap(node.trigger, kind),
                    instruction = wrap(node.instruction, kind),
                )
            else -> error("no Transform supported for ${node.kind}")
          }

      @Suppress("UNCHECKED_CAST") // in theory the `require` above makes this approximately safe?
      return wrapped as P
    }

    // TODO make transformer available
    fun <P : PetNode> unwrap(node: P, kind: String) = unwrapper(kind).transform(node)

    fun unwrapper(kind: String): PetTransformer {
      return object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          val result: PetNode =
              if (node is TransformNode<*> && node.transformKind == kind) {
                node.extract()
              } else {
                transformChildren(node)
              }
          @Suppress("UNCHECKED_CAST")
          return result as P
        }
      }
    }
  }
}
