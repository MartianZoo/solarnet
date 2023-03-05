package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.overlayMaps

/**
 * Offers various functions, for transforming [PetNode] subtrees, that depend on a [PClassLoader].
 */
public class Transformer internal constructor(val loader: PClassLoader) {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Player2, Class<Steel>>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use [AstTransforms.deprodify].
   */
  public fun <P : PetNode> deprodify(node: P): P =
      AstTransforms.deprodify(node, subclassNames(STANDARD_RESOURCE))

  /** Sanitizes [fx] so that it can be attached to a context object that is not Owned. */
  public fun fixEffectForUnownedContext(fx: Effect, pc: PClass): Effect =
      if (OWNED in pc.allSuperclasses.classNames() ||
          OWNER !in fx.instruction ||
          OWNER in fx.trigger) {
        fx
      } else {
        fx.copy(trigger = ByTrigger(fx.trigger, OWNER))
      }

  private fun subclassNames(parent: ClassName): Set<ClassName> =
      loader.getClass(parent).allSubclasses.classNames()

  /**
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Owner, LandArea(HAS? Neighbor<OwnedTile<Owner>>)>`. (Search `DEFAULT` in any
   * `*.pets` files for other examples.)
   */
  public fun <P : PetNode> insertDefaults(node: P, contextComponent: Expression = THIS.expr): P {
    val step = GainRemoveDefaultApplier(contextComponent).transform(node)
    return AllCasesDefaultApplier(contextComponent).transform(step)
  }

  private inner class GainRemoveDefaultApplier(val context: Expression) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      val result: PetNode =
          when (node) {
            is Gain -> {
              val original: Expression = node.scaledEx.expression
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = loader.allDefaults[original.className]!!
                val fixed = insertDefaultsIntoExpr(original, defaults.gainOnlyDependencies, context)
                val scaledEx = ScaledExpression(node.count, fixed)
                Gain(scaledEx, node.intensity ?: defaults.gainIntensity)
              }
            }
            is Remove -> { // TODO duplication
              val original: Expression = node.scaledEx.expression
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = loader.allDefaults[original.className]!!
                val fixed =
                    insertDefaultsIntoExpr(original, defaults.removeOnlyDependencies, context)
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

  private inner class AllCasesDefaultApplier(val context: Expression) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      if (node !is Expression) return transformChildren(node)
      if (leaveItAlone(node)) return node

      val defaults: Defaults = loader.allDefaults[node.className]!!
      val result =
          insertDefaultsIntoExpr(transformChildren(node), defaults.allCasesDependencies, context)

      @Suppress("UNCHECKED_CAST") return result as P
    }

    private fun leaveItAlone(unfixed: Expression) = unfixed.className in setOf(THIS, CLASS)
  }

  // only has to modify the args/specs
  internal fun insertDefaultsIntoExpr(
      original: Expression,
      defaultDeps: DependencySet,
      contextCpt: Expression = THIS.expr,
  ): Expression {

    val pclass: PClass = loader.getClass(original.className)
    val dethissed: Expression = original.replaceAll(THIS.expr, contextCpt)
    val preferred = pclass.loader.match(dethissed.arguments,
        pclass.baseType.dependencies).keys.zip(original.arguments).toMap()
    val back = defaultDeps.asSet.associate { it.key to it.expression }
    val overlaid: Map<Key, Expression> = overlayMaps(preferred, back)

    // reorder them
    val newArgs: List<Expression> =
        pclass.allDependencyKeys
            .mapNotNull { overlaid[it] }
            .also { require(it.size == overlaid.size) }

    return original.copy(arguments = newArgs).also {
      require(it.className == original.className)
      require(it.refinement == original.refinement)
      require(it.arguments.containsAll(original.arguments))
    }
  }
}
