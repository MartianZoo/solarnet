package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.AstTransforms.insertDefaultPlayer
import dev.martianzoo.tfm.pets.AstTransforms.replaceTypes
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType
import dev.martianzoo.tfm.pets.ast.TypeExpr

/** Offers various functions for transforming [PetNode] subtrees that depend on a [PClassLoader]. */
public class LiveTransformer internal constructor(val loader: PClassLoader) {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Player2, Class<Steel>>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use [AstTransforms.deprodify].
   */
  public fun <P : PetNode> deprodify(node: P): P =
      AstTransforms.deprodify(node, subclassNames(STANDARD_RESOURCE))

  public fun <P : PetNode> insertDefaultPlayer(node: P): P =
      insertDefaultPlayer(node, subclassNames(ANYONE), subclassNames(OWNED))

  private fun subclassNames(parent: ClassName): Set<ClassName> =
      loader.getClass(parent).allSubclasses.classNames()

  public fun <P : PetNode> simplifyTypes(element: P, thiss: PType): P {
    var tx = element
    tx = replaceTypes(element, THIS.type, thiss.typeExpr)

    val simplifier =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P): P {
            return if (node is TypeExpr) {
              loader.resolveType(node).typeExpr as P
            } else {
              defaultTransform(node)
            }
          }
        }
    tx = simplifier.transform(tx)
    tx = replaceTypes(tx, thiss.typeExpr, THIS.type)
    return tx
  }

  /**
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Owner, LandArea(HAS? Neighbor<OwnedTile<Me>>)>`. (Search `DEFAULT` in any
   * `*.pets` files for other examples.)
   */
  public fun <P : PetNode> applyDefaultsIn(node: P) = Defaulter(loader).transform(node)

  private class Defaulter(val loader: PClassLoader) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      val xfd =
          when (node) {
            is Gain -> {
              val defaults = loader.getClass(node.gaining.className).defaults
              val fixedType = fixType(node.gaining, defaults.gainOnlyDependencies)
              Gain(scaledType(node.count, fixedType), node.intensity ?: defaults.gainIntensity)
            }
            is Remove -> {
              val rcn = node.removing.className
              if (rcn == THIS) {
                Remove(
                    scaledType(node.count, node.removing),
                    node.intensity ?: loader.componentClass.defaults.removeIntensity)
              } else {
                val defaults = loader.getClass(rcn).defaults
                val fixedType = fixType(node.removing, defaults.removeOnlyDependencies)
                Remove(
                    scaledType(node.count, fixedType), node.intensity ?: defaults.removeIntensity)
              }
            }
            else -> defaultTransform(node)
          }
      @Suppress("UNCHECKED_CAST") return xfd as P
    }

    private fun fixType(writtenType: TypeExpr, defaultSpecs: DependencyMap): TypeExpr {
      val fixedType =
          if (writtenType.isTypeOnly) {
            val deps: Collection<Dependency> = defaultSpecs.types
            writtenType.addArgs(deps.map { it.typeExpr }).refine(writtenType.refinement)
          } else {
            writtenType
          }
      return x(fixedType)
    }
  }
}
