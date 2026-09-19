package dev.martianzoo.pets

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.actionToEffect
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
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
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.FromExpression.Unchanged
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
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
import dev.martianzoo.pets.ast.withTypeVariables
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Defaults
import dev.martianzoo.pets.types.Defaults.DefaultSpec
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.DependencySet
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.TypeVariableScope
import dev.martianzoo.pets.types.recordTypeVariableScopes
import dev.martianzoo.pets.util.invoke

/**
 * Applies Class-table-dependent elaboration packages to authored Pets, as defined by
 * [section 12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration).
 * Elaboration fills in what a physical game leaves implicit — that a tile goes on a land area, that
 * a resource belongs to the player doing the thing, that "gain 3 cards" means three separate cards.
 * It changes how a source *reads*; it never changes which types exist.
 *
 * The stages are fixed ([rule
 * L12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)):
 * record Type-variable scopes, split atomized gains, insert defaults, bind the contextual owner,
 * dispatch transform blocks, expand property evaluations. The entry points supply different
 * contexts and permit different property forms while preserving that shared ordering.
 *
 * Runtime binding operations return [PetTransformer] only where the engine must retain one deferred
 * binding across several AST families.
 */
public class PetElaborator(public val classTable: ClassTable) {
  private val effectsByClass = mutableMapOf<Class, List<Effect>>()
  private val transformDispatcher: Lazy<PetTransformer> = lazy {
    classTable.transformDispatcher()
  }

