package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNER
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.deprodify

/**
 * Various functions for transforming [PetNode] subtrees that require some `engine`-module type or
 * other (otherwise they should be moved to the `pets` module).
 */
public object AstTransforms {
  /**
   * Resolves `PROD[...]` regions by replacing, for example, `Steel<Player2>` with
   * `Production<Class<Steel>, Player2>`. This form of the function uses [loader] to look up the
   * names of the standard resource classes; one could also use (TODO) directly if these class names
   * are already known.
   */
  public fun <P : PetNode> deprodify(node: P, loader: PClassLoader): P {
    val stdRes = loader.getClass(SpecialClassNames.STANDARD_RESOURCE)
    val producible = loader.allClasses.filter(stdRes::isSuperclassOf).map(PClass::name).toSet()
    return deprodify(node, producible)
  }

  /**
   * For any type expression whose root type is in [ownedClassNames] but does not already have
   * either `Owner` or `Anyone` as a type argument, adds `Owner` as a type argument. This is
   * implementing what the code `class Owned { DEFAULT This<Owner> ... }` is already trying to
   * express, but I haven't gotten that working in a general way yet.
   *
   * TODO move this to `pets` module
   */
  public fun <P : PetNode> addOwnerToOwned(node: P, ownedClassNames: Set<ClassName>): P {
    val ok = setOf(OWNER.type, ANYONE.type)
    val pwner =
        object : PetTransformer() {
          override fun <Q : PetNode> doTransform(node: Q): Q {
            return if (node !is TypeExpr) {
              defaultTransform(node)
            } else if (node.className == CLASS) {
              node // don't descend; it's perfect how it is
            } else if (node.className in ownedClassNames && node.arguments.intersect(ok).none()) {
              defaultTransform(node).addArgs(OWNER.type) as Q
            } else {
              defaultTransform(node)
            }
          }
        }
    return node.transform(pwner)
  }

  /**
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Owner, LandArea(HAS? Neighbor<OwnedTile<Me>>)>`. (Search `DEFAULT` in any
   * `*.pets` files for other examples.)
   */
  public fun <P : PetNode> applyGainDefaultsIn(node: P, loader: PClassLoader) =
      node.transform(Defaulter(loader))

  private class Defaulter(val loader: PClassLoader) : PetTransformer() {
    override fun <P : PetNode> doTransform(node: P): P {
      val transformed: PetNode =
          when (node) {
            is Gain -> {
              // this should be the real source form because we should run first
              val writtenType = node.sat.typeExpr
              val defaults = loader.getClass(writtenType.className).defaults
              val fixedType =
                  if (writtenType.isTypeOnly) {
                    val deps: Collection<Dependency> = defaults.gainOnlyDependencies.types
                    writtenType
                        .addArgs(deps.map { it.toTypeExprFull() })
                        .refine(writtenType.refinement)
                  } else {
                    writtenType
                  }
              Gain(sat(node.sat.scalar, x(fixedType)), node.intensity ?: defaults.gainIntensity)
            }
            else -> defaultTransform(node)
          }
      @Suppress("UNCHECKED_CAST") return transformed as P
    }
  }

  internal fun <P : PetNode> unreplaceThis(node: P, owning: PClass) =
      node.transform(
          object : PetTransformer() {
            override fun <P : PetNode> doTransform(node: P): P =
                if (node is TypeExpr && owning.isBaseType(node)) {
                  @Suppress("UNCHECKED_CAST")
                  THIS.type as P
                } else {
                  defaultTransform(node)
                }
          })
}
