package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType

/** Offers various functions for transforming [PetNode] subtrees that depend on a [PClassLoader]. */
public class LiveTransformer internal constructor(val loader: PClassLoader) {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Player2, Class<Steel>>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use [AstTransforms.deprodify].
   */
  public fun <P : PetNode> deprodify(node: P): P =
      AstTransforms.deprodify(node, subclassNames(STANDARD_RESOURCE))

  public fun <P : PetNode> addOwner(node: P): P =
      AstTransforms.addOwner(node, subclassNames(ANYONE), subclassNames(OWNED))

  private fun subclassNames(parent: ClassName): Set<ClassName> =
      loader.getClass(parent).allSubclasses.classNames()

  /**
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Owner, LandArea(HAS? Neighbor<OwnedTile<Me>>)>`. (Search `DEFAULT` in any
   * `*.pets` files for other examples.)
   */
  public fun <P : PetNode> applyGainDefaultsIn(node: P) = Defaulter(loader).transform(node)

  private class Defaulter(val loader: PClassLoader) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      if (node !is Gain) return defaultTransform(node)
      val writtenType = node.scaledType.typeExpr
      val defaults = loader.getClass(writtenType.className).defaults
      val fixedType =
          if (writtenType.isTypeOnly) {
            val deps: Collection<Dependency> = defaults.gainOnlyDependencies.types
            writtenType.addArgs(deps.map { it.typeExpr }).refine(writtenType.refinement)
          } else {
            writtenType
          }
      val transformed =
          Gain(scaledType(node.scaledType.scalar, x(fixedType)), node.intensity ?: defaults.gainIntensity)
      @Suppress("UNCHECKED_CAST") return transformed as P
    }
  }
}