  /**
   * Elaborates one dynamically typed, session-authored Pets element for execution in [owner]'s
   * context. Per
   * [rule L12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration),
   * a submitted element defaults against `This`, atomizes before defaulting, and binds the
   * contextual owner to the submitting player. An Instruction is treated as the broader
   * InstructionTree family, so its cardinality may change.
   *
   * Property evaluation is rejected here, because an ordinary submitted instruction has no receiver
   * context to expand against ([rule
   * L12-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration));
   * see [elaborateMetricInput], which does.
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

  /**
   * Elaborates a session-authored Metric, including explicit Class-property evaluation — a
   * submitted *metric* is given a receiver context, so unlike a submitted instruction it may carry
   * `EVAL` ([rule
   * L12-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)).
   */
  public fun elaborateMetricInput(
      input: Metric,
      context: Expression,
      owner: HasClassName? = null,
  ): Metric =
      chain(
              normalizeInput(),
              finishAuthoredSyntax(context, owner),
              propertyEvaluator(context, owner),
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
      chain(useFullNames(), classTable.recordTypeVariableScopes())

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

  /**
   * Effects inherited by [klass], processed as far as possible without a concrete component. Per
   * [rule L12-13](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)
   * a class's effects are elaborated against that class's own context and gathered from every
   * superclass; the contextual owner is left open here, and [specializeEffect] closes each one over
   * an exact component later.
   */
  public fun classEffects(klass: Class): List<Effect> {
    require(classTable.isIncluded(klass)) { "$klass is not included in this game" }
    return effectsByClass.getOrPut(klass) {
      val scopeRecorder = classTable.recordTypeVariableScopes()
      fun directClassEffects(source: Class) =
          source.declaration
              .let { declaration ->
                declaration.executableEffects
                    ?: declaration.authoredEffects +
                        declaration.authoredActions.mapIndexed { index, action ->
                          actionToEffect(
                              scopeRecorder.transformAction(action),
                              index + 1,
                          )
                        }
              }
              .map { effect ->
                try {
                  attachToClassTransformer(source)
                      .transformEffect(source.interpretTypeVariablesIn(effect))
                } catch (e: PetException) {
                  throw invalidPetDefinition(
                      "Invalid effect declared by `${source.className}`: `$effect`: ${e.message}",
                      e,
                  )
                }
              }

      val evaluator =
          propertyEvaluator(
              context = klass.defaultExpression,
              deferAbstract = true,
          )
      klass.allSuperclasses().flatMap(::directClassEffects).map(evaluator::transformEffect)
    }
  }

  /**
   * Rejects property evaluation syntax outside a class effect ([rule
   * L12-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)).
   */
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

  /**
   * Expands Class-property evaluations that are concrete in the supplied instruction context ([rule
   * L12-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)).
   * One whose receiver is still abstract stays unexpanded until it is not.
   */
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
   * Returns a deferred binding for the contextual `Owner` placeholder. Per
   * [rule L12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)
   * it binds everywhere except inside the body of an Owner-selecting fanout, where the selection
   * supplies the Owner instead; the selector itself is not shielded, and a `RANK` selector shields
   * nothing.
   *
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
        classTable.recordTypeVariableScopes(),
        atomizer(),
        insertDefaults(context),
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

  /**
   * Adds icon-grammar `BY Owner` when an ownerless Effect's result needs its event's Player — how a
   * rule on a class that is neither an owner nor owned learns whose event it is reacting to ([rule
   * L12-13](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)).
   */
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

  /**
   * Rule L12-11: a gain of several `Atomized` components becomes several gains of one, because
   * three cards are three separate things to choose.
   */
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
            val one = gain(scaledEx(scex.expression, ActualScalar(1)), node.quantifier) as Gain
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

  /** Rebuilds a compact transmutation after independently processing its two projections. */
  private fun retainCompactForm(
      original: Compact,
      gaining: Expression,
      removing: Expression,
  ): Compact {
    if (
        gaining.className != original.className ||
            removing.className != original.className ||
            removing.refinement != null
    ) {
      throw PetSyntaxException(
          "Defaulting cannot change the root or removed refinement of compact transmutation " +
              original
      )
    }

    val klass = classTable.getClass(original.className)
    fun keyedArguments(expression: Expression): Map<Key, Expression> {
      val keys = klass.dependencies.matchPartial(expression.arguments, classTable).keys
      return keys.zip(expression.arguments).toMap()
    }

    val gainingByKey = keyedArguments(gaining)
    val removingByKey = keyedArguments(removing)
    if (gainingByKey.keys != removingByKey.keys) {
      throw PetSyntaxException(
          "Defaulting cannot give compact transmutation projections different dependencies: " +
              original
      )
    }

    val originalKeys =
        klass.dependencies.matchPartial(original.toExpression.arguments, classTable).keys
    val originalByKey = originalKeys.zip(original.arguments).toMap()
    val arguments = buildList {
      klass.dependencies.keys.forEach { key ->
        val gained = gainingByKey[key] ?: return@forEach
        val removed = removingByKey[key] ?: return@forEach
        val authored = originalByKey[key]
        add(
            when {
              authored != null &&
                  authored.toExpression == gained &&
                  authored.fromExpression == removed -> authored
              gained == removed -> Unchanged(gained)
              else -> Full(gained, removed)
            }
        )
      }
    }
    if (arguments.count { it !is Unchanged } != 1) {
      throw PetSyntaxException(
          "Defaulting cannot change more than one dependency of compact transmutation $original"
      )
    }
    return Compact(original.className, arguments, gaining.refinement)
  }

  private fun insertGainRemoveDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val result: PetNode =
            if (node is Change) {
              when (node) {
                is Gain ->
                    handleIt(node, node.gaining, { it.gainOnly }) { fixed, quantifier ->
                      gain(scaledEx(fixed, node.count), quantifier)
                    }
                is Remove ->
                    handleIt(node, node.removing, { it.removeOnly }) { fixed, quantifier ->
                      remove(scaledEx(fixed, node.count), quantifier)
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
          rebuild: (Expression, Instruction.Quantifier?) -> Instruction,
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
          val quantifier = node.quantifier ?: spec.quantifier
          rebuild(fixed, quantifier)
        }
      }

      // Rule L12-8: the gained and removed projections are defaulted independently, and where the
      // transmutation writes no quantifier, the projections' defaults are intersected.
      private fun handleTransmute(node: Transmute): Transmute {
        val gainDefault = defaultFor(node.gaining, { it.gainOnly }, gain = true)
        val removeDefault = defaultFor(node.removing, { it.removeOnly }, gain = false)
        val quantifier =
            node.quantifier
                ?: intersectQuantifiers(gainDefault?.quantifier, removeDefault?.quantifier)

        val gaining = applyDefault(node.gaining, gainDefault, context, gain = true)
        val removing = applyDefault(node.removing, removeDefault, context, gain = false)
        val fromExpression =
            (node.fromEx as? Compact)?.let { retainCompactForm(it, gaining, removing) }
                ?: Full(gaining, removing)
        return Transmute(fromExpression, node.count, quantifier)
            .withTypeVariables(node.typeVariables.transformedBy(this))
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

      /** The stricter of the two, per rule L12-8: mandatory beats AMAP, which beats optional. */
      private fun intersectQuantifiers(
          gainQuantifier: Instruction.Quantifier?,
          removeQuantifier: Instruction.Quantifier?,
      ): Instruction.Quantifier? =
          when {
            gainQuantifier == null -> removeQuantifier
            removeQuantifier == null -> gainQuantifier
            gainQuantifier == Instruction.Quantifier.MANDATORY ||
                removeQuantifier == Instruction.Quantifier.MANDATORY ->
                Instruction.Quantifier.MANDATORY
            gainQuantifier == Instruction.Quantifier.AMAP ||
                removeQuantifier == Instruction.Quantifier.AMAP -> Instruction.Quantifier.AMAP
            else -> Instruction.Quantifier.OPTIONAL
          }
    }
  }

  /**
   * Rule L12-5: a gain must opt in. Where a class has gain dependency defaults, a gain may not
   * leave its argument list implicit — `OceanTile<>` accepts them — so a defaulted placement stays
   * visible at the point of use. A removal declines by writing nothing instead (L12-6), so this is
   * not applied to one.
   */
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

  /**
   * Rule L12-7: `Foo<>` is invalid where that use has no dependency defaults to accept. An empty
   * list is an acceptance, not merely a second spelling, so `Plant<>` cannot honestly mean
   * anything.
   */
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

  /**
   * Rule L12-6: a removal declines its use-specific defaults by writing nothing. An implicit
   * argument list on a removal is not an error — it simply does not receive the removal-only
   * defaults, though all-use defaults (L12-4) still apply.
   */
  private fun hasUnacceptedDependencyDefaults(
      expression: Expression,
      default: DefaultSpec,
  ): Boolean =
      default.dependencies.keys.isNotEmpty() &&
          expression.arguments.isEmpty() &&
          !expression.argumentsSpecified

  /**
   * Rule L12-4: every expression receives its class's all-use dependency defaults, recursively.
   *
   * Inside a refinement two of those insertions are held back so that candidate substitution can
   * bind them instead: a bare dependent expression reserves the first slot that could accept the
   * refined domain (L12-9), and a default whose dependency is a direct use of a class-header type
   * variable is deferred (L12-10). Writing `<>` still accepts the default explicitly in both cases.
   */
  private fun insertExpressionDefaults(context: Expression): PetTransformer {
    var refinementDepth = 0
    val refinementCandidates = mutableListOf<Pair<Expression, Int>>()
    // A nested RANK establishes a separate candidate scope and therefore keeps ordinary defaults.
    var rankDepth = 0
    return object : PetTransformer() {
      fun transformRefinementForCandidate(
          refinement: Expression.Refinement,
          candidate: Expression,
      ): Expression.Refinement =
          when (refinement) {
            is Expression.Refinement.And ->
                Expression.Refinement.create(
                    refinement.refinements.map { transformRefinementForCandidate(it, candidate) }
                )
            is Has -> {
              refinementCandidates += candidate to rankDepth
              try {
                transformRefinement(refinement)
              } finally {
                refinementCandidates.removeLast()
              }
            }
            is Expression.Refinement.Not -> transformRefinement(refinement)
          }

      fun defaultShell(shell: Expression): Expression {
        val klass = classTable.getClass(shell.className)
        val defaultDeps = klass.defaults.allUsages.dependencies
        rejectEmptyArgumentsWithoutDefaults(shell, klass.defaults.allUsages, "all-use")
        val refinementCandidate =
            refinementCandidates.lastOrNull()?.takeIf { (_, depth) -> depth == rankDepth }?.first
        return insertDefaultsIntoExpr(
            shell,
            defaultDeps,
            context,
            classTable,
            deferVariableDefaults = refinementDepth > 0 && !shell.argumentsSpecified,
            refinementCandidate = refinementCandidate,
        )
      }

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
        if (node is Compact) {
          val shell =
              Compact(
                  transformClassName(node.className),
                  node.arguments.map(::transformFromExpression),
              )
          val gaining = defaultShell(shell.toExpression)
          val removing = defaultShell(shell.fromExpression)
          val refinement =
              node.refinement?.let {
                transformRefinementForCandidate(it, gaining.copy(refinement = null))
              }
          return retainCompactForm(
              shell,
              gaining.copy(refinement = refinement),
              removing,
          )
        }
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node

        val shell = transformChildren(node.copy(refinement = null)) as Expression
        val defaulted = defaultShell(shell)
        val refinement =
            node.refinement?.let {
              transformRefinementForCandidate(it, defaulted.copy(refinement = null))
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
    val match: DependencySet = klass.dependencies.matchPartial(dethissed.arguments, classTable)

    val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
    val refinementBoundKey =
        if (refinementCandidate == null || original.argumentsSpecified) {
          null
        } else {
          val candidate =
              replaceThisExpressionsWith(contextCpt).transformExpression(refinementCandidate)
          try {
            klass.matchDependencyKeys(listOf(candidate), classTable).single()
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
    val inferred =
        klass.specialize(dethissed.arguments, classTable).narrowedDependencies.keys - preferred.keys

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
   * Closes one Class Effect over its exact component Type, `This` context, and contextual owner, in
   * one step ([rule
   * L12-15](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration),
   * [rule T13-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
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
    val binder = contextualScope.bind(bindings, classTable)
    return chain(
            contextualizer,
            binder,
            invalidChangesToDie { contextualScope.transformedBy(binder) },
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
        typeVariables.bindingsFrom(
            authoredGeneral,
            general.groundType,
            specific.groundType,
            classTable,
        )
    val contextualizer = chain(owner?.let(::contextualOwnerBinding))
    val contextualScope = typeVariables.transformedBy(contextualizer)
    val binder = contextualScope.bind(bindings, classTable)
    return chain(
        contextualizer,
        binder,
        invalidChangesToDie { contextualScope.transformedBy(binder) },
    )
  }

  /**
   * Retains the explicitly authored Type-variable occurrences when Type resolution rebuilds
   * [resolved] in compact form. Arguments are matched by dependency key, and an authored variable
   * argument omitted only because it equals the Class default remains present.
   */
  public fun retainTypeVariableNames(
      resolved: Expression,
      authored: Expression,
  ): Expression {
    if (authored.descendantsOfType<Expression>().none { it.typeVariableName != null }) {
      return resolved
    }

    fun retainAtRepresentedDependencies(
        target: Expression,
        source: Expression,
    ): Expression {
      val sourceClass = classTable.getClass(source.className)
      val sourceByKey =
          source.arguments
              .zip(sourceClass.matchDependencyKeys(source.arguments, classTable))
              .associate { (argument, key) -> key to argument }
      val targetClass = classTable.getClass(target.className)
      val targetArguments =
          target.arguments.zip(targetClass.matchDependencyKeys(target.arguments, classTable)).map {
              (argument, key) ->
            sourceByKey[key]?.let { sourceArgument ->
              retainAtRepresentedDependencies(argument, sourceArgument)
            } ?: argument
          }
      return target.copy(
          arguments = targetArguments,
          typeVariableName = source.typeVariableName ?: target.typeVariableName,
      )
    }

    val expression = retainAtRepresentedDependencies(resolved, authored)
    val representedKeys =
        classTable
            .getClass(expression.className)
            .matchDependencyKeys(expression.arguments, classTable)
            .toSet()
    val authoredClass = classTable.getClass(authored.className)
    val authoredArguments =
        if (authored.typeVariableName is Reference && !authored.argumentsSpecified) {
          emptyList()
        } else {
          authored.arguments.zip(authoredClass.matchDependencyKeys(authored.arguments, classTable))
        }
    val retainedArguments = authoredArguments.filter { (argument, key) ->
      key !in representedKeys &&
          argument.descendantsOfType<Expression>().any { it.typeVariableName != null }
    }
    val applied = expression.appendArguments(retainedArguments.map { it.first })
    return if (
        authored.typeVariableName != null &&
            authored.argumentsSpecified &&
            !applied.argumentsSpecified
    ) {
      applied.copy(argumentsSpecified = true)
    } else {
      applied
    }
  }

  /**
   * Rule L12-14: a change to a type this game cannot hold becomes `Die` or `Ok`. An invalid
   * post-specialization type becomes `Die`. A resolved but uninhabited type becomes `Die` when the
   * change is mandatory and `Ok` when it permits zero.
   */
  private fun invalidChangesToDie(openVariables: () -> TypeVariableScope): PetTransformer {
    return object : PetTransformer() {
      private val remainingVariables by lazy(LazyThreadSafetyMode.NONE, openVariables)

      override fun transformNode(node: PetNode): PetNode {
        if (node is Instruction.Then && !node.typeVariables.isEmpty) {
          val nested = invalidChangesToDie { remainingVariables + node.typeVariables }
          return node.withParts(
              node.stages.map(nested::transformInstruction),
              nested.transformInstructionTree(node.continuation),
          )
        }
        if (node is Each) {
          val selector = transformExpression(node.selector)
          val body = transformInstructionTree(node.body)
          return if (body is NoOp) NoOp else Each(selector, body)
        }
        if (node is Per) {
          val inner = transformInstruction(node.inner)
          return if (inner is NoOp) NoOp else Per(inner, transformMetric(node.metric))
        }
        val specialized = transformChildren(node)
        if (specialized !is Change) return specialized

        try {
          val expressions = listOfNotNull(specialized.gaining, specialized.removing)
          val visibleVariables = remainingVariables + specialized.typeVariables
          if (
              !visibleVariables.isEmpty &&
                  expressions.any { expression ->
                    expression.descendantsOfType<Expression>().any {
                      visibleVariables.variableAt(it) != null
                    }
                  }
          ) {
            return specialized
          }
          val types = expressions.map(classTable::resolve)
          if (types.any { !classTable.isInhabited(it) }) {
            return if (specialized.quantifier == MANDATORY) {
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
