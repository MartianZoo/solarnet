package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.Predicate
import dev.martianzoo.util.joinOrEmpty

/** An actual type type, like the one represented by `CityTile<LandArea>`. */
data class ComponentType(
    val componentClass: ComponentClass,
    val dependencies: DependencyMap,
    val predicates: Set<Predicate> = setOf()) {
  init {
    // specializations.forEach { }
  }

  fun isSubtypeOf(other: ComponentType): Boolean =
      componentClass.isSubclassOf(other.componentClass) &&
      dependencies.specializes(other.dependencies)

  fun specialize(specs: DependencyMap): ComponentType {
    return copy(dependencies = dependencies.specialize(specs))
  }

  fun glb(other: ComponentType): ComponentType {
    val newClass = componentClass.glb(other.componentClass)
    val newDeps = DependencyMap.merge(listOf(dependencies, other.dependencies))
    val newPredicates = predicates.union(other.predicates)
    return ComponentType(newClass, newDeps, newPredicates).also {
      require(it.isSubtypeOf(this))
      require(it.isSubtypeOf(other))
    }
  }

  override fun toString() =
      "$componentClass$dependencies${predicates.joinOrEmpty(prefix = "(HAS ", suffix = ")")}"
}
