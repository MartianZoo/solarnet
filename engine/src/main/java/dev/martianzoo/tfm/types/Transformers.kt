package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.GLOBAL_PARAMETER
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.engine.Exceptions.InvalidExpressionException
import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Transformers.ReplaceThisWith

public object Transformers {

  public open class CompositeTransformer(val transformers: List<PetTransformer>) :
      PetTransformer() {
    constructor(vararg transformers: PetTransformer) : this(transformers.toList())

    override fun <P : PetNode> transform(node: P): P { // TODO null passthru??
      var result = node
      for (xer in transformers) {
        result = xer.transform(result)
      }
      return result
    }
  }

  public class UseFullNames(val loader: MClassLoader) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is ClassName) {
        @Suppress("UNCHECKED_CAST")
        loader.getClass(node).className as P
      } else {
        node
      }
    }
  }

  public class UseShortNames(val loader: MClassLoader) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is ClassName) {
        @Suppress("UNCHECKED_CAST")
        loader.getClass(node).shortName as P
      } else {
        node
      }
    }
  }

  public class Deprodify(val loader: MClassLoader) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      val classNames =
          loader.getClass(STANDARD_RESOURCE).allSubclasses.flatMap {
            setOf(it.className, it.shortName)
          }
      return AstTransforms.deprodify(node, classNames)
    }
  }

  public class ReplaceThisWith(val contextType: Expression) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return node
          .replaceAll(THIS.classExpression(), contextType.className.classExpression())
          .replaceAll(THIS.expr, contextType)
    }
  }

  public class ReplaceOwnerWith(val owner: ClassName?) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (owner != null) {
        node.replaceAll(OWNER.expr, owner.expr)
      } else {
        node
      }
    }
  }

  public class FixEffectForUnownedContext(val context: MClass) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is Effect &&
          OWNED !in context.allSuperclasses.classNames() &&
          OWNER in node.instruction &&
          OWNER !in node.trigger) {
        val effect: Effect = node.copy(trigger = ByTrigger(node.trigger, OWNER))
        @Suppress("UNCHECKED_CAST")
        effect as P
      } else {
        node
      }
    }
  }

  public class AtomizeGlobalParameterGains(val loader: MClassLoader) : PetTransformer() {
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
        return Multi(flattened) as P
      }
      if (node !is Gain) return transformChildren(node)
      val scex = node.scaledEx
      if (scex.scalar == 1 || THIS in scex.expression) return node
      val type: MType = loader.resolve(scex.expression)
      if (!type.isSubtypeOf(loader.resolve(GLOBAL_PARAMETER.expr))) return node
      val one = node.copy(scaledEx = scex.copy(scalar = 1))
      ourMulti = Multi((1..scex.scalar).map { one })
      return ourMulti as P // TODO Uh oh
    }
  }

  public class InsertDefaults(loader: MClassLoader, context: Expression = THIS.expr) :
      CompositeTransformer(
          InsertGainRemoveDefaults(context, loader), InsertExpressionDefaults(context, loader))

  public class InsertGainRemoveDefaults(val context: Expression, val loader: MClassLoader) :
      PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      val result: PetNode =
          when (node) {
            is Transmute -> {
              val original: Expression = node.gaining
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = Defaults.forClass(loader.getClass(original.className))
                Transmute(node.from, node.scalar, node.intensity ?: defaults.gainIntensity)
                // TODO also gainDeps??
              }
            }
            is Gain -> {
              val original: Expression = node.gaining
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = Defaults.forClass(loader.getClass(original.className))
                val fixed =
                    insertDefaultsIntoExpr(original, defaults.gainOnlyDependencies, context, loader)
                val scaledEx = ScaledExpression(node.count, fixed)
                Gain(scaledEx, node.intensity ?: defaults.gainIntensity)
              }
            }
            is Remove -> { // TODO duplication
              val original: Expression = node.removing
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = loader.allDefaults[original.className]!!
                val fixed =
                    insertDefaultsIntoExpr(
                        original, defaults.removeOnlyDependencies, context, loader)
                val scaledEx = ScaledExpression(node.count, fixed)
                Remove(scaledEx, node.intensity ?: defaults.removeIntensity)
              }
            }
            else -> transformChildren(node)
          }
      @Suppress("UNCHECKED_CAST") return result as P
    }

    private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)
  }

  public class InsertExpressionDefaults(val context: Expression, val loader: MClassLoader) :
      PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      if (node !is Expression) return transformChildren(node)
      if (leaveItAlone(node)) return node

      val defaults: Defaults =
          loader.allDefaults[node.className]
              ?: throw InvalidExpressionException("${node.className}")
      val result =
          insertDefaultsIntoExpr(
              transformChildren(node), defaults.allCasesDependencies, context, loader)

      @Suppress("UNCHECKED_CAST") return result as P
    }

    private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)
  }
}

// only has to modify the args/specs
internal fun insertDefaultsIntoExpr(
    original: Expression,
    defaultDeps: DependencySet,
    contextCpt: Expression = THIS.expr,
    loader: MClassLoader
): Expression {

  val mclass: MClass = loader.getClass(original.className)
  val dethissed: Expression = ReplaceThisWith(contextCpt).transform(original)
  val match: DependencySet = loader.matchPartial(dethissed.arguments, mclass.dependencies)

  val preferred: Map<Key, Expression> = match.keys.zip(original.arguments).toMap()
  val fallbacks: Map<Key, Expression> = defaultDeps.asSet.associate { it.key to it.expression }

  val newArgs: List<Expression> =
      mclass.dependencies.keys.mapNotNull { preferred[it] ?: fallbacks[it] }

  return original.copy(arguments = newArgs).also {
    require(it.className == original.className)
    require(it.refinement == original.refinement)
    require(it.arguments.containsAll(original.arguments))
  }
}
