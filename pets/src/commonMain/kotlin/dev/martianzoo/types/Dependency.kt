package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.api.TypeInfo.NoGameState
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.util.Hierarchical

public sealed class Dependency : Hierarchical<Dependency>, HasExpression, HasClassName {
  public abstract val key: Key

  /**
   * Once a class introduces a dependency, like `CLASS Tile<Area>`, all subclasses know that
   * dependency (which they inherit) by the same key, whether they narrow the type or not.
   */
  public data class Key(
      /**
       * The name of the class originally declaring this dependency (not just narrowing it from a
       * supertype).
       */
      private val declaringClass: ClassName,

      /** The ordinal of this dependency within that list, 0-referenced. */
      val index: Int,
  ) {
    init {
      require(index >= 0)
    }

    override fun toString(): String = "${declaringClass}_$index"
  }

  internal abstract val boundClass: Class

  internal abstract fun narrows(that: Dependency, info: TypeInfo): Boolean

  internal abstract fun intersect(expression: Expression): Dependency?

  /** Any [Dependency] except for the case covered by [FakeDependency] below. */
  public data class TypeDependency(override val key: Key, val boundType: Type) :
      Dependency(), HasExpression by boundType {

    override val boundClass by boundType::rootClass
    override val className: ClassName by boundClass::className

    internal fun allConcreteSpecializations(): Sequence<TypeDependency> =
        boundType.allConcreteSubtypes().map { TypeDependency(key, it) }

    override fun toString(): String = "$key=$expressionFull"

    // Hierarchy

    override val abstract: Boolean by boundType::abstract

    override fun isSubtypeOf(that: Dependency): Boolean = boundType.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): TypeDependency? {
      if (that is ComplementDependency) {
        return if (narrows(that, NoGameState)) this else null
      }
      if (that !is TypeDependency) return null
      return (boundType glb boundOf(that))?.let { copy(boundType = it) }
    }

    override fun lub(that: Dependency): Dependency =
        when (that) {
          is ComplementDependency -> if (narrows(that, NoGameState)) that else that.domain()
          else -> copy(boundType = boundType lub boundOf(that))
        }

    internal inline fun map(function: (Type) -> Type) = copy(boundType = function(boundType))

    override fun intersect(expression: Expression): Dependency? {
      if (expression.complement) {
        val excluded = boundType.typeUniverse.resolve(expression.uncomplemented())
        if (!excluded.isSubtypeOf(boundType)) return null
        return ComplementDependency(key, boundType, excluded)
      }
      return glb(copy(boundType = boundType.typeUniverse.resolve(expression)))
    }

    override fun ensureNarrows(that: Dependency, info: TypeInfo): Unit =
        boundType.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) =
        when (that) {
          is ComplementDependency -> that.matches(boundType, info)
          else -> boundType.narrows(boundOf(that), info)
        }

    private fun boundOf(that: Dependency): Type =
        (that as TypeDependency).boundType.also { require(key == that.key) }
  }

  /** A dependency constrained to exclude one narrower type, as in `OwnedTile<!Player1>`. */
  internal data class ComplementDependency(
      override val key: Key,
      internal val domainType: Type,
      internal val excludedType: Type,
  ) : Dependency(), HasExpression {
    init {
      require(excludedType.isSubtypeOf(domainType)) { "$excludedType does not narrow $domainType" }
    }

    override val boundClass by domainType::rootClass
    override val className by excludedType::className
    override val expression: Expression = excludedType.expression.copy(complement = true)
    override val expressionFull: Expression = excludedType.expressionFull.copy(complement = true)

    internal fun domain() = TypeDependency(key, domainType)

    internal fun allConcreteSpecializations(): Sequence<TypeDependency> =
        domainType
            .allConcreteSubtypes()
            .filterNot { it.isSubtypeOf(excludedType) }
            .map { TypeDependency(key, it) }

    internal fun matches(type: Type, info: TypeInfo): Boolean =
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
          is TypeDependency -> if (that.narrows(this, NoGameState)) this else domain()
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
   * example `boundClass.name` is `"Foo"`). No other class can use this; for example, one cannot
   * declare that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so
   * instead we use `Production<Class<Plant>>`.
   */
  private data class FakeDependency(override val boundClass: Class) : Dependency() {
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

    private fun boundOf(that: Dependency): Class =
        (that as FakeDependency).boundClass.also { require(key == that.key) }

    override fun intersect(expression: Expression): FakeDependency? {
      if (!expression.simple) return null
      val klass = boundClass.typeUniverse.getClass(expression.className)
      return glb(FakeDependency(klass))
    }
  }

  internal companion object {
    // Note these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: Set<Dependency>) {
      require(deps.none { it is FakeDependency } || deps.single() is FakeDependency)
      require(deps.map { it.boundClass.typeUniverse }.distinct().size <= 1) {
        "dependencies belong to different type universes"
      }
    }

    internal fun isForClassType(set: Set<Dependency>) = set.singleOrNull() is FakeDependency

    internal fun getClassForClassType(set: Set<Dependency>): Class =
        (set.single() as FakeDependency).boundClass

    internal fun depsForClassType(klass: Class) = DependencySet.of(setOf(FakeDependency(klass)))
  }
}
