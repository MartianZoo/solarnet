package dev.martianzoo.pets

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Resolution
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
 * [section 9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration).
 * Elaboration fills in what a physical game leaves implicit — that a tile goes on a land area, that
 * a resource belongs to the player doing the thing, that "gain 3 cards" means three separate cards.
 * It changes how a source *reads*; it never changes which types exist.
 *
 * Rule
 * [L9-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration)
 * fixes the meaningful order: split atomized gains before defaults, dispatch transform blocks
 * before final Type-variable scope recording, then insert lexical ownership. Entry points supply
 * different contexts and property-evaluation policies.
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
   * [rule L9-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration),
   * a submitted element defaults against `This`, atomizes before defaulting, and binds the lexical
   * `Me` to the submitting player. An Instruction is treated as the broader InstructionTree family,
   * so its cardinality may change.
   *
   * Property evaluation is rejected here, because an ordinary submitted instruction has no receiver
   * context to expand against ([rule
   * L9-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration));
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
   * L9-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration)).
   */
  public fun elaborateMetricInput(
      input: Metric,
      context: Expression,
      owner: HasClassName? = null,
  ): Metric =
      chain(
              normalizeInput(),
              finishAuthoredSyntax(context, owner),
              propertyEvaluator(context),
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
          bindFreeMe(owner),
          transformDispatcher(),
          classTable.recordTypeVariableScopes(),
          insertOwnedContext(owner?.className?.expression, context),
      )

  /**
   * Effects inherited by [klass], processed as far as possible without a concrete component. Per
   * [rule L9-13](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration)
   * a class's effects are elaborated against that class's own context and gathered from every
   * superclass; inherited lexical names are bound when [specializeEffect] closes each effect over
   * an exact component later.
   */
  public fun classEffects(klass: Class): List<Effect> {
    require(classTable.isIncluded(klass)) { "`$klass` is not included in this game" }
    return effectsByClass.getOrPut(klass) {
      val evaluator = propertyEvaluator(context = klass.defaultExpression, deferAbstract = true)
      fun directClassEffects(source: Class) =
          classTable.effects(source).map { effect ->
            try {
              val lowered =
                  attachToClassTransformer(source)
                      .transformEffect(source.interpretTypeVariablesIn(effect))
              val expanded = refreshClassScope(source, lowered).let(evaluator::transformEffect)
              val me = lexicalMe(source) ?: triggerMe(expanded.trigger)
              refreshClassScope(
                  source,
                  insertOwnedContext(me, source.className.expression).transformEffect(expanded),
              )
            } catch (e: PetException) {
              throw InvalidPetDefinitionException(
                  "invalid effect declared by `${source.className}`: `$effect`: ${e.detail}",
                  e,
                  e.sourceLocation ?: effect.sourceLocation ?: source.className.sourceLocation,
              )
            }
          }

      klass.allSuperclasses().flatMap(::directClassEffects)
    }
  }

  /**
   * Rejects property evaluation syntax outside a class effect ([rule
   * L9-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration)).
   */
  private fun rejectPropertyEvaluations(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            when (node) {
              is Metric.Eval,
              is Requirement.Eval ->
                  throw PetSyntaxException(
                      "`EVAL` is valid only in a class effect or a submitted metric; it cannot appear in a submitted instruction",
                      sourceLocation = node.sourceLocation,
                  )
              else -> transformChildren(node)
            }
      }

  /**
   * Expands Class-property syntax in the supplied instruction context ([rule
   * L9-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration)).
   * An evaluation reached here requires a fixed property value; a value that is still only a bound
   * is an error. [classEffects] may retain such evaluations for later specialization.
   */
  public fun evaluateProperties(
      input: InstructionTree,
      context: Expression,
  ): InstructionTree = propertyEvaluator(context).transformInstructionTree(input)

  /** Expands Class-property syntax in the supplied Metric context, requiring fixed values. */
  public fun evaluateProperties(
      input: Metric,
      context: Expression,
  ): Metric = propertyEvaluator(context).transformMetric(input)

  /**
   * Expands explicit property syntax in context, optionally retaining evaluations for later
   * specialization. Fanout bodies wait until their selected component has been bound.
   */
  private fun propertyEvaluator(
      context: Expression,
      deferAbstract: Boolean = false,
  ): PetTransformer {
    val expanding = mutableSetOf<Pair<Expression, PropertyName>>()
    val contextualizer = replaceThisExpressionsWith(context)
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        // The selected component supplies a fanout branch's context, so its property evaluations
        // must remain inert until Instructor has bound that selection. The selector itself still
        // belongs to the enclosing context and is transformed normally.
        if (node is Each) {
          return Each(transformExpression(node.selector), node.body)
        }
        // A RANK key is evaluated once per candidate. Expanding its properties before then
        // loses the candidate that supplies a lexical Me to the property's value.
        if (node is Metric.Rank) {
          if (deferAbstract) return node
          return node
              .copy(selector = node.selector?.let(::transformExpression))
              .withTypeVariables(node.typeVariables)
              .also { it.sourceLocation = node.sourceLocation }
        }
        val property =
            when (node) {
              is Metric.Eval -> node.property
              is Requirement.Eval -> node.property
              else -> return transformChildren(node)
            }
        val me =
            when (node) {
              is Metric.Eval -> node.me
              is Requirement.Eval -> node.me
              else -> error("unsupported property evaluation syntax")
            }
        val contextualProperty =
            bindFreeMe(me).transformProperty(contextualizer.transformProperty(property))
        val receiver =
            contextualProperty.receiver
                ?: throw ExpressionException(
                    "evaluated property `${contextualProperty.propertyName}` has no receiver",
                    sourceLocation = property.sourceLocation,
                )

        val receiverType = classTable.resolve(receiver)
        val propertyType = receiverType.representedClass?.baseType ?: receiverType
        val propertyClass = propertyType.rootClass
        val value =
            propertyClass.properties[contextualProperty.propertyName]
                ?: throw ExpressionException(
                    "class `${propertyClass.className}` has no property " +
                        "`${contextualProperty.propertyName}`; available properties: " +
                        propertyClass.properties.keys.joinToString { "`$it`" }.ifEmpty { "none" },
                    sourceLocation =
                        property.propertyName.sourceLocation ?: property.sourceLocation,
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
                        throw ExpressionException(
                            "property `${contextualProperty.propertyName}` is not a concrete metric on " +
                                "`${propertyClass.className}`; found `$value`",
                            sourceLocation =
                                property.propertyName.sourceLocation ?: property.sourceLocation,
                        )
                  }
              is Requirement.Eval ->
                  when (value) {
                    AbsentRequirementValue -> Min(scaledEx(COMPONENT, 1))
                    is RequirementValue -> value.value
                    else ->
                        throw ExpressionException(
                            "property `${contextualProperty.propertyName}` is not a concrete requirement on " +
                                "`${propertyClass.className}`; found `$value`",
                            sourceLocation =
                                property.propertyName.sourceLocation ?: property.sourceLocation,
                        )
                  }
              else -> error("unsupported property evaluation syntax: `${node::class.simpleName}`")
            }

        if (
            deferAbstract &&
                syntax.descendantsOfType<Expression>().any {
                  it.typeVariableName?.let { marker ->
                    marker.name == "Me" && marker.resolution == null
                  } == true
                }
        )
            return node

        val key = propertyType.expressionFull to contextualProperty.propertyName
        if (!expanding.add(key)) {
          throw ExpressionException(
              "property `${contextualProperty.propertyName}` is recursive on " +
                  "`${propertyType.expressionFull}`; expansion: " +
                  (expanding.toList() + key).joinToString(" -> ") { (receiver, name) ->
                    "`$receiver.$name`"
                  },
              sourceLocation = property.propertyName.sourceLocation ?: property.sourceLocation,
          )
        }
        val expanded: PetNode =
            try {
              val transformer =
                  chain(
                      replaceThisExpressionsWith(propertyType.expressionFull),
                      bindFreeMe(me),
                      insertOwnedContext(me, propertyType.expressionFull),
                  )
              when (syntax) {
                is Metric -> transformMetric(transformer.transformMetric(syntax))
                is Requirement -> transformRequirement(transformer.transformRequirement(syntax))
                else ->
                    error("unsupported property evaluation syntax: `${syntax::class.simpleName}`")
              }
            } finally {
              expanding.remove(key)
            }
        val finishing =
            chain(
                atomizer(),
                insertDefaults(context),
                bindFreeMe(me),
                transformDispatcher(),
                insertOwnedContext(me, context),
            )
        return when (expanded) {
          is Metric -> finishing.transformMetric(expanded)
          is Requirement -> finishing.transformRequirement(expanded)
          else -> error("property evaluation produced `${expanded::class.simpleName}`")
        }
      }
    }
  }

  /** A property may use the explicitly named `Me` supplied by its evaluation site. */
  private fun bindFreeMe(owner: HasClassName?): PetTransformer {
    if (owner == null) return noOp()
    val selected = owner.className.expression
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node is Expression) {
          val marker = node.typeVariableName
          if (marker?.name == "Me" && marker.resolution == null) {
            val bound = classTable.getClass(marker.boundClassName)
            if (!classTable.getClass(owner.className).isSubtypeOf(bound)) {
              throw ExpressionException("`$selected` cannot supply `${marker.authoredSpelling}`")
            }
            return selected
                .copy(
                    arguments = node.arguments,
                    refinement = node.refinement,
                    argumentsSpecified = node.argumentsSpecified,
                )
                .also { it.sourceLocation = node.sourceLocation }
          }
        }
        return transformChildren(node)
      }
    }
  }

  private fun attachToClassTransformer(klass: Class): PetTransformer {
    val context = klass.className.has(Min(scaledEx(OK, 1)))
    val representedClassMarkers =
        klass.typeVariables
            .filter { it.selectsClass }
            .mapNotNull { it.declaration.expression.typeVariableName?.identity }
            .toSet()
    return chain(
        atomizer(),
        insertDefaults(context),
        transformDispatcher(),
        classTable.recordTypeVariableScopes(),
        insertOwnedContext(lexicalMe(klass), klass.className.expression, representedClassMarkers),
        classTable.recordTypeVariableScopes(),
    )
  }

  private fun lexicalMe(klass: Class): Expression? {
    val named = klass.typeVariables.filter { it.name == "Me" }
    if (named.isEmpty()) return null
    val declared =
        named
            .firstOrNull { candidate ->
              val bound = classTable.getClass(candidate.declaration.expression.className)
              named.all { other ->
                bound.isSubtypeOf(classTable.getClass(other.declaration.expression.className))
              }
            }
            ?.declaration
            ?.expression
            ?: throw ExpressionException(
                "`${klass.className}` has no single narrowest `Me` binding"
            )
    val marker = requireNotNull(declared.typeVariableName as? Declaration)
    return declared.copy(
        typeVariableName =
            Reference(marker.name, marker.boundClassName)
                .resolved(requireNotNull(marker.resolution))
    )
  }

  private fun triggerMe(trigger: Effect.Trigger): Expression? {
    val declaration =
        trigger
            .descendantsOfType<Expression>()
            .mapNotNull { it.typeVariableName as? Declaration }
            .firstOrNull { it.name == "Me" } ?: return null
    return Expression(
        declaration.boundClassName,
        typeVariableName =
            Reference(declaration.name, declaration.boundClassName)
                .resolved(requireNotNull(declaration.resolution)),
    )
  }

  /** Re-observes header names introduced by lowering, without losing effect-local choices. */
  private fun refreshClassScope(source: Class, effect: Effect): Effect {
    val header = source.typeVariablesIn(effect)
    fun markerIdentity(variable: dev.martianzoo.pets.types.TypeVariable): Any =
        variable.declaration.expression.typeVariableName?.identity ?: variable
    val headerIdentities = header.variables.mapTo(mutableSetOf(), ::markerIdentity)
    val locals =
        effect.typeVariables.retaining(
            effect.typeVariables.variables
                .filter { markerIdentity(it) !in headerIdentities }
                .toSet()
        )
    val refreshed = TypeVariableScope.containing((header + locals).variables, effect)
    return classTable
        .recordTypeVariableScopes()
        .transformEffect(effect.copy().withTypeVariables(refreshed))
  }

  /** Inserts the owner dependency from the nearest lexical `Me`, including an effect's trigger. */
  private fun insertOwnedContext(
      outerMe: Expression?,
      context: Expression,
      representedClassMarkers: Set<Any> = emptySet(),
  ): PetTransformer {
    val owned = classTable.findClass(OWNED) ?: return noOp()
    val ownerKey = owned.dependencies.keys.single()
    var activeRepresentedClassMarkers = representedClassMarkers
    val refinementCandidates = mutableListOf<Expression?>()

    fun <T> withinRefinementCandidate(candidate: Expression?, block: () -> T): T {
      refinementCandidates.add(candidate)
      return try {
        block()
      } finally {
        refinementCandidates.removeLast()
      }
    }

    fun matchedKeys(expression: Expression): List<Key> {
      val klass = classTable.getClass(expression.className)
      val contextualArguments =
          expression.arguments.map { argument ->
            replaceThisExpressionsWith(context).transformExpression(argument)
          }
      return klass.argumentDependencies.matchPartial(contextualArguments, classTable).keys
    }

    fun missingOwningArgument(expression: Expression): Boolean {
      val klass = classTable.findClass(expression.className) ?: return false
      if (!klass.isSubtypeOf(owned)) return false
      val marker = expression.typeVariableName
      if (
          marker != null &&
              !(marker is Reference && marker.identity in activeRepresentedClassMarkers) &&
              marker !is Declaration
      )
          return false
      if (ownerKey !in klass.argumentDependencies.keys) return false
      val supplied = matchedKeys(expression)
      if (ownerKey in supplied || klass.dependencyDeterminedBy(ownerKey, supplied)) return false
      val candidate = refinementCandidates.lastOrNull() ?: return true
      val candidateKey =
          try {
            klass.matchDependencyKeys(listOf(candidate), classTable).single()
          } catch (_: ExpressionException) {
            return true
          }
      return ownerKey != candidateKey &&
          !klass.dependencyDeterminedBy(ownerKey, supplied + candidateKey)
    }

    fun insert(expression: Expression, me: Expression): Expression {
      if (!missingOwningArgument(expression)) return expression
      val klass = classTable.getClass(expression.className)
      val existing = matchedKeys(expression).zip(expression.arguments).toMap()
      val arguments =
          klass.argumentDependencies.keys.mapNotNull { key ->
            existing[key] ?: me.takeIf { key == ownerKey }
          }
      val marker = expression.typeVariableName
      val insertedMarker =
          if (marker is Reference) {
            Reference(marker.name, marker.boundClassName, argumentsSpecified = true)
                .resolved(requireNotNull(marker.resolution))
          } else marker
      return expression
          .copy(
              arguments = arguments,
              argumentsSpecified = true,
              typeVariableName = insertedMarker,
          )
          .also {
            it.sourceLocation = expression.sourceLocation
          }
    }

    fun selectedMe(selector: Expression): Expression? {
      val marker = selector.typeVariableName as? Declaration ?: return null
      if (marker.name != "Me") return null
      return selector.copy(
          refinement = null,
          typeVariableName =
              Reference(marker.name, marker.boundClassName)
                  .resolved(requireNotNull(marker.resolution)),
      )
    }

    fun representedInTrigger(trigger: Effect.Trigger): Set<Any> =
        trigger
            .descendantsOfType<Expression>()
            .filter { it.className == CLASS }
            .flatMap { it.arguments }
            .mapNotNull { it.typeVariableName?.identity }
            .toSet()

    fun transformRefinementForCandidate(
        transformer: PetTransformer,
        refinement: Expression.Refinement,
        candidate: Expression,
    ): Expression.Refinement =
        when (refinement) {
          is Expression.Refinement.And ->
              Expression.Refinement.create(
                  refinement.refinements.map {
                    transformRefinementForCandidate(transformer, it, candidate)
                  }
              )
          is Has -> {
            withinRefinementCandidate(candidate) {
              transformer.transformRefinement(refinement)
            }
          }
          is Expression.Refinement.Not -> transformer.transformRefinement(refinement)
        }

    fun needsMe(node: PetNode, available: Boolean = false): Boolean {
      if (node is Each) {
        val selected = available || selectedMe(node.selector) != null
        return needsMe(node.selector, selected) || needsMe(node.body, selected)
      }
      if (node is Metric.Rank) {
        val selected = available || node.selector?.let(::selectedMe) != null
        return withinRefinementCandidate(null) {
          (node.selector?.let { needsMe(it, selected) } ?: false) ||
              node.metrics.any { needsMe(it, selected) }
        }
      }
      if (node is Expression) {
        if (node.className == CLASS) {
          activeRepresentedClassMarkers +=
              node.arguments.mapNotNull { it.typeVariableName?.identity }
          return node.refinement?.let { needsMe(it, available) } ?: false
        }
        if (missingOwningArgument(node) && !available) return true
        if (withinRefinementCandidate(null) { node.arguments.any { needsMe(it, available) } })
            return true
        val candidate = node.copy(refinement = null)
        fun needsMeInRefinement(refinement: Expression.Refinement): Boolean =
            when (refinement) {
              is Expression.Refinement.And -> refinement.refinements.any(::needsMeInRefinement)
              is Has -> {
                withinRefinementCandidate(candidate) {
                  needsMe(refinement.requirement, available)
                }
              }
              is Expression.Refinement.Not -> needsMe(refinement.excluded, available)
            }
        return node.refinement?.let(::needsMeInRefinement) ?: false
      }
      return node.immediateChildren().any { needsMe(it, available) }
    }

    fun supplyFromTrigger(trigger: Effect.Trigger): Pair<Effect.Trigger, Expression> {
      if (trigger.descendantsOfType<Effect.Trigger.Or>().any()) {
        throw PetSyntaxException(
            "an OR trigger cannot implicitly supply `Me`; bind `Me` around the whole trigger",
            sourceLocation = trigger.sourceLocation,
        )
      }
      val resolution = Resolution()
      val declaration =
          Expression(
              dev.martianzoo.pets.api.SystemClasses.PLAYER,
              typeVariableName =
                  Declaration("Me", dev.martianzoo.pets.api.SystemClasses.PLAYER)
                      .resolved(resolution),
          )
      val me =
          Expression(
              dev.martianzoo.pets.api.SystemClasses.PLAYER,
              typeVariableName =
                  Reference("Me", dev.martianzoo.pets.api.SystemClasses.PLAYER)
                      .resolved(resolution),
          )
      var supplied = false
      val marked =
          object : PetTransformer() {
                override fun transformNode(node: PetNode): PetNode {
                  if (!supplied && node is OnGainOf && missingOwningArgument(node.expression)) {
                    supplied = true
                    return OnGainOf.create(insert(node.expression, declaration))
                  }
                  if (!supplied && node is OnRemoveOf && missingOwningArgument(node.expression)) {
                    supplied = true
                    return OnRemoveOf.create(insert(node.expression, declaration))
                  }
                  return transformChildren(node)
                }
              }
              .transformTrigger(trigger)
      return (if (supplied) marked else ByTrigger(marked, declaration)) to me
    }

    return object : PetTransformer() {
      private var me: Expression? = outerMe

      override fun transformNode(node: PetNode): PetNode {
        if (node is Effect) {
          val declaredInTrigger = if (me == null) triggerMe(node.trigger) else null
          val supplied =
              if (me == null && declaredInTrigger == null && needsMe(node.instruction))
                  supplyFromTrigger(node.trigger)
              else null
          val previous = me
          val previousRepresented = activeRepresentedClassMarkers
          me = supplied?.second ?: declaredInTrigger ?: previous
          activeRepresentedClassMarkers =
              previousRepresented +
                  node.typeVariables.variables
                      .filter { it.selectsClass }
                      .mapNotNull { it.declaration.expression.typeVariableName?.identity } +
                  representedInTrigger(node.trigger)
          return try {
            val trigger = transformTrigger(supplied?.first ?: node.trigger)
            node
                .copy(trigger = trigger, instruction = transformInstructionTree(node.instruction))
                .withTypeVariables(node.typeVariables.transformedBy(this))
          } finally {
            me = previous
            activeRepresentedClassMarkers = previousRepresented
          }
        }
        if (node is Metric.Eval) {
          return node.copy(property = transformProperty(node.property), me = node.me ?: me).also {
            it.sourceLocation = node.sourceLocation
          }
        }
        if (node is Requirement.Eval) {
          return node.copy(property = transformProperty(node.property), me = node.me ?: me).also {
            it.sourceLocation = node.sourceLocation
          }
        }
        if (node is Each) {
          val previous = me
          me = selectedMe(node.selector) ?: previous
          return try {
            val selector = transformExpression(node.selector)
            Each(selector, transformInstructionTree(node.body))
                .withTypeVariables(node.typeVariables)
                .also {
                  it.sourceLocation = node.sourceLocation
                }
          } finally {
            me = previous
          }
        }
        if (node is Metric.Rank) {
          val previous = me
          return withinRefinementCandidate(null) {
            try {
              val selector = node.selector?.let(::transformExpression)
              me = selector?.let(::selectedMe) ?: previous
              node
                  .copy(
                      selector = selector,
                      metrics = node.metrics.map(::transformMetric),
                  )
                  .withTypeVariables(node.typeVariables)
                  .also {
                    it.sourceLocation = node.sourceLocation
                  }
            } finally {
              me = previous
            }
          }
        }
        if (node is Compact) {
          val transformed = withinRefinementCandidate(null) { transformChildren(node) as Compact }
          val currentMe = me ?: return transformed
          if (!missingOwningArgument(transformed.toExpression)) return transformed
          val klass = classTable.getClass(transformed.className)
          val byKey = matchedKeys(transformed.toExpression).zip(transformed.arguments).toMap()
          return transformed.copy(
              arguments =
                  klass.argumentDependencies.keys.mapNotNull { key ->
                    byKey[key] ?: Unchanged(currentMe).takeIf { key == ownerKey }
                  }
          )
        }
        if (node is Expression) {
          if (node.className == CLASS) {
            activeRepresentedClassMarkers +=
                node.arguments.mapNotNull { it.typeVariableName?.identity }
          }
          val shell =
              if (node.className == CLASS) node.copy(refinement = null)
              else
                  withinRefinementCandidate(null) {
                    transformChildren(node.copy(refinement = null)) as Expression
                  }
          val ownedShell = me?.let { insert(shell, it) } ?: shell
          val refinement =
              node.refinement?.let {
                transformRefinementForCandidate(this, it, ownedShell)
              }
          return ownedShell.copy(refinement = refinement).also {
            it.sourceLocation = node.sourceLocation
          }
        }
        return transformChildren(node)
      }
    }
  }

  private fun useFullNames(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          return if (node is ClassName) {
            val resolved = classTable.resolve(node.expression).className
            if (resolved == node) node
            else cn(resolved.toString()).also { it.sourceLocation = node.sourceLocation }
          } else {
            transformChildren(node)
          }
        }
      }

  /**
   * Rule L9-11: a gain of several `Atomized` components becomes several gains of one, because three
   * cards are three separate things to choose.
   */
  public fun atomizeGains(input: InstructionTree): InstructionTree =
      atomizer().transformInstructionTree(input)

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
        if (result !== node) result.sourceLocation = node.sourceLocation
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

      // Rule L9-8: the gained and removed projections are defaulted independently, and where the
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

      /** The stricter of the two, per rule L9-8: mandatory beats AMAP, which beats optional. */
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
   * Rule L9-5: a gain must opt in. Where a class has gain dependency defaults, a gain may not leave
   * its argument list implicit — `OceanTile<>` accepts them — so a defaulted placement stays
   * visible at the point of use. A removal declines by writing nothing instead (L9-6), so this is
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
      throw ExpressionException(
          "`${expression.className}` has $kind dependency defaults; write " +
              "`${expression.className}<>` to accept them or provide dependency arguments",
          sourceLocation = expression.sourceLocation,
      )
    }
  }

  /**
   * Rule L9-7: `Foo<>` is invalid where that use has no dependency defaults to accept. An empty
   * list is an acceptance, not merely a second spelling, so `Plant<>` cannot honestly mean
   * anything.
   */
  private fun rejectEmptyArgumentsWithoutDefaults(
      expression: Expression,
      default: DefaultSpec,
      kind: String,
  ) {
    if (
        ((expression.typeVariableName as? Reference)?.argumentsSpecified
            ?: expression.argumentsSpecified) &&
            expression.arguments.isEmpty() &&
            default.dependencies.keys.isEmpty() &&
            classTable.getClass(expression.className).defaults.allUsages.dependencies.keys.isEmpty()
    ) {
      throw ExpressionException(
          "`${expression.className}<>` has no $kind dependency defaults to accept",
          sourceLocation = expression.sourceLocation,
      )
    }
  }

  /**
   * Rule L9-6: a removal declines its use-specific defaults by writing nothing. An implicit
   * argument list on a removal is not an error — it simply does not receive the removal-only
   * defaults, though all-use defaults (L9-4) still apply.
   */
  private fun hasUnacceptedDependencyDefaults(
      expression: Expression,
      default: DefaultSpec,
  ): Boolean =
      default.dependencies.keys.isNotEmpty() &&
          expression.arguments.isEmpty() &&
          !expression.argumentsSpecified

  /**
   * Rule L9-4: every expression receives its class's all-use dependency defaults, recursively.
   *
   * During this pass, an outermost dependent expression written without arguments reserves its
   * first candidate-compatible slot instead of accepting that slot's default (L9-9). Writing any
   * argument list, including `<>`, keeps ordinary default handling. Independently, L9-10 defers a
   * direct class-header-variable default throughout a refinement.
   */
  private fun insertExpressionDefaults(context: Expression): PetTransformer {
    var refinementDepth = 0
    val refinementCandidates = mutableListOf<Expression?>()

    fun <T> withinRefinementCandidate(candidate: Expression?, block: () -> T): T {
      refinementCandidates.add(candidate)
      return try {
        block()
      } finally {
        refinementCandidates.removeLast()
      }
    }
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
              withinRefinementCandidate(candidate) {
                transformRefinement(refinement)
              }
            }
            is Expression.Refinement.Not -> transformRefinement(refinement)
          }

      fun defaultShell(shell: Expression): Expression {
        val klass = classTable.getClass(shell.className)
        val defaultDeps = klass.defaults.allUsages.dependencies
        rejectEmptyArgumentsWithoutDefaults(shell, klass.defaults.allUsages, "all-use")
        val refinementCandidate = refinementCandidates.lastOrNull()
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
          return withinRefinementCandidate(null) { transformChildren(node) }
        }
        if (node is Compact) {
          val shell =
              Compact(
                  transformClassName(node.className),
                  withinRefinementCandidate(null) {
                    node.arguments.map(::transformFromExpression)
                  },
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

        val shell =
            withinRefinementCandidate(null) {
              transformChildren(
                  node.copy(refinement = null).also { it.sourceLocation = node.sourceLocation }
              )
                  as Expression
            }
        val defaulted = defaultShell(shell)
        val refinement =
            node.refinement?.let {
              transformRefinementForCandidate(it, defaulted.copy(refinement = null))
            }
        return defaulted.copy(refinement = refinement).also {
          it.sourceLocation = node.sourceLocation
        }
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
    val match: DependencySet =
        klass.argumentDependencies.matchPartial(dethissed.arguments, classTable)

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
        klass.argumentDependencies.keys.mapNotNull {
          preferred[it] ?: fallbacks[it]?.takeUnless { _ -> it in inferred }
        }

    return original
        .copy(
            arguments = newArgs,
            argumentsSpecified = original.argumentsSpecified || newArgs.isNotEmpty(),
        )
        .also {
          it.sourceLocation = original.sourceLocation
          require(it.className == original.className)
          require(it.refinement == original.refinement)
          require(it.arguments.containsAll(original.arguments))
        }
  }

  /**
   * Closes one Class Effect over its exact component Type and `This` context, in one step ([rule
   * L9-15](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-elaboration),
   * [rule T13-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  public fun specializeEffect(
      general: Type,
      specific: Type,
      effect: Effect,
      context: Expression,
  ): Effect {
    val contextualizer =
        chain(
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
  ): PetTransformer {
    val bindings =
        typeVariables.bindingsFrom(
            authoredGeneral,
            general.groundType,
            specific.groundType,
            classTable,
        )
    val binder = typeVariables.bind(bindings, classTable)
    return chain(
        binder,
        invalidChangesToDie { typeVariables.transformedBy(binder) },
    )
  }

  /**
   * Retains source provenance and explicitly authored Type-variable occurrences when Type
   * resolution rebuilds [resolved] in compact form. Arguments are matched by dependency key, and an
   * authored variable argument omitted only because it equals the Class default remains present.
   */
  public fun retainTypeVariableNames(
      resolved: Expression,
      authored: Expression,
  ): Expression {
    if (authored.descendantsOfType<Expression>().none { it.typeVariableName != null }) {
      return resolved.copy().also { it.sourceLocation = authored.sourceLocation }
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
          refinement =
              if (target.refinement == source.refinement) source.refinement else target.refinement,
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

  /** Rule L9-14: a change to a type this game cannot hold becomes `Die` or `Ok`. */
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
          return if (specialized.quantifier == MANDATORY) gain(DIE) else NoOp
        }
        return specialized
      }
    }
  }
}
