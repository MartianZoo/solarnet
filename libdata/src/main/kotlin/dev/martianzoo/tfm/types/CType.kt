package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.util.joinOrEmpty

/** An actual type type, like the one represented by `CityTile<LandArea>`. */
data class CType(
    val cTypeClass: CTypeClass,
    val dependencies: DependencyMap,
    val predicates: Set<Predicate> = setOf()) {
  init {
    // specializations.forEach { }
  }

  fun isSubtypeOf(other: CType): Boolean =
      cTypeClass.isSubclassOf(other.cTypeClass) &&
      dependencies.specializes(other.dependencies)

  fun specialize(specs: DependencyMap): CType {
    return copy(dependencies = dependencies.specialize(specs))
  }

  fun glb(other: CType): CType {
    val newClass = cTypeClass.glb(other.cTypeClass)
    val newDeps = DependencyMap.merge(listOf(dependencies, other.dependencies))
    val newPredicates = predicates.union(other.predicates)
    return CType(newClass, newDeps, newPredicates).also {
      require(it.isSubtypeOf(this))
      require(it.isSubtypeOf(other))
    }
  }

  override fun toString() =
      "$cTypeClass$dependencies${predicates.joinOrEmpty(prefix = "(HAS ", suffix = ")")}"
}
