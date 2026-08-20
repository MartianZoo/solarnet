package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.DefaultKind
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical.Companion.glb

public data class Defaults(
    val allUsages: DefaultSpec,
    val gainOnly: DefaultSpec,
    val removeOnly: DefaultSpec,
    val triggerOnly: DefaultSpec,
) {
  internal companion object {
    /**
     * Determines the [Defaults] for this class, taking into account its own declaration and that of
     * all its superclasses.
     */
    internal fun forClass(klass: Class): Defaults {
      val allUsagesDeps: DependencySet = gatherDefaultDeps(klass, DefaultKind.ALL_USAGES)
      val gainDeps: DependencySet = gatherDefaultDeps(klass, DefaultKind.GAIN_ONLY)
      val removeDeps: DependencySet = gatherDefaultDeps(klass, DefaultKind.REMOVE_ONLY)
      val triggerDeps: DependencySet = gatherDefaultDeps(klass, DefaultKind.TRIGGER_ONLY)

      val gainIntensity = inheritDefault(klass, { it.defaultsDecl.gainOnly.intensity })!!
      val removeIntensity = inheritDefault(klass, { it.defaultsDecl.removeOnly.intensity })!!

      return Defaults(
          allUsages = DefaultSpec(allUsagesDeps, null),
          gainOnly = DefaultSpec(gainDeps, gainIntensity),
          removeOnly = DefaultSpec(removeDeps, removeIntensity),
          triggerOnly = DefaultSpec(triggerDeps, null),
      )
    }

    private fun <T> inheritDefault(
        klass: Class,
        extractor: (Class) -> T?,
        merger: (List<T>) -> T = { it.single() },
    ): T? {
      val haveDefault: List<Class> = klass.allSuperclasses().filter { extractor(it) != null }

      // Anything that was overridden by *any* of our superclasses must be discarded
      val lasdfasdf = haveDefault.flatMap { it.properSuperclasses() }.toSet()
      val inheritFrom = haveDefault - lasdfasdf
      val candidates: List<T> = inheritFrom.map { extractor(it)!! }.distinct()

      return if (candidates.any()) merger(candidates) else null
    }

    private fun gatherDefaultDeps(klass: Class, kind: DefaultKind): DependencySet {
      // TODO: this is complex and this human doesn't understand it
      fun toDependencyMap(origin: Class, specs: List<Expression>): DependencySet {
        val resolved = origin.classTable.resolve(origin.className.of(specs)).narrowedDependencies
        if (OWNER.expression !in specs) return resolved

        // Owner also acts as a contextual variable. Don't normalize that variable to its bound
        // before it can be replaced with Player1, etc.
        val ownerKey =
            origin.dependencies
                .matchPartial(listOf(OWNER.expression))
                .typeDependencies()
                .single()
                .key
        val owner = TypeDependency(ownerKey, klass.classTable.resolve(OWNER.expression))
        return resolved.merge(DependencySet.of(setOf(owner))) { _, contextual -> contextual }
      }

      val deps: List<Dependency> =
          klass.dependencies.keys.mapNotNull { key ->
            inheritDefault(
                klass,
                { origin ->
                  val inherited =
                      toDependencyMap(origin, origin.defaultsDecl.default(kind).specs)
                          .getIfPresent(key)
                  if (inherited?.expression == OWNER.expression) {
                    inherited
                  } else {
                    inherited?.glb(klass.dependencies.get(key))
                  }
                },
                { deps: List<Dependency> -> glb(deps)!! },
            )
          }
      return DependencySet.of(deps)
    }
  }

  public data class DefaultSpec(
      val dependencies: DependencySet = DependencySet.of(),
      val intensity: Intensity?,
  )
}
