package dev.martianzoo.engine

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.Vocabulary
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
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
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

public class Transformers(public val classTable: ClassTable) {
  // TODO: Contract temporary tfm-tests transformation seams.

  private val effectsByClass = mutableMapOf<Class, List<Effect>>()
  private val transformDispatcher by lazy { classTable.transformDispatcher() }

  /** Expands the marked Pets syntax configured by this game's Catalog. */
  public fun transformMarkedSyntax(): PetTransformer = transformDispatcher

  /** Rewrites session-localized input names to their canonical engine names. */
  public fun canonicalize(vocabulary: Vocabulary): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            if (node is ClassName) vocabulary.canonicalName(node) else transformChildren(node)
      }

  /** Effects inherited by [klass], processed as far as possible without a concrete component. */
  internal fun classEffects(klass: Class): List<Effect> {
    require(classTable.isActive(klass)) { "$klass is not active in this game" }
    return effectsByClass.getOrPut(klass) {
      fun directClassEffects(source: Class) =
          source.declaration.effects.map(attachToClassTransformer(source)::transformEffect)

      val evaluator =
          evaluateProperties(
              context = klass.defaultType.expressionFull,
              deferAbstract = true,
          )
      klass.allSuperclasses().flatMap(::directClassEffects).map(evaluator::transformEffect)
    }
  }

  /** Rejects property evaluation syntax outside a class effect. */
  internal fun rejectPropertyEvaluations(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            when (node) {
              is Metric.Eval,
              is Requirement.Eval ->
                  throw PetSyntaxException("EVAL is valid only inside a class effect")
              else -> transformChildren(node)
            }
      }

  /** Expands explicit property evaluations after their receivers have become concrete. */
  internal fun evaluateProperties(
      context: Expression,
      owner: HasClassName? = null,
      deferAbstract: Boolean = false,
  ): PetTransformer {
    val expanding = mutableSetOf<Pair<Expression, PropertyName>>()
    val contextualizer =
        chain(
            replaceThisExpressionsWith(context),
            owner?.let(::replaceOwnerWith),
        )
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
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
                      owner?.let(::replaceOwnerWith),
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
                owner?.let(::replaceOwnerWith),
                transformDispatcher,
            )
        return when (expanded) {
          is Metric -> finishing.transformMetric(expanded)
          is Requirement -> finishing.transformRequirement(expanded)
          else -> error("checked above")
        }
      }
    }
  }

  private fun attachToClassTransformer(klass: Class): PetTransformer {
    val context = klass.className.has(Min(scaledEx(OK, 1)))
    return chain(
        insertDefaults(context),
        atomizer(),
        transformDispatcher,
        fixEffectForUnownedContext(klass),
    )
  }

  private fun fixEffectForUnownedContext(klass: Class): PetTransformer? {
    if (klass.allSuperclasses().any { it.className == OWNED || it.className == OWNER }) return null
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        return if (node is Effect && OWNER in node.instruction && OWNER !in node.trigger) {
          node.copy(trigger = ByTrigger(node.trigger, OWNER))
        } else {
          transformChildren(node)
        }
      }
    }
  }

  public fun useFullNames(): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          return if (node is ClassName) {
            classTable.resolve(node.expression).className
          } else {
            transformChildren(node)
          }
        }
      }

  @Suppress("ComplexCondition") // TODO: fix that
  public fun atomizer(): PetTransformer {
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

  public fun insertDefaults(): PetTransformer = insertDefaults(THIS.expression)

  public fun insertDefaults(context: Expression): PetTransformer =
      chain(
          insertTriggerDefaults(context),
          insertGainRemoveDefaults(context),
          insertExpressionDefaults(context),
      )

  private fun insertTriggerDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode =
          when (node) {
            is OnGainOf -> applyTriggerDefault(node, node.expression)
            is OnRemoveOf -> applyTriggerDefault(node, node.expression)
            else -> transformChildren(node)
          }

      private fun applyTriggerDefault(node: Effect.Trigger, original: Expression): Effect.Trigger {
        val default = classTable.getClass(original.className).defaults.triggerOnly
        requireExplicitDependencyDefaults(original, default, "trigger")
        val fixed = insertDefaultsIntoExpr(original, default.dependencies, context, classTable)
        val replacer =
            object : PetTransformer() {
              override fun transformNode(node: PetNode): PetNode =
                  if (node === original) fixed else transformChildren(node)
            }
        return replacer.transformTrigger(node)
      }
    }
  }

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

  private fun hasUnacceptedDependencyDefaults(
      expression: Expression,
      default: DefaultSpec,
  ): Boolean =
      default.dependencies.keys.isNotEmpty() &&
          expression.arguments.isEmpty() &&
          !expression.argumentsSpecified

  public fun insertExpressionDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node
        if (node.hasDeferredOwnerComplement()) return node

        val defaultDeps = classTable.getClass(node.className).defaults.allUsages.dependencies
        val result =
            insertDefaultsIntoExpr(
                transformChildren(node) as Expression,
                defaultDeps,
                context,
                classTable,
            )
        return result
      }
    }
  }

  internal fun insertDeferredComplementDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node !is Expression) return transformChildren(node)
        if (!node.hasComplement()) return node

        val transformed = transformChildren(node) as Expression
        val defaultDeps = classTable.getClass(node.className).defaults.allUsages.dependencies
        val result = insertDefaultsIntoExpr(transformed, defaultDeps, context, classTable)
        return result
      }
    }
  }

  private fun Expression.hasDeferredOwnerComplement(): Boolean =
      (complement && className == OWNER) || arguments.any { it.hasDeferredOwnerComplement() }

  private fun Expression.hasComplement(): Boolean =
      complement || arguments.any { it.hasComplement() }

  private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)

  // only has to modify the args/specs
  private fun insertDefaultsIntoExpr(
      original: Expression,
      defaultDeps: DependencySet,
      contextCpt: Expression,
      classTable: ClassTable,
  ): Expression {

    val klass: Class = classTable.getClass(original.className)
    val dethissed: Expression = replaceThisExpressionsWith(contextCpt).transformExpression(original)
    val match: DependencySet = klass.dependencies.matchPartial(dethissed.arguments)

    val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
    val fallbacks: Map<Key, Expression> =
        defaultDeps.typeDependencies().associate { it.key to it.expression }
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

  internal fun substituter(general: Type, specific: Type): PetTransformer {
    val gendeps = general.dependencies
    val specdeps = specific.dependencies
    val subs = findSubstitutions(gendeps, specdeps)

    return substituter(subs)
  }

  private fun substituter(
      subs: Map<ClassName, Expression>,
      preserved: Set<Expression> = emptySet(),
  ): PetTransformer {
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node is Expression && node in preserved) return node
        if (node is Expression) {
          val transformed = transformChildren(node) as Expression
          val replacement: Expression? = subs[transformed.className]
          if (replacement != null) {
            val expr: Expression =
                replacement
                    .appendArguments(transformed.arguments)
                    .copy(refinement = transformed.refinement, complement = transformed.complement)
            return expr
          }
          return transformed
        }
        return transformChildren(node)
      }
    }
  }

  /**
   * Specializes linked type names and normalizes atomic changes made invalid by that
   * specialization. Optional phantom changes become `Ok`; dead changes become `Die` so enclosing
   * choices can discard them.
   */
  public fun checkedSubstituter(
      general: Type,
      specific: Type,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    return chain(
        listOf(substituter(specializationSubstitutions(general, specific))) +
            afterSubstitution +
            invalidChangesToDie()
    )
  }

  /**
   * Specializes a class effect while retaining complete values for abstract class-header
   * dependencies used by that effect. Those occurrences are variables linked to the header, not
   * ordinary requests to replace every instance of the same abstract Class.
   */
  internal fun checkedEffectSubstituter(
      general: Type,
      specific: Type,
      effect: Effect,
      eventLinkedSources: Set<Expression>,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    val expressions = effect.descendantsOfType<Expression>().toSet()
    val commonPaths =
        general.dependencies.flatten().keys.intersect(specific.dependencies.flatten().keys)
    val dependencyBindings =
        commonPaths
            .mapNotNull { path ->
              val source = general.dependencies.at(path).expression
              val replacement = specific.dependencies.at(path).expressionFull
              if (
                  source.simple &&
                      classTable.getClass(source.className).abstract &&
                      source in expressions &&
                      source !in eventLinkedSources &&
                      replacement != source
              ) {
                source to replacement
              } else {
                null
              }
            }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (source, replacements) ->
              replacements.distinct().singleOrNull()?.let { source to it }
            }
            .toMap()

    return chain(
        listOf(
            substituter(
                specializationSubstitutions(general, specific),
                eventLinkedSources + dependencyBindings.keys,
            )
        ) +
            dependencyBindings.map { (source, replacement) ->
              PetNode.replacer(source, replacement)
            } +
            afterSubstitution +
            invalidChangesToDie()
    )
  }

  /** Applies trigger narrowing only to the source expressions declared by linkages. */
  public fun checkedLinkageSubstituter(
      general: Type,
      specific: Type,
      linkedSources: Set<Expression>,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    val substitutions = specializationSubstitutions(general, specific)
    val broad = substituter(substitutions)
    val dependencyPaths = general.dependencies.flatten().keys
    val linkedReplacements = linkedSources.mapNotNull { source ->
      val broadReplacement = broad.transformExpression(source)
      val replacement =
          if (broadReplacement != source) {
            broadReplacement
          } else {
            dependencyPaths
                .filter { path ->
                  val dependency = general.dependencies.at(path)
                  dependency.expression == source || dependency.expressionFull == source
                }
                .map { specific.dependencies.at(it).expressionFull }
                .distinct()
                .singleOrNull() ?: source
          }
      if (replacement == source) null else PetNode.replacer(source, replacement)
    }
    return chain(linkedReplacements + afterSubstitution + invalidChangesToDie())
  }

  private fun specializationSubstitutions(
      general: Type,
      specific: Type,
  ): Map<ClassName, Expression> {
    val subs = findSubstitutions(general.dependencies, specific.dependencies).toMutableMap()
    if (general.rootClass.abstract && specific.rootClass != general.rootClass) {
      subs[general.className] = specific.className.expression
    }
    return subs
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

  internal fun findSubstitutions(
      gendeps: DependencySet,
      specdeps: DependencySet,
  ): Map<ClassName, Expression> {
    val commonKeys = gendeps.flatten().keys.intersect(specdeps.flatten().keys)
    return commonKeys
        .mapNotNull {
          val replaced = gendeps.at(it).expressionFull
          val replacement = specdeps.at(it).expressionFull
          when {
            classTable.getClass(replaced.className).abstract &&
                replaced.className != replacement.className ->
                replaced.className to replacement.className.expression
            replaced.simple && replacement != replaced -> replaced.className to replacement
            else -> null
          }
        }
        // A name can occur in independent slots; only agreement makes it one binding.
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (name, replacements) ->
          replacements.distinct().singleOrNull()?.let { name to it }
        }
        .toMap()
  }
}
