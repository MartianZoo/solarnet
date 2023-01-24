package dev.martianzoo.tfm.types

public data class Dependency(val key: Key, val ptype: PType) {
  public val abstract by ptype::abstract

  private fun checkKeys(that: Dependency): Dependency = that.also { require(this.key == that.key) }

  public fun specializes(that: Dependency) = ptype.isSubtypeOf(checkKeys(that).ptype)

  public fun intersect(that: Dependency): Dependency? = this intersect checkKeys(that).ptype

  public infix fun intersect(otherType: PType): Dependency? =
      (this.ptype intersect otherType)?.let { copy(ptype = it) }

  public fun toTypeExprFull() = ptype.toTypeExprFull()

  override fun toString() = "$key=${toTypeExprFull()}"

  public data class Key(val declaringClass: PClass, val index: Int) {
    init {
      require(index >= 0)
    }

    override fun toString() = "${declaringClass.name}_$index"
  }
}
