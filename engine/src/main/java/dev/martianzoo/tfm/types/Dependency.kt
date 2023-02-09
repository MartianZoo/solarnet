package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.ClassName

internal sealed class Dependency: Type {
  abstract val key: Key
  override val refinement = null
  abstract fun intersect(that: Dependency): Dependency?

  /**
   * Once a class introduces a dependency, like `CLASS Tile<Area>`, all subclasses know that
   * dependency (which they inherit) by the same key, whether they narrow the type or not.
   */
  data class Key(val declaringClass: ClassName, val index: Int) {
    init {
      require(index >= 0)
    }
    override fun toString() = "${declaringClass}_$index"
  }

  /** Any [Dependency] except for the case covered by [ClassDependency] below. */
  data class TypeDependency(override val key: Key, val ptype: PType) : Dependency() {
    init {
      require(key != ClassDependency.KEY)
    }
    override val className by ptype::className
    override val abstract by ptype::abstract

    private fun checkKeys(that: Dependency): TypeDependency {
      require(this.key == that.key)
      return that as TypeDependency
    }

    override fun isSubtypeOf(that: Type) =
        that is Dependency && ptype.isSubtypeOf(checkKeys(that).ptype)

    override fun intersect(that: Dependency): TypeDependency? = this intersect checkKeys(that).ptype

    infix fun intersect(otherType: PType): TypeDependency? =
        (this.ptype intersect otherType)?.let { copy(ptype = it) }

    override val typeExprFull by ptype::typeExprFull
    override val typeExpr by ptype::typeExpr
    override fun toString() = "$key=${typeExpr}"
  }

  /**
   * A dependency used *only* by types of the class `Class`; for example `Class<Foo>` (in which
   * example `pclass.name` is `"Foo"`). No other class can use this; for example, one cannot declare
   * that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so instead we
   * use `Production<Class<Plant>>`.
   */
  data class ClassDependency(val pclass: PClass) : Dependency() {
    companion object {
      /** The only dependency key that may point to this kind of dependency. */
      val KEY = Key(CLASS, 0)
    }

    override val key: Key by ::KEY
    override val className by pclass::className
    override val abstract by pclass::abstract

    override fun isSubtypeOf(that: Type) =
        that is ClassDependency && pclass.isSubclassOf(that.pclass)

    override fun intersect(that: Dependency): ClassDependency? =
        this intersect (that as ClassDependency).pclass

    infix fun intersect(otherClass: PClass): ClassDependency? =
        (this.pclass intersect otherClass)?.let { copy(pclass = it) }

    override val typeExprFull by pclass.className::type
    override val typeExpr by pclass.className::type
    override fun toString() = "$key=${typeExpr}"
  }
}
