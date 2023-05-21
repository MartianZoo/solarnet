package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.api.SpecialClassNames.ATOMIZED
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.PROD
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.PetTransformer.Companion.noOp
import dev.martianzoo.tfm.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.tfm.types.Defaults.DefaultSpec
import dev.martianzoo.tfm.types.Dependency.Key

public class Transformers(private val table: MClassTable) {

  internal val requiredClasses: Set<ClassName> = setOf(PRODUCTION)

  public fun standardPreprocess() = chain(useFullNames(), atomizer(), insertDefaults(), deprodify())

  public fun useFullNames() =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            table.resolve(node.expression).className as P
          } else {
            transformChildren(node)
          }
        }
      }

  public fun deprodify(): PetTransformer {
    if (STANDARD_RESOURCE !in table.allClassNamesAndIds ||
        PRODUCTION !in table.allClassNamesAndIds) {
      return noOp()
    }
    val classNames =
        table.getClass(STANDARD_RESOURCE).allSubclasses.flatMap {
          setOf(it.className, it.shortName)
        }

    var inProd = false

    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        val rewritten: PetNode =
            when {
              node is Multi -> {
                val badIndex =
                    node.instructions.indexOfFirst {
                      it is Transform && it.transformKind == PROD && it.instruction is Multi
                    }
                val xed = transformChildren(node)
                if (badIndex == -1) {
                  xed
                } else {
                  Multi.create(
                      xed.instructions.subList(0, badIndex) +
                          (xed.instructions[badIndex] as Multi).instructions +
                          xed.instructions.subList(badIndex + 1, xed.instructions.size),
                  )
                }
              }
              node is TransformNode<*> && node.transformKind == PROD -> {
                require(!inProd)
                inProd = true
                val inner = transform(node.extract())
                inProd = false
                if (inner == node.extract()) {
                  throw PetSyntaxException("No standard resources found in PROD box: $inner")
                }
                inner
              }
              inProd && node is Expression && node.className in classNames ->
                  PRODUCTION.of(node.arguments + node.className.classExpression())
              else -> transformChildren(node)
            }
        @Suppress("UNCHECKED_CAST") return rewritten as P
      }
    }
  }

  public fun atomizer(): PetTransformer {
    val atomized =
        try {
          table.getClass(ATOMIZED)
        } catch (e: Exception) {
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
          @Suppress("UNCHECKED_CAST") return Multi(flattened) as P
        }
        if (node !is Gain) return transformChildren(node)
        val scex = node.scaledEx
        val sc = scex.scalar

        if (sc !is ActualScalar ||
            sc.value == 1 ||
            THIS in scex.expression ||
            !table.resolve(scex.expression).root.isSubtypeOf(atomized)) {
          return node
        }

        val one = node.copy(scaledEx = scex.copy(scalar = ActualScalar(1)))
        ourMulti = Multi((1..sc.value).map { one })

        @Suppress("UNCHECKED_CAST")
        return ourMulti as P // TODO This is not actually correct/safe...
      }
    }
  }

  public fun insertDefaults() = insertDefaults(THIS.expression)

  public fun insertDefaults(context: Expression) =
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
        @Suppress("UNCHECKED_CAST") return result as P
      }

      private fun <P : Change> handleIt(
          node: P,
          original: Expression,
          extractor: (Defaults) -> DefaultSpec,
      ): P {
        return if (leaveItAlone(original)) {
          node // don't descend
        } else {
          val spec: DefaultSpec = extractor(table.defaults(original.className))
          val fixed =
              insertDefaultsIntoExpr(
                  original,
                  spec.dependencies,
                  context,
                  table,
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
                        SimpleFrom(fixed, node.removing)
                      }
                  Transmute(fixedFrom, node.count, intensity)
                }
                else -> error("") // TODO why
              }

          @Suppress("UNCHECKED_CAST")
          result as P
        }
      }
    }
  }

  private fun insertExpressionDefaults(context: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node !is Expression) return transformChildren(node)
        if (leaveItAlone(node)) return node

        val defaultDeps = table.defaults(node.className).allUsages.dependencies
        val result = insertDefaultsIntoExpr(transformChildren(node), defaultDeps, context, table)
        @Suppress("UNCHECKED_CAST") return result as P
      }
    }
  }

  private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)

  // only has to modify the args/specs
  private fun insertDefaultsIntoExpr(
      original: Expression,
      defaultDeps: DependencySet,
      contextCpt: Expression = THIS.expression,
      table: MClassTable,
  ): Expression {

    val mclass: MClass = table.getClass(original.className)
    val dethissed: Expression = replaceThisExpressionsWith(contextCpt).transform(original)
    val match: DependencySet = mclass.dependencies.matchPartial(dethissed.arguments)

    val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
    val fallbacks: Map<Key, Expression> =
        defaultDeps.typeDependencies.associate { it.key to it.expression }

    val newArgs: List<Expression> =
        mclass.dependencies.keys.mapNotNull { preferred[it] ?: fallbacks[it] }

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

    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node is Expression) {
          val replacement: Expression? = subs[node.className]
          if (replacement != null) {
            val expr: Expression = replacement.appendArguments(node.arguments)
            @Suppress("UNCHECKED_CAST") return expr as P
          }
        }
        return transformChildren(node)
      }
    }
  }

  // We check if MartianIndustries reifies CardFront(HAS 1 BuildingTag)
  // by testing the requirement `1 BuildingTag<MartianIndustries>`
  internal fun refinementMangler(proposed: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return if (node is Expression) {
          val modded = table.resolve(node).specialize(listOf(proposed))
          @Suppress("UNCHECKED_CAST")
          modded.expressionFull as P
        } else {
          transformChildren(node)
        }
      }
    }
  }

  internal fun findSubstitutions(
      gendeps: DependencySet,
      specdeps: DependencySet,
  ): Map<ClassName, Expression> {
    val commonKeys = gendeps.flattened.keys.intersect(specdeps.flattened.keys)
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
        .toMap()
  }

  // Only use this if the context object is unowned!
  fun fixEffectForUnownedContext(): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return if (node is Effect && OWNER in node.instruction && OWNER !in node.trigger) {
          // no need to recurse on the components
          val effect: Effect = node.copy(trigger = ByTrigger(node.trigger, OWNER))
          @Suppress("UNCHECKED_CAST")
          effect as P
        } else {
          transformChildren(node)
        }
      }
    }
  }

  private companion object {
    val PRODUCTION = cn("Production")
    val STANDARD_RESOURCE = cn("StandardResource")
  }
}
