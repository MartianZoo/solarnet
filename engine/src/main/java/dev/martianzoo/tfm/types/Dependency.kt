package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.Hierarchical

internal sealed class Dependency : Hierarchical<Dependency> { // TODO HasExpression
  abstract val key: Key
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

  /** Any [Dependency] except for the case covered by [FakeDependency] below. */
  // TODO rename Dependency?
  data class TypeDependency(override val key: Key, val bound: PType) : Dependency() {
    fun allConcreteSpecializations(): Sequence<TypeDependency> =
        bound.allConcreteSubtypes().map { TypeDependency(key, it) }

    override val expressionFull by bound::expressionFull

    override val expression by bound::expression

    override fun toString() = "$key=${expression}"

    // Hierarchy

    override val abstract by bound::abstract

    override fun isSubtypeOf(that: Dependency) = bound.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency) = (bound glb boundOf(that))?.let { copy(bound = it) }

    override fun lub(that: Dependency) = copy(bound = bound lub boundOf(that))

    private fun boundOf(that: Dependency): PType =
        (that as TypeDependency).bound.also { require(key == that.key) }

  }

  /**
   * A dependency used *only* by types of the class `Class`; for example `Class<Foo>` (in which
   * example `pclass.name` is `"Foo"`). No other class can use this; for example, one cannot declare
   * that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so instead we
   * use `Production<Class<Plant>>`.
   */
  private data class FakeDependency(val bound: PClass) : Dependency() {
    override val key: Key = Key(CLASS, 0)

    override val expressionFull by bound.className::expr

    override val expression by ::expressionFull

    override fun toString() = "$key=${expression}"

    // Hierarchy

    override val abstract by bound::abstract

    override fun isSubtypeOf(that: Dependency) = bound.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): FakeDependency? = (bound glb boundOf(that))?.let(::copy)

    override fun lub(that: Dependency): FakeDependency = copy(bound lub boundOf(that))

    private fun boundOf(that: Dependency): PClass =
        (that as FakeDependency).bound.also { require(key == that.key) }

  }

  companion object {
    // TODO these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: Set<Dependency>) {
      require(deps.all { it is TypeDependency } || deps.single() is FakeDependency)
    }

    internal fun getClassForClassType(set: Set<Dependency>): PClass =
        (set.single() as FakeDependency).bound

    internal fun depsForClassType(pclass: PClass) = DependencySet(setOf(FakeDependency(pclass)))
  }
}
