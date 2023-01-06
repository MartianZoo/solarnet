package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression

data class Dependency(val key: DependencyKey, val type: PetType) {
  val abstract by type::abstract

  fun combine(that: Dependency) =
      copy(type = type.glb(checkKeys(that).type))

  private fun checkKeys(that: Dependency) =
      that.also { require(this.key == that.key) }

  fun specializes(that: Dependency) =
      type.isSubtypeOf(checkKeys(that).type)

  fun acceptsSpecialization(specType: PetType) = type.hasGlbWith(specType)

  fun specialize(spec: TypeExpression, loader: PetClassLoader): Dependency {
    return copy(type = loader.resolve(spec).glb(type))
  }

  fun toTypeExpressionFull() = type.toTypeExpressionFull()

  override fun toString() = "$key=${toTypeExpressionFull()}"
}
