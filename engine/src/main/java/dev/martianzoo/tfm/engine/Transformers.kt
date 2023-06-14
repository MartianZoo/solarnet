package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.ATOMIZED
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.tfm.data.TfmClasses.PROD
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.Defaults
import dev.martianzoo.types.Defaults.DefaultSpec
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType
import javax.inject.Inject

@GameScoped
internal class Transformers @Inject constructor(val table: MClassTable) {

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
        table.getClass(STANDARD_RESOURCE).getAllSubclasses().flatMap {
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

        @Suppress("UNCHECKED_CAST") // not technically safe...
        return ourMulti as P
      }
    }
  }

  public fun insertDefaults() = insertDefaults(THIS.expression)

  public fun insertDefaults(context: Expression) =
      chain(insertGainRemoveDefaults(context), insertExpressionDefaults(context))

  public fun insertGainRemoveDefaults(context: Expression): PetTransformer {
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
          val spec: DefaultSpec = extractor(table.getClass(original.className).defaults)
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
                else -> error("what?")
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

        val defaultDeps = table.getClass(node.className).defaults.allUsages.dependencies
        val result = insertDefaultsIntoExpr(transformChildren(node), defaultDeps, context, table)
        @Suppress("UNCHECKED_CAST") return result as P
      }
    }
  }

  // Excluding THIS because we won't find it as an actual class... TODO is this the right thing?
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
        defaultDeps.typeDependencies().associate { it.key to it.expression }

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
}
