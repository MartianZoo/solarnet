package dev.martianzoo.pets

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Defaults
import dev.martianzoo.pets.types.Defaults.DefaultSpec
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.DependencySet
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.TypeVariableScope
import dev.martianzoo.pets.types.inferTypeVariables
import dev.martianzoo.pets.util.invoke

/**
 * Applies Class-table-dependent elaboration packages to authored Pets. Runtime binding operations
 * return [PetTransformer] only where the engine must retain one deferred binding across several AST
 * families.
 */
public class PetElaborator(public val classTable: ClassTable) {
  private val effectsByClass = mutableMapOf<Class, List<Effect>>()
  private val transformDispatcher: Lazy<PetTransformer> = lazy {
    classTable.transformDispatcher()
  }

  /**
   * Elaborates one dynamically typed, session-authored Pets element for execution in [owner]'s
   * context. An Instruction is treated as the broader InstructionTree family, so its cardinality
   * may change. Property evaluation is rejected because ordinary mutation input cannot quote and
   * execute Class-property syntax.
   */
  public fun elaborateInput(
      input: PetElement,
      owner: HasClassName? = null,
  ): PetElement = inputElaborator(owner).transformElement(input)

  /**
   * The statically typed [InstructionTree] form of [elaborateInput], with the same elaboration and
   * cardinality-changing behavior.
   */
  public fun elaborateInput(
      input: InstructionTree,
      owner: HasClassName? = null,
  ): InstructionTree = inputElaborator(owner).transformInstructionTree(input)

  /** Elaborates a session-authored Metric, including explicit Class-property evaluation. */
  public fun elaborateMetricInput(
      input: Metric,
      context: Expression,
      owner: HasClassName? = null,
  ): Metric =
      chain(
              normalizeInput(),
              propertyEvaluator(context, owner),
              finishAuthoredSyntax(context, owner),
          )
          .transformMetric(input)

  /** Elaborates source-shaped Pets returned by one custom instruction implementation. */
  public fun elaborateCustomInstruction(
      input: InstructionTree,
      owner: HasClassName? = null,
  ): InstructionTree = finishAuthoredSyntax(THIS.expression, owner).transformInstructionTree(input)

  private fun inputElaborator(owner: HasClassName?): PetTransformer =
      chain(
          rejectPropertyEvaluations(),
          normalizeInput(),
          finishAuthoredSyntax(THIS.expression, owner),
      )

  private fun normalizeInput(): PetTransformer =
      chain(useFullNames(), classTable.inferTypeVariables())

  private fun finishAuthoredSyntax(
      context: Expression,
      owner: HasClassName?,
  ): PetTransformer =
      chain(
          atomizer(),
          insertDefaults(context),
          owner?.let(::contextualOwnerBinding),
          transformDispatcher(),
      )

  /** Effects inherited by [klass], processed as far as possible without a concrete component. */
  public fun classEffects(klass: Class): List<Effect> {
    require(classTable.isActive(klass)) { "$klass is not active in this game" }
    return effectsByClass.getOrPut(klass) {
      fun directClassEffects(source: Class) =
          source.declaration.effects
              .map(source::interpretTypeVariablesIn)
              .map(attachToClassTransformer(source)::transformEffect)

      val evaluator =
          propertyEvaluator(
              context = klass.defaultType.expressionFull,
              deferAbstract = true,
          )
      klass.allSuperclasses().flatMap(::directClassEffects).map(evaluator::transformEffect)
    }
  }

  /** Rejects property evaluation syntax outside a class effect. */
  private fun rejectPropertyEvaluations(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            when (node) {
              is Metric.Eval,
              is Requirement.Eval ->
                  throw PetSyntaxException("EVAL is valid only inside a class effect")
              else -> transformChildren(node)
            }
      }

  /** Expands Class-property evaluations that are concrete in the supplied instruction context. */
  public fun evaluateProperties(
      input: InstructionTree,
      context: Expression,
      owner: HasClassName? = null,
  ): InstructionTree = propertyEvaluator(context, owner).transformInstructionTree(input)

  /** Expands Class-property evaluations that are concrete in the supplied Metric context. */
  public fun evaluateProperties(
      input: Metric,
      context: Expression,
      owner: HasClassName? = null,
  ): Metric = propertyEvaluator(context, owner).transformMetric(input)

