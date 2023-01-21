package dev.martianzoo.tfm.types

public data class Dependency(val key: Key, val type: PetType) {
  public val abstract by type::abstract

  private fun checkKeys(that: Dependency): Dependency =
      that.also { require(this.key == that.key) }

  public fun specializes(that: Dependency) = type.isSubtypeOf(checkKeys(that).type)

  public fun intersect(that: Dependency): Dependency? = this intersect checkKeys(that).type

  public infix fun intersect(otherType: PetType): Dependency? =
      (this.type intersect otherType)?.let { copy(type = it) }

  public fun toTypeExpressionFull() = type.toTypeExpression()

  override fun toString() = "$key=${toTypeExpressionFull()}"

  public data class Key(val declaringClass: PetClass, val index: Int) {
    init {
      require(index >= 0)
    }

    override fun toString() = "${declaringClass.name}_$index"
  }
}
