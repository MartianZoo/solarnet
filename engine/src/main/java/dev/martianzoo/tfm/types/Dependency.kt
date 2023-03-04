package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

internal sealed class Dependency {
  abstract val key: Key
  abstract fun intersect(that: Dependency): Dependency?
  abstract fun lub(that: Dependency?): Dependency?
  abstract val abstract: Boolean
  abstract fun isSubtypeOf(that: Dependency): Boolean
  abstract val expression: Expression // TODO bound?? or common intfc?
  abstract val expressionFull: Expression

  /**
   * Once a class introduces a dependency, like `CLASS Tile<Area>`, all subclasses know that
   * dependency (which they inherit) by the same key, whether they narrow the type or not.
   */
  data class Key(
      /**
       * The name of the class originally declaring this dependency (not just narrowing it from a
       * supertype).
       */
      val declaringClass: ClassName,

      /** The ordinal of this dependency within that list, 0-referenced. */
      val index: Int,
  ) {
    init {
      require(index >= 0)
    }

    override fun toString() = "${declaringClass}_$index"
  }

  /** Any [Dependency] except for the case covered by [ClassDependency] below. */
  data class TypeDependency(override val key: Key, val bound: PType) : Dependency() {
    init {
      require(key != ClassDependency.KEY)
    }
    override val abstract by bound::abstract

    private fun checkKeys(that: Dependency): TypeDependency {
      require(this.key == that.key)
      return that as TypeDependency
    }

    override fun isSubtypeOf(that: Dependency) = bound.isSubtypeOf(checkKeys(that).bound)

    override fun intersect(that: Dependency) = intersect(checkKeys(that).bound)

    fun intersect(otherType: PType): TypeDependency? =
        (this.bound.intersect(otherType))?.let { copy(bound = it) }

    override fun lub(that: Dependency?) = that?.let { lub(checkKeys(it).bound) }

    fun lub(otherType: PType) = copy(bound = bound.lub(otherType))

    fun allConcreteSpecializations(): Sequence<TypeDependency> =
        bound.allConcreteSubtypes().map { TypeDependency(key, it) }

    override val expressionFull by bound::expressionFull
    override val expression by bound::expression
    override fun toString() = "$key=${expression}"
  }

  /**
   * A dependency used *only* by types of the class `Class`; for example `Class<Foo>` (in which
   * example `pclass.name` is `"Foo"`). No other class can use this; for example, one cannot declare
   * that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so instead we
   * use `Production<Class<Plant>>`.
   */
  data class ClassDependency(val bound: PClass) : Dependency() {
    companion object {
      /** The only dependency key that may point to this kind of dependency. */
      val KEY = Key(CLASS, 0)
    }

    override val key: Key by ::KEY
    override val abstract by bound::abstract

    override fun isSubtypeOf(that: Dependency) =
        that is ClassDependency && bound.isSubclassOf(that.bound)

    override fun intersect(that: Dependency): ClassDependency? =
        intersect((that as ClassDependency).bound)

    fun intersect(otherClass: PClass): ClassDependency? =
        bound.intersect(otherClass)?.let { ClassDependency(it) }

    override fun lub(that: Dependency?): ClassDependency =
        ClassDependency(bound.lub((that as ClassDependency).bound))

    override val expressionFull by bound.className::expr
    override val expression by ::expressionFull
    override fun toString() = "$key=${expression}"
  }

  companion object {
    fun intersect(a: List<Dependency>): Dependency? {
      var result: Dependency = a.firstOrNull() ?: return null
      for (next: Dependency in a.drop(1)) {
        result = result.intersect(next)!!
      }
      return result
    }
  }
}
