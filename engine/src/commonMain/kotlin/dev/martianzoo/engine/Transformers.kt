package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.ATOMIZED
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
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
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.types.Defaults
import dev.martianzoo.types.Defaults.DefaultSpec
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType

internal class Transformers(internal val classes: MClassTable) {

  internal fun useFullNames() =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is Count && classes.isUnresolvedClassLiteral(node.expression)) {
            node
          } else if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            classes.resolve(node.expression).className as P
          } else {
            transformChildren(node)
          }
        }
      }

  @Suppress("ComplexCondition") // TODO: fix that
  internal fun atomizer(): PetTransformer {
    val atomized =
        try {
          classes.getClass(ATOMIZED)
        } catch (_: ExpressionException) {
          return noOp()
        }

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
          return Multi(flattened) as P
        }
        if (node !is Gain) return transformChildren(node)
        val scex = node.scaledEx
        val sc = scex.scalar

        if (
            sc !is ActualScalar ||
                sc.value == 1 ||
                THIS in scex.expression ||
                !classes.resolve(scex.expression).root.isSubtypeOf(atomized)
        ) {
          return node
        }

        val one = gain(scex.copy(scalar = ActualScalar(1)), node.intensity) as Gain
        ourMulti = Multi((1..sc.value).map { one })

        @Suppress("UNCHECKED_CAST") // not technically safe...
        return ourMulti as P
      }
    }
  }

  internal fun insertDefaults() = insertDefaults(THIS.expression)

  internal fun insertDefaults(context: Expression) =
      chain(insertGainRemoveDefaults(context), insertExpressionDefaults(context))

  internal fun insertGainRemoveDefaults(context: Expression): PetTransformer {
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
          val spec: DefaultSpec = extractor(classes.getClass(original.className).defaults)
          val fixed =
              insertDefaultsIntoExpr(
                  original,
                  spec.dependencies,
                  context,
                  classes,
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

  internal fun insertExpressionDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node

        val defaultDeps = classes.getClass(node.className).defaults.allUsages.dependencies
        val result = insertDefaultsIntoExpr(transformChildren(node), defaultDeps, context, classes)
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
      table: MClassTable,
  ): Expression {

    val mclass: MClass = table.getClass(original.className)
    val dethissed: Expression = replaceThisExpressionsWith(contextCpt).transform(original)
    val match: DependencySet = mclass.dependencies.matchPartial(dethissed.arguments)

    val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
    val fallbacks: Map<Key, Expression> =
        defaultDeps.typeDependencies().associate { it.key to it.expression }
    val inferred = mclass.specialize(dethissed.arguments).narrowedDependencies.keys - preferred.keys

    val newArgs: List<Expression> =
        mclass.dependencies.keys.mapNotNull {
          preferred[it] ?: fallbacks[it]?.takeUnless { _ -> it in inferred }
        }

    return original.copy(arguments = newArgs).also {
      require(it.className == original.className)
      require(it.refinement == original.refinement)
      require(it.arguments.containsAll(original.arguments))
    }
  }

  internal fun substituter(general: MType, specific: MType): PetTransformer {
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
      general: MType,
      specific: MType,
      vararg afterSubstitution: PetTransformer?,
  ): PetTransformer {
    val subs = findSubstitutions(general.dependencies, specific.dependencies).toMutableMap()
    if (general.root.abstract && specific.root != general.root) {
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
          specialized.gaining?.let(classes::resolve)
          specialized.removing?.let(classes::resolve)
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
