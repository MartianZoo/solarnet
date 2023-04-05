package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.FromExpression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar
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
   */
  public abstract fun <P : PetNode> transform(node: P): P

  protected fun <P : PetNode> transformChildren(node: P): P {
    @Suppress("UNCHECKED_CAST")
    fun <P : PetNode?> x(node: P): P = node?.let(::transform) as P
    fun <P : PetNode> x(nodes: Iterable<P>): List<P> = nodes.map(::x)
    fun <P : PetNode> x(nodes: Set<P>): Set<P> = x(nodes as Iterable<P>).toSetStrict()

    return (node as PetNode).run {
      val rewritten =
          when (this) {
            is ClassName -> this
            is Expression -> Expression(x(className), x(arguments), x(refinement))
            is ScaledExpression -> ScaledExpression(x(scalar), x(expression))
            is Scalar -> this
            is Metric ->
                when (this) {
                  is Metric.Count -> Metric.Count(x(expression))
                  is Metric.Scaled -> Metric.Scaled(unit, x(metric))
                  is Metric.Max -> Metric.Max(x(metric), maximum)
                  is Metric.Plus -> Metric.Plus(x(metrics))
                  is Metric.Transform -> Metric.Transform(x(metric), transformKind)
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
                  is Instruction.NoOp -> this
                  is Instruction.Gain -> Instruction.Gain(x(scaledEx), intensity)
                  is Instruction.Remove -> Instruction.Remove(x(scaledEx), intensity)
                  is Instruction.Transmute -> Instruction.Transmute(x(fromEx), x(scalar), intensity)
                  is Instruction.Per -> Instruction.Per(x(instruction), x(metric))
                  is Instruction.Gated -> Instruction.Gated(x(gate), mandatory, x(instruction))
                  is Instruction.Custom ->
                      Instruction.Custom(functionName, x(arguments), multiplier)
                  is Instruction.Then -> Instruction.Then(x(instructions))
                  is Instruction.Or -> Instruction.Or(x(instructions))
                  is Instruction.Multi -> Instruction.Multi(x(instructions))
                  is Instruction.Transform -> Instruction.Transform(x(instruction), transformKind)
                }
            is FromExpression ->
                when (this) {
                  is FromExpression.SimpleFrom ->
                      FromExpression.SimpleFrom(x(toExpression), x(fromExpression))
                  is FromExpression.ComplexFrom ->
                      FromExpression.ComplexFrom(x(className), x(arguments), x(refinement))
                  is FromExpression.ExpressionAsFrom ->
                      FromExpression.ExpressionAsFrom(x(expression))
                }
            is Effect -> Effect(x(trigger), x(instruction), automatic)
            is Trigger ->
                when (this) {
                  is Trigger.OnGainOf -> Trigger.OnGainOf.create(x(expression))
                  is Trigger.OnRemoveOf -> Trigger.OnRemoveOf.create(x(expression))
                  is Trigger.ByTrigger -> Trigger.ByTrigger(x(inner), x(by))
                  is Trigger.IfTrigger -> Trigger.IfTrigger(x(inner), x(condition))
                  is Trigger.XTrigger -> Trigger.XTrigger(x(inner))
                  is Trigger.Transform -> Trigger.Transform(x(trigger), transformKind)
                  is Trigger.WhenGain -> this
                  is Trigger.WhenRemove -> this
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
}
