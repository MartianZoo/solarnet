package dev.martianzoo.engine

import dev.martianzoo.api.SystemClasses.ATOMIZED
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.PetTransformer.Companion.noOp
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
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
import javax.inject.Inject

@GameScoped
internal class Transformers @Inject constructor(val classes: MClassTable) {

  public fun useFullNames() =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            classes.resolve(node.expression).className as P
          } else {
            transformChildren(node)
          }
        }
      }

  public fun atomizer(): PetTransformer {
    val atomized =
        try {
          classes.getClass(ATOMIZED)
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
            !classes.resolve(scex.expression).root.isSubtypeOf(atomized)) {
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

        val defaultDeps = classes.getClass(node.className).defaults.allUsages.dependencies
        val result = insertDefaultsIntoExpr(transformChildren(node), defaultDeps, context, classes)
        @Suppress("UNCHECKED_CAST") return result as P
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
}
