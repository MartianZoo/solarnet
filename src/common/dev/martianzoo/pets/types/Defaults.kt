package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind
import dev.martianzoo.pets.types.Dependency.TypeDependency

/**
 * The three independently inherited default sets defined by
 * [rule 10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
 * Defaults provide authored context during elaboration; they do not change which types exist.
 *
 * @constructor Groups the independently inherited [allUsages], [gainOnly], and [removeOnly] sets
 *   separated by
 *   [rule 10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
 */
public data class Defaults(
    /**
     * Dependency defaults applied to every use of the class under
     * [rule 10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
     */
    val allUsages: DefaultSpec,

    /**
     * Dependency and intensity defaults applied only to gains under
     * [rules 10-1 and 10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
     */
    val gainOnly: DefaultSpec,

    /**
     * Dependency and intensity defaults applied only to removals under
     * [rules 10-1 and 10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
     */
    val removeOnly: DefaultSpec,
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

      val gainIntensity =
          inheritDefault(klass, { it.defaultsDecl.gainOnly.intensity }, onlyOne(klass, "gain"))!!
      val removeIntensity =
          inheritDefault(
              klass,
              { it.defaultsDecl.removeOnly.intensity },
              onlyOne(klass, "removal"),
          )!!

      return Defaults(
          allUsages = DefaultSpec(allUsagesDeps, null),
          gainOnly = DefaultSpec(gainDeps, gainIntensity),
          removeOnly = DefaultSpec(removeDeps, removeIntensity),
      )
    }

    /** Reports the class rather than failing anonymously when supertypes disagree. */
    private fun <T> onlyOne(klass: Class, kind: String): (List<T>) -> T = { candidates ->
      candidates.singleOrNull()
          ?: throw invalidPetDefinition(
              "${klass.className} inherits conflicting $kind intensity defaults: " +
                  candidates.joinToString()
          )
    }

    private fun <T> inheritDefault(
        klass: Class,
        extractor: (Class) -> T?,
        merger: (List<T>) -> T = { it.single() },
    ): T? {
      val haveDefault: List<Class> = klass.allSuperclasses().filter { extractor(it) != null }

      // Anything that was overridden by *any* of our superclasses must be discarded
      val overridden = haveDefault.flatMap { it.properSuperclasses() }.toSet()
      val inheritFrom = haveDefault - overridden
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
                { deps: List<Dependency> ->
                  deps.reduce { left, right ->
                    (left glb right)
                        ?: throw invalidPetDefinition(
                            "${klass.className} inherits incompatible defaults for $key: " +
                                "$left and $right"
                        )
                  }
                },
            )
          }
      return DependencySet.of(deps)
    }
  }

  /**
   * One inherited default set for a use kind, combined according to
   * [rules 10-2 through 10-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
   *
   * @constructor Associates narrowed default [dependencies] with the gain or removal [intensity]
   *   under
   *   [rules 10-1 and 10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
   */
  public data class DefaultSpec(
      /**
       * Only dependency bounds narrowed by defaults; omitted keys retain declared bounds under
       * [rule 10-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
       */
      val dependencies: DependencySet = DependencySet.of(),

      /**
       * The gain or removal intensity inherited independently under
       * [rule 10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults),
       * or null for all-usage defaults.
       */
      val intensity: Intensity?,
  )
}
