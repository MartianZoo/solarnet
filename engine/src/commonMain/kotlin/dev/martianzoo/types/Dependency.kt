package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.util.Hierarchical

public sealed class Dependency : Hierarchical<Dependency>, HasExpression, HasClassName {
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

  abstract fun intersect(expression: Expression): Dependency?

  /** Any [Dependency] except for the case covered by [FakeDependency] below. */
  data class TypeDependency(override val key: Key, val boundType: MType) :
      Dependency(), HasExpression by boundType {

    override val boundClass by boundType::root
    override val className by boundClass::className

    fun allConcreteSpecializations(): Sequence<TypeDependency> =
        boundType.allConcreteSubtypes().map { TypeDependency(key, it) }

    override fun toString() = "$key=$expressionFull"

    // Hierarchy

    override val abstract by boundType::abstract

    override fun isSubtypeOf(that: Dependency) = boundType.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): TypeDependency? {
      if (that is ComplementDependency) {
        return if (narrows(that, StubTypeInfo)) this else null
      }
      if (that !is TypeDependency) return null
      return (boundType glb boundOf(that))?.let { copy(boundType = it) }
    }

    override fun lub(that: Dependency) =
        when (that) {
          is ComplementDependency -> if (narrows(that, StubTypeInfo)) that else that.domain()
          else -> copy(boundType = boundType lub boundOf(that))
        }

    internal inline fun map(function: (MType) -> MType) = copy(boundType = function(boundType))

    override fun intersect(expression: Expression): Dependency? {
      if (expression.complement) {
        val excluded = boundType.loader.resolve(expression.uncomplemented())
        if (!excluded.narrows(boundType)) return null
        return ComplementDependency(key, boundType, excluded)
      }
      return glb(copy(boundType = boundType.loader.resolve(expression)))
    }

    override fun ensureNarrows(that: Dependency, info: TypeInfo) =
        boundType.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) =
        when (that) {
          is ComplementDependency -> that.matches(boundType, info)
          else -> boundType.narrows(boundOf(that), info)
        }

    private fun boundOf(that: Dependency): MType =
        (that as TypeDependency).boundType.also { require(key == that.key) }
  }

  /** A dependency constrained to exclude one narrower type, as in `OwnedTile<!Player1>`. */
  data class ComplementDependency(
      override val key: Key,
      internal val domainType: MType,
      internal val excludedType: MType,
  ) : Dependency(), HasExpression {
    init {
      require(excludedType.narrows(domainType)) { "$excludedType does not narrow $domainType" }
    }

    override val boundClass by domainType::root
    override val className by excludedType::className
    override val expression: Expression = excludedType.expression.copy(complement = true)
    override val expressionFull: Expression = excludedType.expressionFull.copy(complement = true)

    fun domain() = TypeDependency(key, domainType)

    fun allConcreteSpecializations(): Sequence<TypeDependency> =
        domainType
            .allConcreteSubtypes()
            .filterNot { it.narrows(excludedType) }
            .map { TypeDependency(key, it) }

    fun matches(type: MType, info: TypeInfo): Boolean =
        type.narrows(domainType, info) && !type.narrows(excludedType, info)

    override fun toString() = "$key=$expressionFull"

    override val abstract: Boolean = true

    override fun isSubtypeOf(that: Dependency) =
        when (that) {
          is TypeDependency -> domainType.isSubtypeOf(that.boundType)
          is ComplementDependency -> domainType.isSubtypeOf(that.domainType)
          else -> false
        }

    override fun glb(that: Dependency): Dependency? =
        when (that) {
          is TypeDependency -> that.glb(this)
          is ComplementDependency ->
              if (excludedType == that.excludedType) {
                (domainType glb that.domainType)?.let { copy(domainType = it) }
              } else {
                null
              }
          else -> null
        }

    override fun lub(that: Dependency): Dependency =
        when (that) {
          is TypeDependency -> if (that.narrows(this, StubTypeInfo)) this else domain()
          is ComplementDependency ->
              if (excludedType == that.excludedType) {
                copy(domainType = domainType lub that.domainType)
              } else {
                domain()
              }
          else -> domain()
        }

    override fun ensureNarrows(that: Dependency, info: TypeInfo) {
      if (!narrows(that, info)) {
        domainType.ensureNarrows((that as TypeDependency).boundType, info)
      }
    }

    override fun narrows(that: Dependency, info: TypeInfo) =
        when (that) {
          is TypeDependency -> domainType.narrows(that.boundType, info)
          is ComplementDependency ->
              domainType.narrows(that.domainType, info) && excludedType == that.excludedType
          else -> false
        }

    override fun intersect(expression: Expression): Dependency? =
        domain().intersect(expression)?.let { it glb this }
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

    override fun toString() = "$key=$expressionFull"

    // Hierarchy

    override val abstract by boundClass::abstract

    override fun isSubtypeOf(that: Dependency) = boundClass.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): FakeDependency? {
      if (that !is FakeDependency) return null
      return (boundClass glb boundOf(that))?.let(::copy)
    }

    override fun lub(that: Dependency): FakeDependency =
        FakeDependency(boundClass lub boundOf(that))

    override fun ensureNarrows(that: Dependency, info: TypeInfo) =
        boundClass.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) = boundClass.isSubtypeOf(boundOf(that))

    private fun boundOf(that: Dependency): MClass =
        (that as FakeDependency).boundClass.also { require(key == that.key) }

    override fun intersect(expression: Expression): FakeDependency? {
      if (!expression.simple) return null
      val mclass = boundClass.loader.getClass(expression.className)
      return glb(FakeDependency(mclass))
    }
  }

  companion object {
    // Note these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: Set<Dependency>) {
      require(deps.none { it is FakeDependency } || deps.single() is FakeDependency)
    }

    internal fun isForClassType(set: Set<Dependency>) = set.singleOrNull() is FakeDependency

    internal fun getClassForClassType(set: Set<Dependency>): MClass =
        (set.single() as FakeDependency).boundClass

    internal fun depsForClassType(mclass: MClass) = DependencySet.of(setOf(FakeDependency(mclass)))
  }
}
