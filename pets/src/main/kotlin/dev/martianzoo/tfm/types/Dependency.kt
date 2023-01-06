package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression

data class Dependency(val key: Key, val type: PetType) {
  val abstract by type::abstract

  private fun checkKeys(that: Dependency): Dependency =
      that.also { require(this.key == that.key) }

  fun specializes(that: Dependency) =
      type.isSubtypeOf(checkKeys(that).type)

  fun canIntersect(that: Dependency) = this.type.canIntersect(that.type)

  fun intersect(that: Dependency): Dependency =
      this intersect checkKeys(that).type

  infix fun intersect(otherType: PetType) =
      copy(type = this.type intersect otherType)

  fun intersect(otherType: TypeExpression, loader: PetClassLoader) =
      this intersect loader.resolve(otherType)

  fun toTypeExpressionFull() = type.toTypeExpressionFull()

  override fun toString() = "$key=${toTypeExpressionFull()}"

  data class Key(val declaringClass: PetClass, val index: Int) {
    init { require(index >= 0) }
    override fun toString() = "${declaringClass.name}_$index"
  }
}
