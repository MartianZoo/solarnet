package dev.martianzoo.tfm.types

data class Dependency(val key: Key, val type: PetType) {
  val abstract by type::abstract

  private fun checkKeys(that: Dependency): Dependency =
      that.also { require(this.key == that.key) }

  fun specializes(that: Dependency) = type.isSubtypeOf(checkKeys(that).type)

  fun intersect(that: Dependency): Dependency? = this intersect checkKeys(that).type

  infix fun intersect(otherType: PetType): Dependency? =
      (this.type intersect otherType)?.let { copy(type = it) }

  fun toTypeExpressionFull() = type.toTypeExpression()

  override fun toString() = "$key=${toTypeExpressionFull()}"

  data class Key(val declaringClass: PetClass, val index: Int) {
    init {
      require(index >= 0)
    }

    override fun toString() = "${declaringClass.name}_$index"
  }
}
