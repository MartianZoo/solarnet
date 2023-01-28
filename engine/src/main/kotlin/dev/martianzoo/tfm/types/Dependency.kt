package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpr

abstract class Dependency {
  abstract val key: Key
  abstract val abstract: Boolean
  abstract fun specializes(that: Dependency): Boolean
  abstract fun intersect(that: Dependency): Dependency?

  abstract fun toTypeExprFull(): TypeExpr

  public data class Key(val declaringClass: ClassName, val index: Int) {
    init {
      require(index >= 0)
    }
    override fun toString() = "${declaringClass}_$index"
  }

  public data class TypeDependency(override val key: Key, val ptype: PType) : Dependency() {
    override val abstract by ptype::abstract

    private fun checkKeys(that: Dependency): TypeDependency {
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

  /** Okay this is used ONLY by Class_0, and the value is just a class, like just Tile. */
  public data class ClassDependency(val pclass: PClass) : Dependency() {
    companion object {
      val KEY = Key(CLASS, 0)
    }

    override val key: Key by ::KEY
    override val abstract by pclass::abstract

    override fun specializes(that: Dependency) =
        pclass.isSubclassOf((that as ClassDependency).pclass)

    override fun intersect(that: Dependency): ClassDependency? =
        this intersect (that as ClassDependency).pclass

    infix fun intersect(otherClass: PClass): ClassDependency? =
        (this.pclass intersect otherClass)?.let { copy(pclass = it) }

    override fun toTypeExprFull() = pclass.name.type
    override fun toString() = "$key=${toTypeExprFull()}"
  }
}
