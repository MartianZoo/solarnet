package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.types.Dependency.Companion.intersect
import dev.martianzoo.util.associateByStrict

internal data class Defaults(
    val allCasesDependencies: DependencyMap = DependencyMap(),
    val gainOnlyDependencies: DependencyMap = DependencyMap(),
    val gainIntensity: Intensity,
    val removeOnlyDependencies: DependencyMap = DependencyMap(),
    val removeIntensity: Intensity,
) {
  companion object {
    fun forClass(pclass: PClass): Defaults {
      fun <T> inheritDefault(
          extractor: (DefaultsDeclaration) -> T?,
          merger: (List<T>) -> T = { it.single() },
      ): T? {
        fun extractFromClass(c: PClass): T? = extractor(c.declaration.defaultsDeclaration)

        val haveDefault: List<PClass> =
            pclass.allSuperclasses.filter { extractFromClass(it) != null }

        // Anything that was overridden by *any* of our superclasses must be discarded
        val inheritFrom = haveDefault - haveDefault.flatMap { it.properSuperclasses }.toSet()

        val candidates: List<T> = inheritFrom.map { extractFromClass(it)!! }.distinct()
        return if (candidates.any()) merger(candidates) else null
      }

      fun gatherDefaultDeps(extractor: (DefaultsDeclaration) -> List<Expression>): DependencyMap {
        // excludes ones that don't specialize, is that okay? TODO
        fun toDependencyMap(specs: List<Expression>): DependencyMap =
            pclass.loader.resolve(pclass.className.addArgs(specs)).narrowedDependencies

        val depList: List<Dependency> =
            pclass.allDependencyKeys.mapNotNull { key ->
              inheritDefault(
                  { toDependencyMap(extractor(it)).getIfPresent(key) },
                  { a: List<Dependency> -> intersect(a)!! })
            }
        return DependencyMap(depList.associateByStrict { it.key })
      }

      return Defaults(
          allCasesDependencies = gatherDefaultDeps { it.universalSpecs },
          gainOnlyDependencies = gatherDefaultDeps { it.gainOnlySpecs },
          removeOnlyDependencies = gatherDefaultDeps { it.removeOnlySpecs },
          gainIntensity = inheritDefault({ it.gainIntensity })!!,
          removeIntensity = inheritDefault({ it.removeIntensity })!!,
      )
    }
  }
}
