package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.util.Hierarchical.Companion.glb

internal data class Defaults(
    val allCasesDependencies: DependencySet = DependencySet.of(),
    val gainOnlyDependencies: DependencySet = DependencySet.of(),
    val gainIntensity: Intensity,
    val removeOnlyDependencies: DependencySet = DependencySet.of(),
    val removeIntensity: Intensity,
) {
  companion object {
    fun forClass(mclass: MClass): Defaults {
      fun <T> inheritDefault(
          extractor: (DefaultsDeclaration) -> T?,
          merger: (List<T>) -> T = { it.single() },
      ): T? {
        fun extractFromClass(c: MClass): T? = extractor(c.declaration.defaultsDeclaration)

        val haveDefault: List<MClass> =
            mclass.allSuperclasses.filter { extractFromClass(it) != null }

        // Anything that was overridden by *any* of our superclasses must be discarded
        val inheritFrom = haveDefault - haveDefault.flatMap { it.properSuperclasses }.toSet()

        val candidates: List<T> = inheritFrom.map { extractFromClass(it)!! }.distinct()
        return if (candidates.any()) merger(candidates) else null
      }

      fun gatherDefaultDeps(extractor: (DefaultsDeclaration) -> List<Expression>): DependencySet {
        fun toDependencyMap(specs: List<Expression>): DependencySet =
            mclass.loader.resolve(mclass.className.addArgs(specs)).narrowedDependencies

        val deps: List<Dependency> =
            mclass.dependencies.keys.mapNotNull { key ->
              inheritDefault(
                  { toDependencyMap(extractor(it)).getIfPresent(key) },
                  { deps: List<Dependency> -> glb(deps)!! })
            }
        return DependencySet.of(deps)
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
