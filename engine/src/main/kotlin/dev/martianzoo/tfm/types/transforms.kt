package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
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
   * Translates a Pets node in source form to one where defaults have been applied; for example, an
   * instruction to gain a `GreeneryTile` with no type arguments listed would be converted to
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
            is TypeExpr -> addDefaultArgs(node).refine(x(node.refinement))
            else -> defaultTransform(node)
          }
      @Suppress("UNCHECKED_CAST") return transformed as P
    }

    // This is one of the gnarliest functions in the whole codebase right now...
    private fun addDefaultArgs(writtenTypeExpr: TypeExpr): TypeExpr { // e.g. CityTile<VolcanicArea>
      val writtenClassName = writtenTypeExpr.className // CityTile
      if (writtenClassName == CLASS) return writtenTypeExpr

      val writtenClass = loader.getClass(writtenClassName)
      val allCasesDeps = writtenClass.defaults.allCasesDependencies // Owned_0=Me
      if (allCasesDeps.isEmpty()) return writtenTypeExpr

      /*
       * As we have it now, we don't know what kind of class or type these nodes
       * ultimately belong to, so we have to just remove THIS & ignore it
       */
      val writtenArgs: List<TypeExpr> = writtenTypeExpr.arguments - THIS.type // [VA]

      val writtenArgTypes: List<PType> = writtenArgs.map(loader::resolveType) // [VA]
      val writtenDeps = writtenClass.findMatchups(writtenArgTypes) // Tile_0=VA

      val writtenPlusDefaults = writtenDeps.overlayOn(allCasesDeps) // {Tile_0 to VA, Owned_0 to Me}
      val newArgs = writtenPlusDefaults.argsAsTypeExprs() // [VA, Me]

      val transformedNewArgs = newArgs.map { x(it) } // [VA, Me]
      return writtenClassName.addArgs(transformedNewArgs) // CityTile<VolcanicArea, Me>
    }
  }
}
