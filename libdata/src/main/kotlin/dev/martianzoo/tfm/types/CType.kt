package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Predicate

sealed interface CType {
  object This : CType
  object Me : CType

  data class RegularCType(
      val rootType: CTypeDefinition,
      val specializations: Map<BaseDependency, CType> = mapOf(),
      val predicates: List<Predicate> = listOf()) : CType {
  }
}
