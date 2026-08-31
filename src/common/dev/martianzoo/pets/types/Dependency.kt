package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

public sealed class Dependency : Specification<Dependency>, HasExpression, HasClassName {
  public abstract val key: Key
  public abstract val abstract: Boolean

  override fun isAbstract(info: TypeInfo): Boolean = abstract

  public abstract fun isSubtypeOf(that: Dependency): Boolean

  /** Returns whether this dependency is a supertype of [that], including equality. */
  public fun isSupertypeOf(that: Dependency): Boolean = that.isSubtypeOf(this)

  public abstract infix fun glb(that: Dependency): Dependency?

  public abstract infix fun lub(that: Dependency): Dependency

  /**
   * Once a class introduces a dependency, like `CLASS Tile<Area>`, all subclasses know that
   * dependency (which they inherit) by the same key, whether they narrow the type or not.
   */
  public data class Key(
      /**
       * The name of the class originally declaring this dependency (not just narrowing it from a
       * supertype).
       */
      public val declaringClass: ClassName,

      /** The ordinal of this dependency within that list, 0-referenced. */
      public val index: Int,
  ) {
    init {
      require(index >= 0)
    }

    override fun toString(): String = "${declaringClass}_$index"
  }

  internal abstract val boundClass: Class

  public abstract override fun narrows(that: Dependency, info: TypeInfo): Boolean

  internal abstract fun intersect(expression: Expression): Dependency?

  /** Any [Dependency] except for the case covered by [FakeDependency] below. */
  public data class TypeDependency(override val key: Key, val boundType: GroundType) :
      Dependency(), HasExpression by boundType {

    override val boundClass: Class
      get() = boundType.rootClass

    override val className: ClassName
      get() = boundClass.className

    internal fun allConcreteSpecializations(): Sequence<TypeDependency> =
        boundType.allConcreteSubtypes().map { TypeDependency(key, it) }

    override fun toString(): String = "$key=$expressionFull"

    // Hierarchy

    override val abstract: Boolean
      get() = boundType.abstract

    override fun isSubtypeOf(that: Dependency): Boolean = boundType.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): Dependency? {
      if (that is ComplementDependency) {
        return that glb this
      }
      if (that !is TypeDependency) return null
      return (boundType glb boundOf(that))?.let { copy(boundType = it) }
    }

    override fun lub(that: Dependency): Dependency =
        when (that) {
          is ComplementDependency -> if (narrows(that, NoGameState)) that else that.domain()
          else -> copy(boundType = boundType lub boundOf(that))
        }

    internal inline fun map(function: (GroundType) -> GroundType) =
        copy(boundType = function(boundType))

    override fun intersect(expression: Expression): Dependency? {
      if (expression.complement) {
        val excluded = boundType.classTable.resolve(expression.uncomplemented())
        if (!excluded.isSubtypeOf(boundType)) return null
        return ComplementDependency(key, boundType, excluded)
      }
      return glb(copy(boundType = boundType.classTable.resolve(expression)))
    }

    override fun ensureNarrows(that: Dependency, info: TypeInfo) {
      when (that) {
        is ComplementDependency if !that.matches(boundType, info) ->
            throw NarrowingException("$this does not narrow $that")
        is ComplementDependency -> Unit
        is TypeDependency -> boundType.ensureNarrows(boundOf(that), info)
        else -> throw NarrowingException("$this does not narrow $that")
      }
    }

    override fun narrows(that: Dependency, info: TypeInfo): Boolean =
        when (that) {
          is ComplementDependency -> that.matches(boundType, info)
          is TypeDependency -> boundType.narrows(boundOf(that), info)
          else -> false
        }

    private fun boundOf(that: Dependency): GroundType =
        (that as TypeDependency).boundType.also { require(key == that.key) }
  }

  /** A dependency constrained to exclude one narrower type, as in `OwnedTile<!Player1>`. */
  internal data class ComplementDependency(
      override val key: Key,
      internal val domainType: GroundType,
      internal val excludedType: GroundType,
  ) : Dependency(), HasExpression {
    init {
      require(excludedType.isSubtypeOf(domainType)) { "$excludedType does not narrow $domainType" }
    }

    override val boundClass: Class
      get() = domainType.rootClass

    override val className: ClassName
      get() = excludedType.className

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
          is TypeDependency -> {
            val intersectionDomain = domainType glb that.boundType ?: return null
            val intersectionExcluded = excludedType glb intersectionDomain
            when {
              intersectionExcluded == null -> TypeDependency(key, intersectionDomain)
              intersectionDomain.isSubtypeOf(intersectionExcluded) -> null
              else -> copy(domainType = intersectionDomain, excludedType = intersectionExcluded)
            }
          }
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
        throw NarrowingException("$this does not narrow $that")
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

    override val className: ClassName
      get() = boundClass.className

    override val expression: Expression
      get() = className.expression

    override val expressionFull: Expression
      get() = expression

    override fun toString() = "$key=$expressionFull"

    // Hierarchy

    override val abstract: Boolean
      get() = boundClass.abstract

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
      val klass = boundClass.classTable.getClass(expression.className)
      return glb(FakeDependency(klass))
    }
  }

  internal companion object {
    // Note these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: List<Dependency>) {
      deps.indices.forEach { index ->
        for (previous in 0 until index) {
          require(deps[index].key != deps[previous].key) { "duplicate dependency keys: $deps" }
        }
      }
      require(deps.none { it is FakeDependency } || deps.single() is FakeDependency)
      val classTable = deps.firstOrNull()?.boundClass?.classTable
      require(deps.all { it.boundClass.classTable === classTable }) {
        "dependencies belong to different class tables"
      }
    }

    internal fun isForClassType(deps: List<Dependency>) = deps.singleOrNull() is FakeDependency

    internal fun getClassForClassType(deps: List<Dependency>): Class =
        (deps.single() as FakeDependency).boundClass

    internal fun depsForClassType(klass: Class) = DependencySet.of(listOf(FakeDependency(klass)))
  }
}
