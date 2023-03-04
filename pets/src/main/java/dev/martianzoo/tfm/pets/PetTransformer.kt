package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.From
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.util.toSetStrict

/** Extend this to implement transformations over trees of [PetNode]s. */
public abstract class PetTransformer {
  /**
   * Returns an altered form of the [node] tree.
   *
   * Implementation notes:
   * * If you simply return [node] or some hardcoded subtree, that prevents child subtrees from
   *   being traversed.
   * * Call [transformChildren] from here to transform the subtree by transforming each of its child
   *   subtrees. You can of course either preprocess or post-process the subtree.
   * * To transform a single child subtree you can pass it to [x]. It will accept iterables of nodes
   *   or a nullable node. TODO fix this "x" situation.
   */
  public abstract fun <P : PetNode> transform(node: P): P

  protected fun <P : PetNode> transformChildren(node: P): P {
    return (node as PetNode).run {
      val rewritten =
          when (this) {
            is ClassName -> this
            is Expression -> Expression(x(className), x(arguments), x(refinement), link)
            is ScaledExpression -> ScaledExpression(scalar, x(expression))
            is Metric ->
              when (this) {
                is Metric.Count -> Metric.Count(x(scaledEx))
                is Metric.Max -> Metric.Max(x(metric), maximum)
              }

            is Requirement ->
              when (this) {
                is Requirement.Min -> Requirement.Min(x(scaledEx))
                is Requirement.Max -> Requirement.Max(x(scaledEx))
                is Requirement.Exact -> Requirement.Exact(x(scaledEx))
                is Requirement.Or -> Requirement.Or(x(requirements))
                is Requirement.And -> Requirement.And(x(requirements))
                is Requirement.Transform -> Requirement.Transform(x(requirement), transformKind)
              }

            is Instruction ->
                when (this) {
                  is Instruction.Gain -> Instruction.Gain(x(scaledEx), intensity)
                  is Instruction.Remove -> Instruction.Remove(x(scaledEx), intensity)
                  is Instruction.Per -> Instruction.Per(x(instruction), x(metric))
                  is Instruction.Gated -> Instruction.Gated(x(gate), x(instruction))
                  is Instruction.Transmute -> Instruction.Transmute(x(from), scalar)
                  is Instruction.Custom -> Instruction.Custom(functionName, x(arguments))
                  is Instruction.Then -> Instruction.Then(x(instructions))
                  is Instruction.Or -> Instruction.Or(x(instructions))
                  is Instruction.Multi -> Instruction.Multi(x(instructions))
                  is Instruction.Transform -> Instruction.Transform(x(instruction), transformKind)
                }
            is From ->
                when (this) {
                  is From.SimpleFrom -> From.SimpleFrom(x(toExpression), x(fromExpression))
                  is From.ComplexFrom -> From.ComplexFrom(x(className), x(arguments), x(refinement))
                  is From.ExpressionAsFrom -> From.ExpressionAsFrom(x(expression))
                }
            is Effect -> Effect(x(trigger), x(instruction), automatic)
            is Trigger ->
                when (this) {
                  is Trigger.OnGainOf -> Trigger.OnGainOf.create(x(expression))
                  is Trigger.OnRemoveOf -> Trigger.OnRemoveOf.create(x(expression))
                  is Trigger.ByTrigger -> Trigger.ByTrigger(x(inner), x(by))
                  is Trigger.Transform -> Trigger.Transform(x(trigger), transformKind)
                  else -> this
                }
            is Action -> Action(x(cost), x(instruction))
            is Cost ->
                when (this) {
                  is Cost.Spend -> Cost.Spend(x(scaledEx))
                  is Cost.Per -> Cost.Per(x(cost), x(metric))
                  is Cost.Or -> Cost.Or(x(costs))
                  is Cost.Multi -> Cost.Multi(x(costs))
                  is Cost.Transform -> Cost.Transform(x(cost), transformKind)
                }
          }
      @Suppress("UNCHECKED_CAST")
      rewritten as P
    }
  }

  @Suppress("UNCHECKED_CAST")
  protected fun <P : PetNode?> x(node: P): P = node?.let(::transform) as P
  protected fun <P : PetNode> x(nodes: Iterable<P>): List<P> = nodes.map(::x)
  protected fun <P : PetNode> x(nodes: Set<P>): Set<P> = x(nodes as Iterable<P>).toSetStrict()
}
