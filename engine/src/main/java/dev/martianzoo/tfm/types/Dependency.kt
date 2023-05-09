package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.util.Hierarchical

internal sealed class Dependency : Hierarchical<Dependency>, HasExpression, HasClassName {
  abstract val key: Key

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

  abstract val boundClass: MClass

  abstract fun narrows(that: Dependency, info: TypeInfo): Boolean

  /** Any [Dependency] except for the case covered by [FakeDependency] below. */
  data class TypeDependency(override val key: Key, val boundType: MType) :
      Dependency(), HasExpression by boundType {

    override val boundClass by boundType::root
    override val className by boundClass::className

    fun allConcreteSpecializations(): Sequence<TypeDependency> =
        boundType.allConcreteSubtypes().map { TypeDependency(key, it) }

    override fun toString() = "$key=${expression}"

    // Hierarchy

    override val abstract by boundType::abstract

    override fun isSubtypeOf(that: Dependency) = boundType.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency) = (boundType glb boundOf(that))?.let { copy(boundType = it) }

    override fun lub(that: Dependency) = copy(boundType = boundType lub boundOf(that))

    internal fun map(function: (MType) -> MType) = copy(boundType = function(boundType))

    override fun ensureNarrows(that: Dependency, info: TypeInfo) =
        boundType.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) =
        boundType.narrows(boundOf(that), info)

    private fun boundOf(that: Dependency): MType =
        (that as TypeDependency).boundType.also { require(key == that.key) }
  }

  /**
   * A dependency used *only* by types of the class `Class`; for example `Class<Foo>` (in which
   * example `mclass.name` is `"Foo"`). No other class can use this; for example, one cannot declare
   * that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so instead we
   * use `Production<Class<Plant>>`.
   */
  private data class FakeDependency(override val boundClass: MClass) : Dependency() {
    override val key: Key = Key(CLASS, 0)

    override val className by boundClass::className
    override val expression by className::expression
    override val expressionFull by ::expression

    override fun toString() = "$key=$expression"

    // Hierarchy

    override val abstract by boundClass::abstract

    override fun isSubtypeOf(that: Dependency) = boundClass.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): FakeDependency? =
        (boundClass glb boundOf(that))?.let(::copy)

    override fun lub(that: Dependency): FakeDependency = copy(boundClass lub boundOf(that))

    override fun ensureNarrows(that: Dependency, info: TypeInfo) =
        boundClass.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) =
        boundClass.isSubtypeOf(boundOf(that))

    private fun boundOf(that: Dependency): MClass =
        (that as FakeDependency).boundClass.also { require(key == that.key) }
  }

  companion object {
    // Note these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: Set<Dependency>) {
      require(deps.all { it is TypeDependency } || deps.single() is FakeDependency)
    }

    internal fun getClassForClassType(set: Set<Dependency>): MClass =
        (set.single() as FakeDependency).boundClass

    internal fun depsForClassType(mclass: MClass) = DependencySet.of(setOf(FakeDependency(mclass)))
  }
}
