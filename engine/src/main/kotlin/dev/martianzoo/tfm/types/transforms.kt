package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr

public fun <P : PetNode> applyDefaultsIn(node: P, loader: PClassLoader) =
    node.transform(Defaulter(loader))

private class Defaulter(val table: PClassTable) : PetTransformer() {
  override fun <P : PetNode> doTransform(node: P): P {
    val transformed: PetNode =
        when (node) {
          is Gain -> {
            // this should be the real source form because we should run first
            val writtenType = node.sat.typeExpr.asGeneric()
            val defaults = table[writtenType.root].defaults
            val fixedType =
                if (writtenType.isTypeOnly) {
                  val deps = defaults.gainOnlyDependencies.types
                  writtenType.addArgs(deps.map { it.ptype.toTypeExprFull() })
                } else {
                  writtenType
                }
            Gain(sat(node.sat.scalar, x(fixedType)), node.intensity ?: defaults.gainIntensity)
          }
          THIS.type -> node
          ME.type -> node
          is GenericTypeExpr -> {
            val pclass = table[node.root]
            val allCasesDependencies = pclass.defaults.allCasesDependencies
            if (allCasesDependencies.isEmpty()) {
              node
            } else {
              // TODO have to reengineer what resolve would do because the ptype has forgotten
              val explicitDeps = pclass.baseType.dependencies
              val foo = explicitDeps.findMatchups(node.args.map { table.resolve(it) })
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
