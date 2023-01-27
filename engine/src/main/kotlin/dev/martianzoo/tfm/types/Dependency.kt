package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpr

interface Dependency {
  val key: Key
  val abstract: Boolean
  fun checkKeys(that: Dependency): Dependency
  fun specializes(that: Dependency): Boolean
  fun intersect(that: Dependency): Dependency?

  fun toTypeExprFull(): TypeExpr

  public data class Key(val declaringClass: PClass, val index: Int) {
    init {
      require(index >= 0)
    }

    override fun toString() = "${declaringClass.name}_$index"
  }

  public data class TypeDependency(override val key: Key, val ptype: PType) : Dependency {
    override val abstract by ptype::abstract

    override fun checkKeys(that: Dependency): TypeDependency {
      require(this.key == that.key)
      return that as TypeDependency
    }

    override fun specializes(that: Dependency) = ptype.isSubtypeOf(checkKeys(that).ptype)

    override fun intersect(that: Dependency): TypeDependency? = this intersect checkKeys(that).ptype

    infix fun intersect(otherType: PType): TypeDependency? =
        (this.ptype intersect otherType)?.let { copy(ptype = it) }

    override fun toTypeExprFull() = ptype.toTypeExprFull()

    override fun toString() = "$key=${toTypeExprFull()}"
  }

  /** Okay this is used ONLY by Class_0, and the value is just a class, like just Tile.
   * */
  public data class ClassDependency(override val key: Key, val pclass: PClass) : Dependency {
    override val abstract by pclass::abstract

    override fun checkKeys(that: Dependency): ClassDependency {
      require(this.key == that.key)
      return that as ClassDependency
    }

    override fun specializes(that: Dependency) = pclass.isSubclassOf(checkKeys(that).pclass)

    override fun intersect(that: Dependency): ClassDependency? =
        this intersect checkKeys(that).pclass

    infix fun intersect(otherType: PClass): ClassDependency? =
        (this.pclass intersect otherType)?.let { copy(pclass = it) }

    override fun toTypeExprFull() = pclass.name.type

    override fun toString() = "$key=${toTypeExprFull()}"
  }

}
