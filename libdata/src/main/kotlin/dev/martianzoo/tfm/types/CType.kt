package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Predicate

/** An actual type type, like the one represented by `CityTile<LandArea>`. */
data class CType(
    val rootType: CTypeClass,
    val dependencies: DependencyMap,
    val predicates: List<Predicate> = listOf()) {
  init {
    // specializations.forEach { }
  }

  fun isSubtypeOf(other: CType): Boolean =
      rootType.isSubclassOf(other.rootType) &&
      dependencies.specializes(other.dependencies)

  fun specialize(specs: DependencyMap): CType {
    return copy(dependencies = dependencies.specialize(specs))
  }

  override fun toString(): String {
    var s = "$rootType$dependencies"
    if (predicates.isNotEmpty()) {
      s += "(HAS ${predicates.joinToString()})"
    }
    return s
  }

  companion object {
    fun min(a: CType, b: CType) = when {
      a.isSubtypeOf(b) -> a
      b.isSubtypeOf(a) -> b
      else -> error("")
    }
  }
}
