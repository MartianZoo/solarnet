package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

public fun <P : PetNode> applyDefaultsIn(node: P, loader: PetClassLoader): P {
  return Defaulter(loader).transform(node)
}

private class Defaulter(val table: PetClassTable) : PetNodeVisitor() {
  override fun <P : PetNode?> transform(node: P): P {
    val transformed: PetNode? =
        when (node) {
          is Gain -> {
            // this should be the real source form because we should run first
            val writtenType = node.sat.type.asGeneric()
            val defaults = table.get(writtenType.root).defaults
            val fixedType =
                if (writtenType.isTypeOnly) {
                  val deps = defaults.gainOnlyDependencies.types
                  writtenType.addArgs(deps.map { it.type.toTypeExpression() })
                } else {
                  writtenType
                }
            Gain(
                sat(node.sat.scalar, transform(fixedType)),
                node.intensity ?: defaults.gainIntensity)
          }
          null,
          THIS.type,
          ME.type -> node
          is GenericTypeExpression -> {
            val petClass = table.get(node.root)
            val allCasesDependencies = petClass.defaults.allCasesDependencies
            if (allCasesDependencies.isEmpty()) {
              node
            } else {
              // TODO have to reengineer what resolve would do because the pettype has forgotten
              val explicitDeps = petClass.baseType.dependencies
              val foo = explicitDeps.findMatchups(node.args.map { table.resolve(it) })
              val newArgs =
                  foo.overlayOn(allCasesDependencies).types.map {
                    it.toTypeExpressionFull() // TODO not full
                  }
              node.replaceArgs(newArgs.map { transform(it) })
            }
          }
          else -> super.transform(node)
        }
    @Suppress("UNCHECKED_CAST") return transformed as P
  }
}
