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
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.engine.Prod
import dev.martianzoo.types.Class
import dev.martianzoo.types.Defaults
import dev.martianzoo.types.Defaults.DefaultSpec
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet
import dev.martianzoo.types.Type
import dev.martianzoo.types.TypeUniverse

public class Transformers(public val typeUniverse: TypeUniverse) {

  private val effectsByClass = mutableMapOf<Class, List<Effect>>()
  private val resourceClassNames by lazy { Prod.findResourceClassNames(typeUniverse) }

  /** Effects inherited by [klass], processed as far as possible without a concrete component. */
  internal fun classEffects(klass: Class): List<Effect> {
    require(klass.typeUniverse === typeUniverse) { "$klass belongs to a different type universe" }
    return effectsByClass.getOrPut(klass) {
      fun directClassEffects(source: Class) =
          source.declaration.effects.map(attachToClassTransformer(source)::transform)

      klass.allSuperclasses().flatMap(::directClassEffects)
    }
  }

  private fun attachToClassTransformer(klass: Class): PetTransformer {
    val context = klass.className.has(Min(scaledEx(1, OK)))
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
      override fun <P : PetNode> transform(node: P): P {
        return if (node is Effect && OWNER in node.instruction && OWNER !in node.trigger) {
          @Suppress("UNCHECKED_CAST")
          node.copy(trigger = ByTrigger(node.trigger, OWNER)) as P
        } else {
          transformChildren(node)
        }
      }
    }
  }

  public fun useFullNames(): PetTransformer =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is Count && typeUniverse.isUnresolvedClassLiteral(node.expression)) {
            node
          } else if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            typeUniverse.resolve(node.expression).className as P
          } else {
            transformChildren(node)
          }
        }
      }

  @Suppress("ComplexCondition") // TODO: fix that
  internal fun atomizer(): PetTransformer {
    val atomized = typeUniverse.findClass(ATOMIZED) ?: return noOp()

    return object : PetTransformer() {
      var ourMulti: Multi? = null

      override fun <P : PetNode> transform(node: P): P {
        if (node is Multi && ourMulti != null && (ourMulti as Multi) in node.instructions) {
          val flattened =
              node.instructions.flatMap {
                if (it == ourMulti) {
                  ourMulti!!.instructions
                } else {
                  listOf(it)
                }
              }
          @Suppress("UNCHECKED_CAST")
          return Multi.create(flattened) as P
        }
        if (node !is Gain) return transformChildren(node)
        val scex = node.scaledEx
        val sc = scex.scalar

        if (
            sc !is ActualScalar ||
                sc.value == 1 ||
                THIS in scex.expression ||
                !typeUniverse.resolve(scex.expression).rootClass.isSubtypeOf(atomized)
        ) {
          return node
        }

        val one = gain(scex.copy(scalar = ActualScalar(1)), node.intensity) as Gain
        ourMulti = Multi.create((1..sc.value).map { one }) as Multi

        @Suppress("UNCHECKED_CAST") // not technically safe...
        return ourMulti as P
      }
    }
  }

  internal fun insertDefaults() = insertDefaults(THIS.expression)

  internal fun insertDefaults(context: Expression) =
      chain(insertGainRemoveDefaults(context), insertExpressionDefaults(context))

  private fun insertGainRemoveDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        val result: PetNode =
            if (node is Change) {
              when (node) {
                is Gain -> handleIt(node, node.gaining) { it.gainOnly }
                is Remove -> handleIt(node, node.removing) { it.removeOnly }
                is Transmute -> handleIt(node, node.gaining) { it.gainOnly }
              }
            } else {
              transformChildren(node)
            }
        @Suppress("UNCHECKED_CAST")
        return result as P
      }

      private fun <P : Change> handleIt(
          node: P,
          original: Expression,
          extractor: (Defaults) -> DefaultSpec,
      ): P {
        return if (leaveItAlone(original)) {
          node // don't descend
        } else {
          val spec: DefaultSpec = extractor(typeUniverse.getClass(original.className).defaults)
          val fixed =
              insertDefaultsIntoExpr(
                  original,
                  spec.dependencies,
                  context,
                  typeUniverse,
              )
          val intensity = node.intensity ?: spec.intensity

          val result: Change =
              when (node) { // TODO it's weird that the shared method is doing this
                is Gain -> gain(scaledEx(node.count, fixed), intensity) as Gain
                is Remove -> Remove(scaledEx(node.count, fixed), intensity)
                is Transmute -> {
                  val fixedFrom =
                      if (node.gaining == fixed) {
                        node.fromEx // no change, so don't mess up the structure
                      } else {
                        FromExpression(fixed, node.removing)
                      }
                  Transmute(fixedFrom, node.count, intensity)
                }
              }

          @Suppress("UNCHECKED_CAST")
          result as P
        }
      }
    }
  }

  public fun insertExpressionDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node

        val defaultDeps = typeUniverse.getClass(node.className).defaults.allUsages.dependencies
        val result =
            insertDefaultsIntoExpr(transformChildren(node), defaultDeps, context, typeUniverse)
        @Suppress("UNCHECKED_CAST")
        return result as P
      }
    }
  }

  private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)

  // only has to modify the args/specs
  private fun insertDefaultsIntoExpr(
      original: Expression,
      defaultDeps: DependencySet,
      contextCpt: Expression,
      typeUniverse: TypeUniverse,
  ): Expression {

    val klass: Class = typeUniverse.getClass(original.className)
    val dethissed: Expression = replaceThisExpressionsWith(contextCpt).transform(original)
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

  private fun substituter(subs: Map<ClassName, Expression>): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node is Expression) {
          val replacement: Expression? = subs[node.className]
          if (replacement != null) {
            val expr: Expression =
                replacement
                    .appendArguments(node.arguments)
                    .copy(refinement = node.refinement, complement = node.complement)
            @Suppress("UNCHECKED_CAST")
            return expr as P
          }
        }
        return transformChildren(node)
      }
    }
  }

  /**
   * Specializes linked type names and turns any atomic change made invalid by that specialization
   * into `Die`. This lets enclosing choices discard an impossible specialized branch.
   */
  internal fun checkedSubstituter(
      general: Type,
      specific: Type,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    val subs = findSubstitutions(general.dependencies, specific.dependencies).toMutableMap()
    if (general.rootClass.abstract && specific.rootClass != general.rootClass) {
      subs[general.className] = specific.className.expression
    }
    return chain(listOf(substituter(subs)) + afterSubstitution + invalidChangesToDie())
  }

  private fun invalidChangesToDie(): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        val specialized = transformChildren(node)
        if (specialized !is Change) return specialized

        try {
          specialized.gaining?.let(typeUniverse::resolve)
          specialized.removing?.let(typeUniverse::resolve)
        } catch (_: ExpressionException) {
          @Suppress("UNCHECKED_CAST")
          return gain(scaledEx(expression = DIE.expression)) as P
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
          if (replaced.simple && replacement != replaced) {
            replaced.className to replacement
          } else {
            null
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
