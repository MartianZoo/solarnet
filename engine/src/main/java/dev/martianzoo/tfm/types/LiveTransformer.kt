package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.overlayMaps

/** Offers various functions for transforming [PetNode] subtrees that depend on a [PClassLoader]. */
public class LiveTransformer internal constructor(val loader: PClassLoader) {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Player2, Class<Steel>>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use [AstTransforms.deprodify].
   */
  public fun <P : PetNode> deprodify(node: P): P =
      AstTransforms.deprodify(node, subclassNames(STANDARD_RESOURCE))

  public fun fixEffectForUnownedContext(fx: Effect) =
      if (OWNER in fx.instruction && OWNER !in fx.trigger) {
        fx.copy(trigger = ByTrigger(fx.trigger, OWNER))
      } else {
        fx
      }

  private fun subclassNames(parent: ClassName): Set<ClassName> =
      loader.getClass(parent).allSubclasses.classNames()

  /**
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Owner, LandArea(HAS? Neighbor<OwnedTile<Owner>>)>`. (Search `DEFAULT` in any
   * `*.pets` files for other examples.)
   */
  public fun <P : PetNode> applyGainRemoveDefaults(
      node: P,
      contextComponent: TypeExpr = THIS.type,
  ): P {
    return GainRemoveDefaultApplier(loader, contextComponent).transform(node)
  }
  public fun <P : PetNode> applyAllCasesDefaults(
      node: P,
      contextComponent: TypeExpr = THIS.type,
  ): P {
    return AllCasesDefaultApplier(loader, contextComponent).transform(node)
  }

  private inner class GainRemoveDefaultApplier(val loader: PClassLoader, val context: TypeExpr) :
      PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      val result: PetNode =
          when (node) {
            is Gain -> {
              val original = node.scaledType.typeExpr
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = loader.getClass(original.className).defaults
                val fixed = insertDefaults(original, defaults.gainOnlyDependencies, context)
                val scaledType = ScaledTypeExpr(node.count, fixed)
                Gain(scaledType, node.intensity ?: defaults.gainIntensity)
              }
            }
            is Remove -> { // TODO duplication
              val original = node.scaledType.typeExpr
              if (leaveItAlone(original)) {
                return node // don't descend
              } else {
                val defaults: Defaults = loader.getClass(original.className).defaults
                val fixed = insertDefaults(original, defaults.removeOnlyDependencies, context)
                val scaledType = ScaledTypeExpr(node.count, fixed)
                Remove(scaledType, node.intensity ?: defaults.removeIntensity)
              }
            }
            else -> transformChildren(node)
          }
      @Suppress("UNCHECKED_CAST") return result as P
    }

    private fun leaveItAlone(unfixed: TypeExpr) = unfixed.className in setOf(THIS, CLASS)
  }

  private inner class AllCasesDefaultApplier(val loader: PClassLoader, val context: TypeExpr) :
      PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      if (node !is TypeExpr) return transformChildren(node)
      if (leaveItAlone(node)) return node

      val defaults: Defaults = loader.getClass(node.className).defaults
      val result = insertDefaults(transformChildren(node), defaults.allCasesDependencies, context)

      @Suppress("UNCHECKED_CAST") return result as P
    }

    private fun leaveItAlone(unfixed: TypeExpr) = unfixed.className in setOf(THIS, CLASS)
  }

  // only has to modify the args/specs
  internal fun insertDefaults(
      original: TypeExpr, // SpecialTile<Player2>
      defaultDeps: DependencyMap, // Tile_0=LandArea
      contextCpt: TypeExpr = THIS.type,
  ): TypeExpr {

    val pclass: PClass = loader.getClass(original.className) // SpecialTile
    val dethissed: TypeExpr = original.replaceAll(THIS.type, contextCpt)
    val preferred: Map<Key, TypeExpr> =
        pclass.match(dethissed.arguments).map { it.key }.zip(original.arguments).toMap()

    val fallbacks: Map<Key, TypeExpr> = defaultDeps.map.mapValues { (_, v) -> v.typeExpr }
    val overlaid: Map<Key, TypeExpr> = overlayMaps(preferred, fallbacks)

    // reorder them
    val newArgs: List<TypeExpr> =
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
