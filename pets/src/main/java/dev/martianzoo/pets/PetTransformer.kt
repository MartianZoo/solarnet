package dev.martianzoo.pets

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.util.toSetStrict

/**
 * An object that transforms [PetNode]s (typically entire syntax trees) into other [PetNode]s. Most
 * transformations should be exposed as a [PetTransformer] instance so they can be chained together
 * with [chain], etc.
 */
public abstract class PetTransformer protected constructor() {
  public companion object {
    /** A transformer that just returns its input. */
    public fun noOp(): PetTransformer =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P) = node
        }

    /**
     * A transformer that applies each *non-null* transformer in [transformers], feeding the output
     * from each to the input of the next.
     */
    public fun chain(transformers: List<PetTransformer?>): PetTransformer =
        InSeriesTransformer(transformers.filterNotNull())

    /** Vararg form of [chain]. */
    public fun chain(vararg transformers: PetTransformer?) = chain(transformers.toList())

    private open class InSeriesTransformer(val transformers: List<PetTransformer>) :
        PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        var result = node
        for (xer in transformers) {
          result = xer.transform(result)
        }
        return result
      }
    }
  }

  /**
   * Returns an altered form of the [node] tree. To have your transformer descend the subtrees from
   * here you must call [transformChildren]; you can either preprocess or postprocess those trees if
   * needed.
   */
  public abstract fun <P : PetNode> transform(node: P): P

  /**
   * Transforms [node] by transforming each of its children using [transform]; a "default"
   * transformation. Call this from your own [transform] override when you don't need to transform
   * the node itself, but you want to continue to descend the subtrees.
   */
  protected fun <P : PetNode> transformChildren(node: P): P {
    // You can see below why we need these method names to be as short as possible...
    @Suppress("UNCHECKED_CAST") fun <P : PetNode?> x(node: P): P = node?.let(::transform) as P
    fun <P : PetNode> x(nodes: Iterable<P>): List<P> = nodes.map(::x)
    fun <P : PetNode> x(nodes: Set<P>): Set<P> = x(nodes as Iterable<P>).toSetStrict()

    return (node as PetNode).run {
      // The least interesting code in the entire project?
      val rewritten =
          when (this) {
            is ClassName -> this
            is Refinement -> Refinement(x(requirement), forgiving)
            is Expression -> Expression(x(className), x(arguments), x(refinement))
            is ScaledExpression -> ScaledExpression(x(scalar), x(expression))
            is Scalar -> this
            is Metric ->
                when (this) {
                  is Metric.Count -> Metric.Count(x(expression))
                  is Metric.Scaled -> Metric.Scaled(unit, x(inner))
                  is Metric.Max -> Metric.Max(x(inner), maximum)
                  is Metric.Plus -> Metric.Plus(x(metrics))
                  is Metric.Transform -> Metric.Transform(x(inner), transformKind)
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
                  is Instruction.Per -> Instruction.Per(x(inner), x(metric))
                  is Instruction.Gated -> Instruction.Gated(x(gate), x(inner), mandatory)
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
                  is Trigger.Transform -> Trigger.Transform(x(inner), transformKind)
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
