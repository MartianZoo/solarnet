package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.ATOMIZED
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression
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
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.engine.Prod
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Defaults
import dev.martianzoo.types.Defaults.DefaultSpec
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet
import dev.martianzoo.types.Type

public class Transformers(public val classTable: ClassTable) {

  private val effectsByClass = mutableMapOf<Class, List<Effect>>()
  private val resourceClassNames by lazy { Prod.findResourceClassNames(classTable) }

  /** Rewrites session-localized input names to their canonical engine names. */
  public fun canonicalize(vocabulary: Vocabulary): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            if (node is ClassName) vocabulary.canonicalName(node) else transformChildren(node)
      }

  /** Effects inherited by [klass], processed as far as possible without a concrete component. */
  internal fun classEffects(klass: Class): List<Effect> {
    require(klass.classTable === classTable) { "$klass belongs to a different class table" }
    return effectsByClass.getOrPut(klass) {
      fun directClassEffects(source: Class) =
          source.declaration.effects.map(attachToClassTransformer(source)::transformEffect)

      klass.allSuperclasses().flatMap(::directClassEffects)
    }
  }

  private fun attachToClassTransformer(klass: Class): PetTransformer {
    val context = klass.className.has(Min(scaledEx(OK, 1)))
    return chain(
        insertDefaults(context),
        atomizer(),
        Prod.deprodify(resourceClassNames),
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
  internal fun atomizer(): PetTransformer {
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

  internal fun insertDefaults() = insertDefaults(THIS.expression)

  internal fun insertDefaults(context: Expression) =
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
          val spec: DefaultSpec = extractor(classTable.getClass(original.className).defaults)
          val fixed =
              insertDefaultsIntoExpr(
                  original,
                  spec.dependencies,
                  context,
                  classTable,
              )
          val intensity = node.intensity ?: spec.intensity
          rebuild(fixed, intensity)
        }
      }

      private fun handleTransmute(node: Transmute): Transmute {
        val gainDefault = defaultFor(node.gaining) { it.gainOnly }
        val removeDefault = defaultFor(node.removing) { it.removeOnly }
        val intensity =
            node.intensity ?: intersectIntensities(gainDefault?.intensity, removeDefault?.intensity)

        return Transmute(
            FromExpression(
                applyDefault(node.gaining, gainDefault, context),
                applyDefault(node.removing, removeDefault, context),
            ),
            node.count,
            intensity,
        )
      }

      private fun defaultFor(
          expression: Expression,
          extractor: (Defaults) -> DefaultSpec,
      ): DefaultSpec? =
          if (leaveItAlone(expression)) null
          else extractor(classTable.getClass(expression.className).defaults)

      private fun applyDefault(
          expression: Expression,
          default: DefaultSpec?,
          context: Expression,
      ): Expression =
          if (default == null) expression
          else insertDefaultsIntoExpr(expression, default.dependencies, context, classTable)

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

    return original.copy(arguments = newArgs).also {
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
          val replacement: Expression? = subs[node.className]
          if (replacement != null) {
            val expr: Expression =
                replacement
                    .appendArguments(node.arguments)
                    .copy(refinement = node.refinement, complement = node.complement)
            return expr
          }
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
  internal fun checkedSubstituter(
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

  /** Specializes a component while leaving trigger-local linked expressions to the event match. */
  internal fun checkedSubstituterPreserving(
      general: Type,
      specific: Type,
      preserved: Set<Expression>,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    return chain(
        listOf(substituter(specializationSubstitutions(general, specific), preserved)) +
            afterSubstitution +
            invalidChangesToDie()
    )
  }

  /** Applies trigger narrowing only to the source expressions declared by linkages. */
  internal fun checkedLinkageSubstituter(
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
          if (types.any(Type::phantom)) {
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
          val replaced = gendeps.at(it).expression
          val replacement = specdeps.at(it).expression
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
