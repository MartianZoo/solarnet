package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.types.CTypeDefinition.BaseDependency

/** An actual type type, like the one represented by `CityTile<LandArea>`. */
data class CType(
    val rootType: CTypeDefinition,
    val specializations: Map<BaseDependency, CType> = mapOf(),
    val predicates: List<Predicate> = listOf()) {
  init {
    // specializations.forEach { }
  }
}
