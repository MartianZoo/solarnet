package dev.martianzoo.pets

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.BasicTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.withTypeVariables
import dev.martianzoo.pets.util.toSetStrict
import kotlin.reflect.KClass

/**
 * Transforms [PetNode] trees while preserving only the kind requested by each public entry point.
 * For example, [transformInstruction] promises another [Instruction], but not the same concrete
 * node type; [transformInstructionTree] also permits an [Instruction] to become an
 * [InstructionGroup].
 *
 * Implementations use [transformNode] as an unchecked internal dispatcher. Clients should use the
 * narrowest named kind-preserving entry point that describes what they require.
 */
public abstract class PetTransformer protected constructor() {
  public companion object {
    /** A transformer that just returns its input. */
    public fun noOp(): PetTransformer =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode = node
        }

    /**
     * Applies each non-null transformer in order, feeding each result into the next transformer.
     */
    public fun chain(transformers: List<PetTransformer?>): PetTransformer =
        InSeriesTransformer(transformers.filterNotNull())

    /** Vararg form of [chain]. */
    public fun chain(vararg transformers: PetTransformer?): PetTransformer =
        chain(transformers.toList())

    private class InSeriesTransformer(private val transformers: List<PetTransformer>) :
        PetTransformer() {
      override fun transformNode(node: PetNode): PetNode =
          transformers.fold(node) { current, transformer ->
            transformer.transformWithoutKindCheck(current)
          }
    }
  }

  /**
   * Implements this transformation for one node. Call [transformChildren] to descend into its
   * children. This method deliberately makes no kind promise; the named public entry points enforce
   * the kind each caller requests.
   */
  protected abstract fun transformNode(node: PetNode): PetNode

  /**
   * Transforms a dynamically typed major Pets element using its major preprocessing kind.
   * Instructions use the broader [InstructionTree] kind because their cardinality may change.
   */
  public fun transformElement(node: PetElement): PetElement =
      when (node) {
        is Action -> transformAction(node)
        is Effect -> transformEffect(node)
        is Expression -> transformExpression(node)
        is InstructionTree -> transformInstructionTree(node)
        is Metric -> transformMetric(node)
        is Requirement -> transformRequirement(node)
      }

  /** Transforms an action while preserving the [Action] kind. */
  public fun transformAction(node: Action): Action = transformAsKind(node, Action::class)

  /** Transforms an action cost while preserving the [Cost] kind. */
  public fun transformCost(node: Cost): Cost = transformAsKind(node, Cost::class)

  /** Transforms a class name while preserving the [ClassName] kind. */
  public fun transformClassName(node: ClassName): ClassName =
      transformAsKind(node, ClassName::class)

  /** Transforms an effect while preserving the [Effect] kind. */
  public fun transformEffect(node: Effect): Effect = transformAsKind(node, Effect::class)

  /** Transforms an effect trigger while preserving the [Trigger] kind. */
  public fun transformTrigger(node: Trigger): Trigger = transformAsKind(node, Trigger::class)

  /** Transforms a child that structurally must remain a [BasicTrigger]. */
  public fun transformBasicTrigger(node: BasicTrigger): BasicTrigger =
      transformAsKind(node, BasicTrigger::class)

  /** Transforms an expression while preserving the [Expression] kind. */
  public fun transformExpression(node: Expression): Expression =
      transformAsKind(node, Expression::class)

  /** Transforms a refinement while preserving the [Refinement] kind. */
  public fun transformRefinement(node: Refinement): Refinement =
      transformAsKind(node, Refinement::class)

  /** Transforms a from-expression while preserving the [FromExpression] kind. */
  public fun transformFromExpression(node: FromExpression): FromExpression =
      transformAsKind(node, FromExpression::class)

  /**
   * Transforms one task-shaped instruction while preserving the [Instruction] kind. Use
   * [transformInstructionTree] when cardinality is allowed to change.
   */
  public fun transformInstruction(node: Instruction): Instruction =
      transformAsKind(node, Instruction::class)

  /**
   * Transforms while preserving the broader [InstructionTree] kind, permitting cardinality changes.
   */
  public fun transformInstructionTree(node: InstructionTree): InstructionTree =
      transformAsKind(node, InstructionTree::class)

  /** Transforms a metric while preserving the [Metric] kind. */
  public fun transformMetric(node: Metric): Metric = transformAsKind(node, Metric::class)

  /** Transforms a property while preserving the [Property] kind. */
  public fun transformProperty(node: Property): Property = transformAsKind(node, Property::class)

  /** Transforms a property name while preserving the [PropertyName] kind. */
  public fun transformPropertyName(node: PropertyName): PropertyName =
      transformAsKind(node, PropertyName::class)

  /** Transforms a property value while preserving the [PropertyValue] kind. */
  public fun transformPropertyValue(node: PropertyValue): PropertyValue =
      transformAsKind(node, PropertyValue::class)

  /** Transforms a requirement while preserving the [Requirement] kind. */
  public fun transformRequirement(node: Requirement): Requirement =
      transformAsKind(node, Requirement::class)

  /** Transforms a scaled expression while preserving the [ScaledExpression] kind. */
  public fun transformScaledExpression(node: ScaledExpression): ScaledExpression =
      transformAsKind(node, ScaledExpression::class)

  /** Transforms a scalar while preserving the [Scalar] kind. */
  public fun transformScalar(node: Scalar): Scalar = transformAsKind(node, Scalar::class)

  /** Transforms heterogeneous infrastructure data without promising or checking a result kind. */
  // TODO: Contract this temporary tfm-canon seam.
  public fun transformWithoutKindCheck(node: PetNode): PetNode = transformNode(node)

  private fun <P : PetNode> transformAsKind(node: PetNode, requiredKind: KClass<P>): P {
    val transformed = transformWithoutKindCheck(node)
    if (!requiredKind.isInstance(transformed)) {
      throw KindException(
          "${this::class.simpleName ?: "PetTransformer"} transformed ${node::class.simpleName} " +
              "outside the ${requiredKind.simpleName} kind: $transformed"
      )
    }
    @Suppress("UNCHECKED_CAST")
    return transformed as P
  }

  /** Returns [node] rebuilt after recursively transforming each immediate child. */
  @Suppress("CyclomaticComplexMethod") // TODO: break up
  protected fun transformChildren(node: PetNode): PetNode {
    fun expressions(nodes: Iterable<Expression>): List<Expression> =
        nodes.map(::transformExpression)
    fun expressions(nodes: Set<Expression>): Set<Expression> =
        expressions(nodes as Iterable<Expression>).toSetStrict()
    fun metrics(nodes: Iterable<Metric>): List<Metric> = nodes.map(::transformMetric)
    fun requirements(nodes: Iterable<Requirement>): List<Requirement> =
        nodes.map(::transformRequirement)
    fun requirements(nodes: Set<Requirement>): Set<Requirement> =
        requirements(nodes as Iterable<Requirement>).toSetStrict()
    fun trees(nodes: Iterable<InstructionTree>): List<InstructionTree> =
        nodes.map(::transformInstructionTree)

    return when (node) {
      is ClassName -> node
      is Refinement -> Refinement(transformRequirement(node.requirement), node.forgiving)
      is Expression ->
          Expression(
              transformClassName(node.className),
              expressions(node.arguments),
              node.refinement?.let(::transformRefinement),
              node.complement,
              node.argumentsSpecified,
          )
      is ScaledExpression ->
          scaledEx(transformExpression(node.expression), transformScalar(node.scalar))
      is Scalar -> node
      is PropertyName -> node
      is PropertyValue ->
          when (node) {
            is PropertyValue.MetricType,
            is PropertyValue.NumberType,
            is PropertyValue.RequirementType,
            is PropertyValue.OptionalRequirementType,
            is PropertyValue.AbsentRequirementValue,
            is PropertyValue.NumberValue -> node
            is PropertyValue.MetricValue -> PropertyValue.MetricValue(transformMetric(node.value))
            is PropertyValue.RequirementValue ->
                PropertyValue.RequirementValue(transformRequirement(node.value))
          }
      is Metric ->
          when (node) {
            is Metric.Count -> Metric.Count(transformExpression(node.expression))
            is Metric.Constant -> node
            is Property ->
                Property(
                    transformPropertyName(node.propertyName),
                    node.receiver?.let(::transformExpression),
                )
            is Metric.Scaled -> Metric.scaled(transformMetric(node.inner), node.unit)
            is Metric.Max -> Metric.Max(transformMetric(node.inner), transformMetric(node.maximum))
            is Metric.Subtract ->
                Metric.Subtract(
                    transformMetric(node.minuend),
                    transformMetric(node.subtrahend),
                )
            is Metric.Or -> Metric.Or.create(metrics(node.metrics))!!
            is Metric.Eval -> Metric.Eval(transformProperty(node.property))
            is Metric.Transform -> Metric.Transform(transformMetric(node.inner), node.transformKind)
          }
      is Requirement ->
          when (node) {
            is Requirement.Min -> Requirement.Min(node.target, transformMetric(node.metric))
            is Requirement.Max -> Requirement.Max(node.target, transformMetric(node.metric))
            is Requirement.Exact -> Requirement.Exact(node.target, transformMetric(node.metric))
            is Requirement.Or -> Requirement.Or(requirements(node.requirements))
            is Requirement.And -> Requirement.And(requirements(node.requirements))
            is Requirement.Eval -> Requirement.Eval(transformProperty(node.property))
            is Requirement.Transform ->
                Requirement.Transform(transformRequirement(node.requirement), node.transformKind)
          }
      is InstructionGroup -> InstructionGroup.createTree(trees(node.instructions))
      is Instruction ->
          when (node) {
            is Instruction.NoOp -> node
            is Instruction.Gain ->
                Instruction.Gain(transformScaledExpression(node.scaledEx), node.intensity)
            is Instruction.Remove ->
                Instruction.Remove(transformScaledExpression(node.scaledEx), node.intensity)
            is Instruction.Transmute ->
                Instruction.Transmute(
                        transformFromExpression(node.fromEx),
                        transformScalar(node.scalar),
                        node.intensity,
                    )
                    .withTypeVariables(node.typeVariables.transformedBy(this))
            is Instruction.Per ->
                Instruction.Per(transformInstruction(node.inner), transformMetric(node.metric))
            is Instruction.By ->
                Instruction.By.createTree(
                    transformInstructionTree(node.inner),
                    transformExpression(node.actor),
                )
            is Instruction.Gated ->
                Instruction.Gated(
                    transformRequirement(node.gate),
                    transformInstructionTree(node.inner),
                )
            is Instruction.Then ->
                node
                    .withParts(
                        node.stages.map(::transformInstruction),
                        transformInstructionTree(node.continuation),
                    )
                    .withTypeVariables(node.typeVariables.transformedBy(this))
            is Instruction.Or -> Instruction.Or.createTree(trees(node.instructions))
            is Instruction.Transform ->
                Instruction.Transform(
                    transformInstructionTree(node.instruction),
                    node.transformKind,
                )
          }
      is FromExpression ->
          when (node) {
            is FromExpression.Unchanged ->
                FromExpression.Unchanged(transformExpression(node.expression))
            is FromExpression.Full ->
                FromExpression.Full(
                    transformExpression(node.toExpression),
                    transformExpression(node.fromExpression),
                )
            is FromExpression.Compact ->
                FromExpression.Compact(
                    transformClassName(node.className),
                    node.arguments.map(::transformFromExpression),
                    node.refinement?.let(::transformRefinement),
                )
          }
      is Effect ->
          node
              .copy(
                  trigger = transformTrigger(node.trigger),
                  instruction = transformInstructionTree(node.instruction),
              )
              .withTypeVariables(node.typeVariables.transformedBy(this))
      is Trigger ->
          when (node) {
            is Trigger.Or -> Trigger.Or(node.triggers.map(::transformTrigger))
            is Trigger.OnGainOf -> Trigger.OnGainOf.create(transformExpression(node.expression))
            is Trigger.OnRemoveOf -> Trigger.OnRemoveOf.create(transformExpression(node.expression))
            is Trigger.ByTrigger ->
                Trigger.ByTrigger(transformTrigger(node.inner), transformExpression(node.by))
            is Trigger.IfTrigger ->
                Trigger.IfTrigger(
                    transformTrigger(node.inner),
                    transformRequirement(node.condition),
                )
            is Trigger.XTrigger -> Trigger.XTrigger(transformBasicTrigger(node.inner))
            is Trigger.Transform ->
                Trigger.Transform(transformTrigger(node.inner), node.transformKind)
            is Trigger.WhenGain -> node
            is Trigger.WhenRemove -> node
          }
      is Action ->
          Action(
                  node.cost?.let(::transformCost),
                  transformInstructionTree(node.instruction),
              )
              .withTypeVariables(node.typeVariables.transformedBy(this))
      is Cost ->
          when (node) {
            is Cost.Spend -> Cost.Spend(transformScaledExpression(node.scaledEx))
            is Cost.Gated -> Cost.Gated(transformRequirement(node.gate), transformCost(node.cost))
            is Cost.Per -> Cost.Per(transformCost(node.cost), transformMetric(node.metric))
            is Cost.Multi -> Cost.Multi(node.costs.map(::transformCost))
            is Cost.Transform -> Cost.Transform(transformCost(node.cost), node.transformKind)
          }
    }
  }
}
