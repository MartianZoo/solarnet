package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.deprodify

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
   * Translates a Pets node in source form to one where defaults have been applied; for example,
   * an instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
   * `GreeneryTile<Me, LandArea(HAS? Neighbor<OwnedTile<Me>>)>`. (Search `DEFAULT` in any `*.pets`
   * files for other examples.)
   */
  public fun <P : PetNode> applyDefaultsIn(node: P, loader: PClassLoader) =
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
                    writtenType.addArgs(deps.map { it.toTypeExprFull() })
                  } else {
                    writtenType
                  }
              Gain(sat(node.sat.scalar, x(fixedType)), node.intensity ?: defaults.gainIntensity)
            }
            THIS.type -> node
            ME.type -> node
            is TypeExpr -> {
              val pclass = loader.getClass(node.className)
              val allCasesDependencies = pclass.defaults.allCasesDependencies
              if (allCasesDependencies.isEmpty() || pclass == loader.classClass) {
                node
              } else {
                // TODO have to reengineer what resolve would do because the ptype has forgotten
                val explicitDeps = pclass.baseType.dependencies
                val foo = explicitDeps.findMatchups(node.arguments.map { loader.resolveType(it) })
                val newArgs =
                    foo.overlayOn(allCasesDependencies).types.map {
                      it.toTypeExprFull() // TODO not full?
                    }
                node.replaceArgs(newArgs.map(::x)) // recurse on refinement TODO
              }
            }

            else -> defaultTransform(node)
          }
      @Suppress("UNCHECKED_CAST") return transformed as P
    }
  }
}
