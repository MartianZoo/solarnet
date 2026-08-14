package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement

/** Premise-resolution metadata supplied alongside one organizational bundle. */
public data class BundleMetadata(
    public val configurationImplications: Set<ConfigurationImplication> = emptySet(),
    /** Additional signed class selections contributed by a Module from this bundle. */
    public val moduleClassSelections: Map<ClassName, Set<ClassSelection>> = emptyMap(),
    /** Each inner set is a group of alternatives, at least one of which must hold. */
    public val bootstrapValidations: List<Set<Requirement>> = emptyList(),
) {
  /** Adds [included] when every [present] class is selected and every [absent] class is not. */
  public data class ConfigurationImplication(
      public val present: Set<ClassName>,
      public val absent: Set<ClassName> = emptySet(),
      public val included: Set<ClassName>,
  ) {
    init {
      require(present.isNotEmpty())
      require(included.isNotEmpty())
    }

    public fun appliesTo(classNames: Set<ClassName>): Boolean =
        classNames.containsAll(present) && classNames.intersect(absent).isEmpty()
  }
}
