package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.DefaultKind
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical.Companion.glb

internal data class Defaults(
    val allUsages: DefaultSpec,
    val gainOnly: DefaultSpec,
    val removeOnly: DefaultSpec,
) {
  companion object {
    /**
     * Determines the [Defaults] for this class, taking into account its own declaration and that of
     * all its superclasses.
     */
    fun forClass(mclass: MClass): Defaults {
      val allUsagesDeps: DependencySet = gatherDefaultDeps(mclass, DefaultKind.ALL_USAGES)
      val gainDeps: DependencySet = gatherDefaultDeps(mclass, DefaultKind.GAIN_ONLY)
      val removeDeps: DependencySet = gatherDefaultDeps(mclass, DefaultKind.REMOVE_ONLY)

      val gainIntensity = inheritDefault(mclass, { it.gainOnly.intensity })!!
      val removeIntensity = inheritDefault(mclass, { it.removeOnly.intensity })!!

      return Defaults(
          allUsages = DefaultSpec(allUsagesDeps, null),
          gainOnly = DefaultSpec(gainDeps, gainIntensity),
          removeOnly = DefaultSpec(removeDeps, removeIntensity),
      )
    }

    private fun <T> inheritDefault(
        mclass: MClass,
        extractor: (DefaultsDeclaration) -> T?,
        merger: (List<T>) -> T = { it.single() },
    ): T? {
      fun extractFromClass(c: MClass): T? = extractor(c.defaultsDecl)

      val haveDefault: List<MClass> =
          mclass.allSuperclasses().filter { extractFromClass(it) != null }

      // Anything that was overridden by *any* of our superclasses must be discarded
      val lasdfasdf = haveDefault.flatMap { it.properSuperclasses() }.toSet()
      val inheritFrom = haveDefault - lasdfasdf
      val candidates: List<T> = inheritFrom.map { extractFromClass(it)!! }.distinct()

      return if (candidates.any()) merger(candidates) else null
    }

    private fun gatherDefaultDeps(mclass: MClass, kind: DefaultKind): DependencySet {
      // TODO: this is complex and this human doesn't understand it
      fun toDependencyMap(specs: List<Expression>): DependencySet {
        val resolved = mclass.loader.resolve(mclass.className.of(specs)).narrowedDependencies
        if (OWNER.expression !in specs) return resolved

        // Owner also acts as a contextual variable. Don't normalize that variable to its bound
        // before it can be replaced with Player1, etc.
        val ownerKey =
            mclass.dependencies
                .matchPartial(listOf(OWNER.expression))
                .typeDependencies()
                .single()
                .key
        val owner = TypeDependency(ownerKey, mclass.loader.resolve(OWNER.expression))
        return resolved.merge(DependencySet.of(setOf(owner))) { _, contextual -> contextual }
      }

      val deps: List<Dependency> =
          mclass.dependencies.keys.mapNotNull { key ->
            inheritDefault(
                mclass,
                { toDependencyMap(it.default(kind).specs).getIfPresent(key) },
                { deps: List<Dependency> -> glb(deps)!! },
            )
          }
      return DependencySet.of(deps)
    }
  }

  data class DefaultSpec(
      val dependencies: DependencySet = DependencySet.of(),
      val intensity: Intensity?,
  )
}