  /**
   * Expands explicit property evaluations after their receivers have become concrete, deferring
   * fanout bodies until their selected component has been bound.
   */
  private fun propertyEvaluator(
      context: Expression,
      owner: HasClassName? = null,
      deferAbstract: Boolean = false,
  ): PetTransformer {
    val expanding = mutableSetOf<Pair<Expression, PropertyName>>()
    val contextualizer =
        chain(
            replaceThisExpressionsWith(context),
            owner?.let(::contextualOwnerBinding),
        )
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        // The selected component supplies a fanout branch's context, so its property evaluations
        // must remain inert until Instructor has bound that selection. The selector itself still
        // belongs to the enclosing context and is transformed normally.
        if (node is Each) {
          return Each(transformExpression(node.selector), node.body)
        }
        val property =
            when (node) {
              is Metric.Eval -> node.property
              is Requirement.Eval -> node.property
              else -> return transformChildren(node)
            }
        val contextualProperty = contextualizer.transformProperty(property)
        val receiver =
            contextualProperty.receiver
                ?: throw invalidPetDefinition(
                    "Evaluated property `${contextualProperty.propertyName}` has no receiver"
                )

        val receiverType = classTable.resolve(receiver)
        val propertyType =
            if (receiverType.rootClass === classTable.classClass) {
              classTable.resolve(receiverType.expressionFull.arguments.single())
            } else {
              receiverType
            }
        val propertyClass = propertyType.rootClass
        val value =
            propertyClass.properties[contextualProperty.propertyName]
                ?: throw invalidPetDefinition(
                    "Class `${propertyClass.className}` has no property " +
                        "`${contextualProperty.propertyName}`"
                )
        if (deferAbstract && (value.abstract || (propertyType.abstract && THIS in value)))
            return node
        val syntax: PetNode =
            when (node) {
              is Metric.Eval ->
                  when (value) {
                    is MetricValue -> value.value
                    is NumberValue -> contextualProperty
                    else ->
                        throw invalidPetDefinition(
                            "Property `${contextualProperty.propertyName}` is not a concrete Metric on " +
                                "`${propertyClass.className}`"
                        )
                  }
              is Requirement.Eval ->
                  when (value) {
                    AbsentRequirementValue -> Min(scaledEx(COMPONENT, 1))
                    is RequirementValue -> value.value
                    else ->
                        throw invalidPetDefinition(
                            "Property `${contextualProperty.propertyName}` is not a concrete Requirement on " +
                                "`${propertyClass.className}`"
                        )
                  }
              else -> error("checked above")
            }

        val key = propertyType.expressionFull to contextualProperty.propertyName
        if (!expanding.add(key)) {
          throw invalidPetDefinition(
              "Property `${contextualProperty.propertyName}` is recursive on " +
                  "`${propertyType.expressionFull}`"
          )
        }
        val expanded: PetNode =
            try {
              val transformer =
                  chain(
                      replaceThisExpressionsWith(propertyType.expressionFull),
                      owner?.let(::contextualOwnerBinding),
                  )
              when (syntax) {
                is Metric -> transformMetric(transformer.transformMetric(syntax))
                is Requirement -> transformRequirement(transformer.transformRequirement(syntax))
                else -> error("checked above")
              }
            } finally {
              expanding.remove(key)
            }
        val finishing =
            chain(
                atomizer(),
                insertDefaults(context),
                owner?.let(::contextualOwnerBinding),
                transformDispatcher(),
            )
        return when (expanded) {
          is Metric -> finishing.transformMetric(expanded)
          is Requirement -> finishing.transformRequirement(expanded)
          else -> error("checked above")
        }
      }
    }
  }

  /**
   * Returns a deferred binding for the contextual `Owner` placeholder. It binds everywhere except
   * inside the body of an Owner-selecting fanout, where the selection supplies the Owner instead.
   * This raw-transformer seam lets runtime matching accumulate one binding before its eventual AST
   * family is known.
   */
  public fun contextualOwnerBinding(owner: HasClassName): PetTransformer =
      replaceOwnerWith(owner, ::shieldsContextualOwner)

  /** Whether [node] is a fanout whose Owner selection supplies its body's contextual owner. */
  private fun shieldsContextualOwner(node: PetNode): Boolean =
      node is Each && selectionSuppliesOwner(node.selector)

  /** Whether an `EACH` selector's matches are themselves Owners. */
  public fun selectionSuppliesOwner(selector: Expression): Boolean {
    val klass = classTable.findClass(selector.className) ?: return false
    return classTable.findClass(OWNER)?.let(klass::isSubtypeOf) == true
  }

  private fun attachToClassTransformer(klass: Class): PetTransformer {
    val context = klass.className.has(Min(scaledEx(OK, 1)))
    return chain(
        classTable.inferTypeVariables(),
        insertDefaults(context),
        atomizer(),
        transformDispatcher(),
        fixEffectForUnownedContext(klass),
    )
  }

  /** Whether an `Owner` occurrence outside a candidate-owned scope needs a value from the event. */
  private fun ownerNeedsContext(instruction: InstructionTree): Boolean {
    fun needsContext(node: PetNode): Boolean {
      if (node is Expression && node.className == OWNER) return true
      return when (node) {
        is Each ->
            needsContext(node.selector) ||
                (!selectionSuppliesOwner(node.selector) && needsContext(node.body))
        is Metric.Rank ->
            needsContext(node.selector) ||
                (!selectionSuppliesOwner(node.selector) && node.metrics.any(::needsContext))
        else -> node.immediateChildren().any(::needsContext)
      }
    }
    return needsContext(instruction)
  }

  /** Adds icon-grammar `BY Owner` when an ownerless Effect's result needs its event's Player. */
  private fun fixEffectForUnownedContext(klass: Class): PetTransformer? {
    if (klass.allSuperclasses().any { it.className == OWNED || it.className == OWNER }) return null
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (shieldsContextualOwner(node)) return node
        return if (
            node is Effect && ownerNeedsContext(node.instruction) && OWNER !in node.trigger
        ) {
          node.copy(trigger = ByTrigger(node.trigger, OWNER))
        } else {
          transformChildren(node)
        }
      }
    }
  }

  private fun useFullNames(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          return if (node is ClassName) {
            classTable.resolve(node.expression).className
          } else {
            transformChildren(node)
          }
        }
      }

  private fun atomizer(): PetTransformer {
    val atomized = classTable.findClass(ATOMIZED) ?: return noOp()

    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node is Gain) {
          val scex = node.scaledEx
          val sc = scex.scalar
          if (
              sc is ActualScalar &&
                  sc.value > 1 &&
                  classTable.findClass(scex.expression.className)?.isSubtypeOf(atomized) == true
          ) {
            val one = gain(scaledEx(scex.expression, ActualScalar(1)), node.intensity) as Gain
            return InstructionGroup(List(sc.value) { one })
          }
          return node
        }
        return transformChildren(node)
      }
    }
  }

  private fun insertDefaults(context: Expression): PetTransformer =
      chain(insertGainRemoveDefaults(context), insertExpressionDefaults(context))

  private fun insertGainRemoveDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val result: PetNode =
            if (node is Change) {
              when (node) {
                is Gain ->
                    handleIt(node, node.gaining, { it.gainOnly }) { fixed, intensity ->
                      gain(scaledEx(fixed, node.count), intensity)
                    }
                is Remove ->
                    handleIt(node, node.removing, { it.removeOnly }) { fixed, intensity ->
                      remove(scaledEx(fixed, node.count), intensity)
                    }
                is Transmute -> handleTransmute(node)
              }
            } else {
              transformChildren(node)
            }
        return result
      }

      private fun handleIt(
          node: Change,
          original: Expression,
          extractor: (Defaults) -> DefaultSpec,
          rebuild: (Expression, Instruction.Intensity?) -> Instruction,
      ): Instruction {
        return if (leaveItAlone(original)) {
          node // don't descend
        } else {
          val kind = if (node is Gain) "gain" else "removal"
          val spec = extractor(classTable.getClass(original.className).defaults)
          rejectEmptyArgumentsWithoutDefaults(original, spec, kind)
          if (kind == "gain") requireExplicitDependencyDefaults(original, spec, kind)
          val fixed =
              if (kind == "removal" && hasUnacceptedDependencyDefaults(original, spec)) original
              else insertDefaultsIntoExpr(original, spec.dependencies, context, classTable)
          val intensity = node.intensity ?: spec.intensity
          rebuild(fixed, intensity)
        }
      }

      private fun handleTransmute(node: Transmute): Transmute {
        val gainDefault = defaultFor(node.gaining, { it.gainOnly }, gain = true)
        val removeDefault = defaultFor(node.removing, { it.removeOnly }, gain = false)
        val intensity =
            node.intensity ?: intersectIntensities(gainDefault?.intensity, removeDefault?.intensity)

        return Transmute(
            Full(
                applyDefault(node.gaining, gainDefault, context, gain = true),
                applyDefault(node.removing, removeDefault, context, gain = false),
            ),
            node.count,
            intensity,
        )
      }

      private fun defaultFor(
          expression: Expression,
          extractor: (Defaults) -> DefaultSpec,
          gain: Boolean,
      ): DefaultSpec? {
        if (leaveItAlone(expression)) return null
        val default = extractor(classTable.getClass(expression.className).defaults)
        rejectEmptyArgumentsWithoutDefaults(
            expression,
            default,
            if (gain) "gain" else "removal",
        )
        if (gain) requireExplicitDependencyDefaults(expression, default, "gain")
        return default
      }

      private fun applyDefault(
          expression: Expression,
          default: DefaultSpec?,
          context: Expression,
          gain: Boolean,
      ): Expression =
          if (default == null || (!gain && hasUnacceptedDependencyDefaults(expression, default))) {
            expression
          } else insertDefaultsIntoExpr(expression, default.dependencies, context, classTable)

      private fun intersectIntensities(
          gainIntensity: Instruction.Intensity?,
          removeIntensity: Instruction.Intensity?,
      ): Instruction.Intensity? =
          when {
            gainIntensity == null -> removeIntensity
            removeIntensity == null -> gainIntensity
            gainIntensity == Instruction.Intensity.MANDATORY ||
                removeIntensity == Instruction.Intensity.MANDATORY ->
                Instruction.Intensity.MANDATORY
            gainIntensity == Instruction.Intensity.AMAP ||
                removeIntensity == Instruction.Intensity.AMAP -> Instruction.Intensity.AMAP
            else -> Instruction.Intensity.OPTIONAL
          }
    }
  }

  private fun requireExplicitDependencyDefaults(
      expression: Expression,
      default: DefaultSpec,
      kind: String,
  ) {
    if (
        default.dependencies.keys.isNotEmpty() &&
            expression.arguments.isEmpty() &&
            !expression.argumentsSpecified
    ) {
      throw PetSyntaxException(
          "`${expression.className}` has $kind dependency defaults; write " +
              "`${expression.className}<>` to accept them or provide dependency arguments"
      )
    }
  }

  private fun rejectEmptyArgumentsWithoutDefaults(
      expression: Expression,
      default: DefaultSpec,
      kind: String,
  ) {
    if (
        expression.argumentsSpecified &&
            expression.arguments.isEmpty() &&
            default.dependencies.keys.isEmpty()
    ) {
      throw PetSyntaxException(
          "`${expression.className}<>` has no $kind dependency defaults to accept"
      )
    }
  }

  private fun hasUnacceptedDependencyDefaults(
      expression: Expression,
      default: DefaultSpec,
  ): Boolean =
      default.dependencies.keys.isNotEmpty() &&
          expression.arguments.isEmpty() &&
          !expression.argumentsSpecified

  private fun insertExpressionDefaults(context: Expression): PetTransformer {
    var refinementDepth = 0
    val refinementCandidates = mutableListOf<Pair<Expression, Int>>()
    // A nested RANK establishes a separate candidate scope and therefore keeps ordinary defaults.
    var rankDepth = 0
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node is Expression.Refinement) {
          refinementDepth++
          try {
            return transformChildren(node)
          } finally {
            refinementDepth--
          }
        }
        if (node is Metric.Rank) {
          rankDepth++
          try {
            return transformChildren(node)
          } finally {
            rankDepth--
          }
        }
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node

        val shell = transformChildren(node.copy(refinement = null)) as Expression
        val klass = classTable.getClass(shell.className)
        val defaultDeps = klass.defaults.allUsages.dependencies
        rejectEmptyArgumentsWithoutDefaults(node, klass.defaults.allUsages, "all-use")
        val refinementCandidate =
            refinementCandidates.lastOrNull()?.takeIf { (_, depth) -> depth == rankDepth }?.first
        val defaulted =
            insertDefaultsIntoExpr(
                shell,
                defaultDeps,
                context,
                classTable,
                deferVariableDefaults = refinementDepth > 0 && !node.argumentsSpecified,
                refinementCandidate = refinementCandidate,
            )
        val refinement =
            node.refinement?.let {
              if (it is Has) {
                refinementCandidates += defaulted.copy(refinement = null) to rankDepth
              }
              try {
                transformRefinement(it)
              } finally {
                if (it is Has) refinementCandidates.removeLast()
              }
            }
        return defaulted.copy(refinement = refinement)
      }
    }
  }

  private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)

  // only has to modify the args/specs
  private fun insertDefaultsIntoExpr(
      original: Expression,
      defaultDeps: DependencySet,
      contextCpt: Expression,
      classTable: ClassTable,
      deferVariableDefaults: Boolean = false,
      refinementCandidate: Expression? = null,
  ): Expression {

    val klass: Class = classTable.getClass(original.className)
    val dethissed: Expression = replaceThisExpressionsWith(contextCpt).transformExpression(original)
    val match: DependencySet = klass.dependencies.matchPartial(dethissed.arguments)

    val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
    val refinementBoundKey =
        if (refinementCandidate == null || original.argumentsSpecified) {
          null
        } else {
          val candidate =
              replaceThisExpressionsWith(contextCpt).transformExpression(refinementCandidate)
          try {
            klass.matchDependencyKeys(listOf(candidate)).single()
          } catch (_: ExpressionException) {
            null
          }
        }
    val fallbacks: Map<Key, Expression> =
        defaultDeps
            .typeDependencies()
            .filterNot {
              (deferVariableDefaults && klass.isEqualityConstrainedDependency(it.key)) ||
                  it.key == refinementBoundKey
            }
            .associate {
              it.key to it.expression
            }
    val inferred = klass.specialize(dethissed.arguments).narrowedDependencies.keys - preferred.keys

    val newArgs: List<Expression> =
        klass.dependencies.keys.mapNotNull {
          preferred[it] ?: fallbacks[it]?.takeUnless { _ -> it in inferred }
        }

    return original
        .copy(
            arguments = newArgs,
            argumentsSpecified = original.argumentsSpecified || newArgs.isNotEmpty(),
        )
        .also {
          require(it.className == original.className)
          require(it.refinement == original.refinement)
          require(it.arguments.containsAll(original.arguments))
        }
  }

  /**
   * Closes one Class Effect over its exact component Type, `This` context, and contextual owner.
   */
  public fun specializeEffect(
      general: Type,
      specific: Type,
      effect: Effect,
      context: Expression,
      owner: HasClassName? = null,
  ): Effect {
    val contextualizer =
        chain(
            owner?.let(::contextualOwnerBinding),
            replaceThisExpressionsWith(context),
        )
    val scope = effect.typeVariables
    val bindings = specific.variableBindingsFrom(general, scope.variables)
    val contextualScope = scope.transformedBy(contextualizer)
    return chain(
            contextualizer,
            contextualScope.bind(bindings),
            invalidChangesToDie(),
        )
        .transformEffect(effect)
  }

  /**
   * Returns the deferred binding introduced by narrowing one authored variable scope. Runtime
   * matching applies it later to whichever AST family contains the declared variable occurrences.
   */
  public fun specializeVariables(
      general: Type,
      specific: Type,
      authoredGeneral: Expression,
      typeVariables: TypeVariableScope,
      owner: HasClassName? = null,
  ): PetTransformer {
    val bindings =
        typeVariables.bindingsFrom(authoredGeneral, general.groundType, specific.groundType)
    val contextualizer = chain(owner?.let(::contextualOwnerBinding))
    val contextualScope = typeVariables.transformedBy(contextualizer)
    return chain(
        contextualizer,
        contextualScope.bind(bindings),
        invalidChangesToDie(),
    )
  }

  private fun invalidChangesToDie(): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val specialized = transformChildren(node)
        if (specialized !is Change) return specialized

        try {
          val types =
              listOfNotNull(
                  specialized.gaining?.let(classTable::resolve),
                  specialized.removing?.let(classTable::resolve),
              )
          if (types.any { !classTable.isActive(it) }) {
            return if (specialized.intensity == MANDATORY) {
              gain(DIE)
            } else {
              NoOp
            }
          }
        } catch (_: ExpressionException) {
          return gain(DIE)
        }
        return specialized
      }
    }
  }
}
