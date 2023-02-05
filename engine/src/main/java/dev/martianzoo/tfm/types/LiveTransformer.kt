package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.toSetStrict

/** Offers various functions for transforming [PetNode] subtrees that depend on a [PClassLoader]. */
public class LiveTransformer internal constructor(val loader: PClassLoader) {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Class<Steel>, Player2>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use (TODO) directly if these class names
   * are already known.
   */
  public fun <P : PetNode> deprodify(node: P): P =
      AstTransforms.deprodify(node, subclassNames(STANDARD_RESOURCE))

  public fun <P : PetNode> addOwner(node: P): P =
      AstTransforms.addOwner(node, subclassNames(ANYONE), subclassNames(OWNED))

  private fun subclassNames(parent: ClassName) =
      loader.getClass(parent).allSubclasses.map { it.name }.toSetStrict()

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
      val writtenType = node.sat.typeExpr
      val defaults = loader.getClass(writtenType.className).defaults
      val fixedType =
          if (writtenType.isTypeOnly) {
            val deps: Collection<Dependency> = defaults.gainOnlyDependencies.types
            writtenType.addArgs(deps.map { it.toTypeExprMinimal() }).refine(writtenType.refinement)
          } else {
            writtenType
          }
      val transformed =
          Gain(sat(node.sat.scalar, x(fixedType)), node.intensity ?: defaults.gainIntensity)
      @Suppress("UNCHECKED_CAST") return transformed as P
    }
  }

  internal fun <P : PetNode> unreplaceThis(node: P, context: PClass) =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P =
            if (node is TypeExpr && context.isBaseType(node)) {
              @Suppress("UNCHECKED_CAST")
              THIS.type as P
            } else {
              defaultTransform(node)
            }
      }.transform(node)
}
